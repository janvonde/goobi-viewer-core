/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.api.rest.filters;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentExceptionMapper.ErrorMessage;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerPdfBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.StringPair;

/**
 * <p>
 * PdfRequestFilter class.
 * </p>
 */
@Provider
@ContentServerPdfBinding
public class PdfRequestFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(PdfRequestFilter.class);

    private static final String ATTRIBUTE_PDF_QUOTA = "pdf_quota";

    @Context
    private HttpServletRequest servletRequest;

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        try {

            if (DataManager.getInstance().getConfiguration().isPdfApiDisabled()) {
                throw new ServiceNotAllowedException("PDF API is disabled");
            }

            Path requestPath = Paths.get(request.getUriInfo().getPath());
            //            String requestPath = request.getUriInfo().getPath();

            String type = requestPath.getName(0).toString();
            String pi = null;
            String divId = null;
            String imageName = null;
            String privName = IPrivilegeHolder.PRIV_DOWNLOAD_PDF;
            if (servletRequest.getAttribute("pi") != null) {
                pi = (String) servletRequest.getAttribute("pi");
                divId = (String) servletRequest.getAttribute("divId");
                if (servletRequest.getAttribute("filename") != null) {
                    privName = IPrivilegeHolder.PRIV_DOWNLOAD_PAGE_PDF;
                    imageName = (String) servletRequest.getAttribute("filename");
                }
            } else {
                if ("pdf".equalsIgnoreCase(type)) {
                    //multipage mets pdf
                    pi = requestPath.getName(2).toString().replace(".xml", "").replaceAll(".XML", "");
                    if (requestPath.getNameCount() == 5) {
                        divId = requestPath.getName(3).toString();
                    }
                } else if ("image".equalsIgnoreCase(type)) {
                    //single page pdf
                    pi = requestPath.getName(1).toString();
                    imageName = requestPath.getName(2).toString();
                    privName = IPrivilegeHolder.PRIV_DOWNLOAD_PAGE_PDF;
                }
            }
            filterForAccessConditions(pi, divId, imageName, privName);
            filterForDownloadQuota(pi, divId, imageName, servletRequest);
        } catch (ServiceNotAllowedException e) {
            String mediaType = MediaType.TEXT_XML;
            if (request.getUriInfo() != null && request.getUriInfo().getPath().endsWith("json")) {
                mediaType = MediaType.APPLICATION_JSON;
            }
            Response response = Response.status(Status.FORBIDDEN).type(mediaType).entity(new ErrorMessage(Status.FORBIDDEN, e, false)).build();
            request.abortWith(response);
        }
    }

    /**
     * 
     * @param pi Record identifiers
     * @param divId Structure element ID
     * @param contentFileName
     * @param request Servlet request
     * @throws ServiceNotAllowedException
     */
    void filterForDownloadQuota(String pi, String divId, String contentFileName, HttpServletRequest request)
            throws ServiceNotAllowedException {
        try {
            int percentage = AccessConditionUtils.getPdfDownloadQuotaForRecord(pi);

            // Full record PDF
            if (StringUtils.isEmpty(divId) && StringUtils.isEmpty(contentFileName)) {
                if (percentage < 100) {
                    throw new ServiceNotAllowedException("Insufficient download quota for record '" + pi + "': " + percentage + "%");
                }
                return;
            }

            int numTotalRecordPages = (int) DataManager.getInstance()
                    .getSearchIndex()
                    .getHitCount("+" + SolrConstants.PI_TOPSTRUCT + ":" + pi + " +" + SolrConstants.DOCTYPE + ":PAGE");

            if (StringUtils.isNotEmpty(divId) && StringUtils.isEmpty(contentFileName)) {
                // Chapter PDF
                SolrDocumentList docs = DataManager.getInstance()
                        .getSearchIndex()
                        .search(
                                "+" + SolrConstants.PI_TOPSTRUCT + " +" + SolrConstants.LOGID + ":" + divId + " +" + SolrConstants.DOCTYPE + ":PAGE",
                                SolrSearchIndex.MAX_HITS, Collections.singletonList(new StringPair(SolrConstants.ORDER, "asc")),
                                Arrays.asList(new String[] { SolrConstants.ORDER, SolrConstants.FILENAME }));

                if (docs.isEmpty()) {
                    throw new RecordNotFoundException("Document not found: " + pi + "/" + divId);
                }
                // Check each page that belongs to the requested docstruct
                for (SolrDocument doc : docs) {
                    String fileName = (String) doc.getFieldValue(SolrConstants.FILENAME);
                    if (StringUtils.isEmpty(fileName)) {
                        logger.error("File name not found for page belonging to {}/{}", pi, divId);
                    }
                    if (!checkPageAllowed(pi, fileName, percentage, numTotalRecordPages, request)) {
                        logger.trace("Insufficient download quota");
                        throw new ServiceNotAllowedException("Insufficient download quota for record '" + pi + "': " + percentage + "%");
                    }
                }
            } else if (StringUtils.isEmpty(divId) && StringUtils.isNotEmpty(contentFileName)) {
                // Page PDF
                if (!checkPageAllowed(pi, contentFileName, percentage, numTotalRecordPages, request)) {
                    logger.trace("Insufficient download quota");
                    throw new ServiceNotAllowedException("Insufficient download quota for record '" + pi + "': " + percentage + "%");
                }
            }
        } catch (PresentationException | IndexUnreachableException | DAOException | RecordNotFoundException e) {
            logger.error(e.getMessage());
            throw new ServiceNotAllowedException(e.getMessage());
        }
    }

    /**
     * 
     * @param pi Record identifier
     * @param pageFile Page file name
     * @param percentage Allowed percentage of pages for PDF download
     * @param numTotalRecordPages
     * @param request HTTP servlet request object
     * @return true if page allowed as part of the quota; false otherwise
     * @should return false if session unavailable
     * @should return false if no session attribute exists yet
     * @should return true if page already part of quota
     * @should return false if quota already filled
     * @should return true and add page to map if quota not yet filled
     */
    @SuppressWarnings("unchecked")
    static boolean checkPageAllowed(String pi, String pageFile, int percentage, int numTotalRecordPages, HttpServletRequest request) {
        logger.trace("checkPageAllowed({}, {}, {}, {})", pi, pageFile, percentage, numTotalRecordPages);
        if (request == null || request.getSession() == null) {
            logger.trace("session not found");
            return false;
        }
        try {
            Map<String, Set<String>> quotaMap = (Map<String, Set<String>>) request.getSession().getAttribute(ATTRIBUTE_PDF_QUOTA);
            if (quotaMap == null) {
                quotaMap = new HashMap<>();
                request.getSession().setAttribute(ATTRIBUTE_PDF_QUOTA, quotaMap);
            }
            if (quotaMap.get(pi) == null) {
                quotaMap.put(pi, new HashSet<>());
            }
            // Page already allowed as part of the quota
            if (quotaMap.get(pi).contains(pageFile)) {
                logger.trace("Page {} already allowed for {}", pageFile, pi);
                return true;
            }
            // Quota already filled and requested page is not part of it
            int allowedPages = getNumAllowedPages(percentage, numTotalRecordPages);
            logger.trace("Allowed pages for {}: {}", pi, allowedPages);
            if (quotaMap.get(pi).size() >= allowedPages) {
                logger.trace("Quota already filled");
                return false;
            }
            // Add file to quotas
            quotaMap.get(pi).add(pageFile);
            logger.trace("Page {} allowed for {} and added to map", pageFile, pi);
            return true;
        } catch (ClassCastException e) {
            logger.error(e.getMessage(), e);
        }

        return false;
    }

    /**
     * Calculates the maximum number of allowed pages from the total number of pages for a record and the given percentage.
     * 
     * @param percentage Allowed percentage for downloads
     * @param numTotalRecordPages Total record pages
     * @return Maximum number of pages for the given percentage
     * @should return 0 if percentage 0
     * @should return 0 if number of pages 0
     * @should return number of pages if percentage 100
     * @should calculate number correctly
     * 
     */
    static int getNumAllowedPages(int percentage, int numTotalRecordPages) {
        if (percentage < 0) {
            throw new IllegalArgumentException("percentage may not be less than 0");
        }
        if (numTotalRecordPages < 0) {
            throw new IllegalArgumentException("numTotalRecordPages may not be less than 0");
        }
        if (numTotalRecordPages == 0 || percentage == 0) {
            return 0;
        }
        if (percentage == 100) {
            return numTotalRecordPages;
        }

        return (int) Math.floor(((double) numTotalRecordPages / 100 * percentage));
    }

    /**
     * @param pi
     * @param divId
     * @param contentFileName
     * @param privName
     * @throws ServiceNotAllowedException
     * @throws IndexUnreachableException
     */
    private void filterForAccessConditions(String pi, String divId, String contentFileName, String privName) throws ServiceNotAllowedException {
        logger.trace("filterForAccessConditions: {} {}", servletRequest.getSession().getId(), contentFileName);
        contentFileName = StringTools.decodeUrl(contentFileName);
        boolean access = false;
        try {

            access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, divId, privName, servletRequest);

        } catch (IndexUnreachableException e) {
            throw new ServiceNotAllowedException("Serving this image is currently impossibe due to ");
        } catch (DAOException e) {
            throw new ServiceNotAllowedException("Serving this image is currently impossibe due to ");
        }

        if (!access) {
            throw new ServiceNotAllowedException("Serving this image is restricted due to access conditions");
        }
    }

}

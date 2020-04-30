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
package io.goobi.viewer.servlets.rest.iiif.image;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentExceptionMapper.ErrorMessage;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerBinding;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

/**
 * Adds additional parameters to iiif contentServer requests as requestContext properties Parameters must be named "param:" followed by the name of
 * the corresponding contentServer request parameter
 */
@Provider
@ContentServerBinding
public class ImageParameterFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ImageParameterFilter.class);

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        String uri = request.getUriInfo().getPath();
        if (uri.contains("image/") || uri.contains("pdf/mets/")) {

            String requestPath;
            if (uri.contains("image/")) {
                requestPath = uri.substring(uri.indexOf("image/") + 6);
            } else {
                requestPath = uri.substring(uri.indexOf("pdf/mets/") + 9);
            }

            // logger.trace("Filtering request {}", requestPath);
            StringTokenizer tokenizer = new StringTokenizer(requestPath, "/");
            List<String> pathSegments = tokenizer.getTokenList();
            String pi = pathSegments.get(0).replaceAll("\\..+", "");
            try {
                if (StringUtils.isNotBlank(pi) && !"-".equals(pi)) {
                    addRepositoryPathIfRequired(request, pi);
                }
            } catch (PresentationException e) {
                String mediaType = MediaType.TEXT_XML;
                if (request.getUriInfo() != null && request.getUriInfo().getPath().endsWith("json")) {
                    mediaType = MediaType.APPLICATION_JSON;
                }
                Response errorResponse = Response.status(Status.INTERNAL_SERVER_ERROR)
                        .type(mediaType)
                        .entity(new ErrorMessage(Status.INTERNAL_SERVER_ERROR, e, false))
                        .build();
                request.abortWith(errorResponse);
            }
        }

    }

    /**
     * <p>
     * addRepositoryPathIfRequired.
     * </p>
     *
     * @param request a {@link javax.ws.rs.container.ContainerRequestContext} object.
     * @param pi a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public static void addRepositoryPathIfRequired(ContainerRequestContext request, String pi) throws PresentationException {
        try {
            String repositoryRoot = DataFileTools.getDataRepositoryPathForRecord(pi);
            addRepositoryParameter("param:imageSource", repositoryRoot, DataManager.getInstance().getConfiguration().getMediaFolder(), request);
            addRepositoryParameter("param:pdfSource", repositoryRoot, DataManager.getInstance().getConfiguration().getPdfFolder(), request);
            addRepositoryParameter("param:altoSource", repositoryRoot, DataManager.getInstance().getConfiguration().getAltoFolder(), request);
            addRepositoryParameter("param:metsSource", repositoryRoot, DataManager.getInstance().getConfiguration().getIndexedMetsFolder(), request);
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
            throw e;
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            throw new PresentationException(e.getMessage());
        }
    }

    /**
     * @param request
     * @param dataRepository
     * @param repositoryFolder
     * @param requestParameter
     */
    private static void addRepositoryParameter(String requestParameter, String dataRepository, String repositoryFolder,
            ContainerRequestContext request) {
        StringBuilder sb = new StringBuilder(dataRepository).append("/").append(repositoryFolder);
        URI imageRepositoryPath;
        imageRepositoryPath = Paths.get(sb.toString()).toUri();
        request.setProperty(requestParameter, imageRepositoryPath.toString());
    }
}

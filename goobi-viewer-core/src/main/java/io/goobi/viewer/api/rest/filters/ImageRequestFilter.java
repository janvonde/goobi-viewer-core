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
import java.util.List;
import java.util.Optional;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.commons.text.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale.AbsoluteScale;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale.RelativeScale;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentExceptionMapper.ErrorMessage;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerImageBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.AccessConditionUtils;

/**
 * <p>
 * Request filter for image requests. Does two things:
 * <ol>
 * <li>Forwards requests to a page number to the actual file</li>
 * <li>Sets parameters for image delivery in request</li>
 * </ol>
 * </p>
 */
@Provider
@ContentServerImageBinding
@Priority(FilterTools.PRIORITY_REDIRECT)
public class ImageRequestFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ImageRequestFilter.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    @Override
    public void filter(ContainerRequestContext request) throws IOException {

        String mediaType = MediaType.APPLICATION_JSON;
        if (servletRequest != null && servletRequest.getRequestURI().toLowerCase().contains("xml")) {
            mediaType = MediaType.TEXT_XML;
        }

        String pi = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_PI);
        String imageName = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_FILENAME);

        if (forwardToCanonicalUrl(pi, imageName, servletRequest, servletResponse)) {
            //if page order is given for image filename, forward to url with correct filename
            return;
        }
        try {
            if (!BeanUtils.getImageDeliveryBean().isExternalUrl(imageName) && !BeanUtils.getImageDeliveryBean().isPublicUrl(imageName)
                    && !BeanUtils.getImageDeliveryBean().isStaticImageUrl(imageName)) {
                //filtering for accessConditions is done in AccessConditionRequestFilter
                setRequestParameter(request, FilterTools.isThumbnail(servletRequest));
            }
        } catch (ViewerConfigurationException e) {
            Response response = Response.status(Status.INTERNAL_SERVER_ERROR)
                    .type(mediaType)
                    .entity(new ErrorMessage(Status.INTERNAL_SERVER_ERROR, e, false))
                    .build();
            request.abortWith(response);
        }
    }

    /**
     * @param request
     */
    public static void addThumbnailCompression(ContainerRequestContext request) {
        Integer compression = DataManager.getInstance().getConfiguration().getThumbnailsCompression();
        request.setProperty("param:compression", compression.toString());
    }

    /**
     * <p>
     * forwardToCanonicalUrl.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param imageName a {@link java.lang.String} object.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @return a boolean.
     * @throws java.io.IOException if any.
     */
    public boolean forwardToCanonicalUrl(String pi, String imageName, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (imageName != null && !imageName.contains(".") && imageName.matches("\\d+")) {
            try {
                Optional<String> filename = DataManager.getInstance().getSearchIndex().getFilename(pi, Integer.parseInt(imageName));
                if (filename.isPresent()) {
                    request.setAttribute(FilterTools.ATTRIBUTE_FILENAME, filename.get());
                    String redirectURI = request.getRequestURI().replace("/" + imageName + "/", "/" + filename.get() + "/");
                    response.sendRedirect(redirectURI);
                    return true;
                }
            } catch (NumberFormatException | PresentationException | IndexUnreachableException e) {
                logger.error("Unable to resolve image file for image order {} and pi {}", imageName, pi);
            }
        }
        return false;

    }

    /**
     * @param request
     * @param isThumb
     */
    private static void setRequestParameter(ContainerRequestContext request, boolean isThumb) {
        if (isThumb) {
            Integer compression = DataManager.getInstance().getConfiguration().getThumbnailsCompression();
            request.setProperty("param:compression", compression.toString());
        }
        Integer maxWidth = DataManager.getInstance().getConfiguration().getViewerMaxImageWidth();
        request.setProperty("param:maxWidth", maxWidth.toString());

        Integer maxHeight = DataManager.getInstance().getConfiguration().getViewerMaxImageHeight();
        request.setProperty("param:maxHeight", maxHeight.toString());

        if (!DataManager.getInstance().getConfiguration().isDiscloseImageContentLocation()) {
            request.setProperty("param:disclose-content-location", "false");

        }

    }


}

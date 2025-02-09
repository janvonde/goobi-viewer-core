/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.api.rest.v1.media;

import java.io.IOException;
import java.nio.file.Files;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.api.rest.model.MediaDeliveryService;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.security.AccessConditionUtils;
import static io.goobi.viewer.api.rest.v1.ApiUrls.*;

/**
 * A rest resource for delivering video and audio files
 *
 * @author Florian Alpers
 */
@Path(RECORDS_FILES)
public class MediaResource {

    private static final Logger logger = LogManager.getLogger(MediaResource.class);

    @Context
    private HttpServletRequest request;
    @Context
    private HttpServletResponse response;

    private final String pi;

    public MediaResource(@PathParam("pi") String pi) {
        this.pi = pi;
    }

    /**
     * <p>
     * serveMediaContent.
     * </p>
     *
     * @param type a {@link java.lang.String} object.
     * @param format a {@link java.lang.String} object.
     * @param identifier a {@link java.lang.String} object.
     * @param filename a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.AccessDeniedException if any.
     */
    @GET
    @Path(RECORDS_FILES_AUDIO)
    public String serveAudioContent(@PathParam("mimetype") String format, @PathParam("filename") String filename)
            throws PresentationException, IndexUnreachableException, AccessDeniedException {
        return serveMediaContent("audio", format, pi, filename);
    }

    /**
     * <p>
     * serveMediaContent.
     * </p>
     *
     * @param type a {@link java.lang.String} object.
     * @param format a {@link java.lang.String} object.
     * @param identifier a {@link java.lang.String} object.
     * @param filename a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.AccessDeniedException if any.
     */
    @GET
    @Path(RECORDS_FILES_VIDEO)
    public String serveVideoContent(@PathParam("mimetype") String format, @PathParam("filename") String filename)
            throws PresentationException, IndexUnreachableException, WebApplicationException {
        return serveMediaContent("video", format, pi, filename);
    }

    private String serveMediaContent(String type, String format, String identifier, String filename)
            throws PresentationException, IndexUnreachableException, WebApplicationException {
        String mimeType = type + "/" + format;

        checkAccess(type, identifier, filename);

        java.nio.file.Path file =
                DataFileTools.getDataFilePath(identifier, DataManager.getInstance().getConfiguration().getMediaFolder(), null, filename);
        if (Files.isRegularFile(file)) {
            logger.debug("Video file: {} ({} bytes)", file.toAbsolutePath(), file.toFile().length());
            try {
                new MediaDeliveryService().processRequest(request, response, file.toAbsolutePath().toString(), mimeType);
            } catch (IOException e) {
                throw new PresentationException("Error accessing media resource", e);
            }
        } else {
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (IOException e) {
                throw new WebApplicationException(e);
            }
        }
        return "";
    }

    /**
     * <p>
     * checkAccess.
     * </p>
     *
     * @param action a {@link java.lang.String} object.
     * @param pi a {@link java.lang.String} object.
     * @param contentFilename a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.AccessDeniedException if any.
     */
    public void checkAccess(String action, String pi, String contentFilename) throws WebApplicationException {
        boolean access = false;
        try {
            access = AccessConditionUtils.checkAccess(request, action, pi, contentFilename, false).isGranted();
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
        } catch (DAOException e) {
            logger.debug("DAOException thrown here: {}", e.getMessage());
        }
        if (!access) {
            throw new WebApplicationException(new AccessDeniedException("Access denied for " + pi + "/" + contentFilename));
        }
    }

}

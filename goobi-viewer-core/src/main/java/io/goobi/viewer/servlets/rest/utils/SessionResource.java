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
package io.goobi.viewer.servlets.rest.utils;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.ViewerRestServiceBinding;
import io.goobi.viewer.controller.DataManager;

/**
 * Resource for outputting the current session info.
 */
@Path("/session")
@ViewerRestServiceBinding
public class SessionResource {

    private static final Logger logger = LoggerFactory.getLogger(SessionResource.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * <p>
     * Constructor for SessionResource.
     * </p>
     */
    public SessionResource() {
    }

    /**
     * For testing
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     */
    protected SessionResource(HttpServletRequest request) {
        this.servletRequest = request;
    }

    /**
     * <p>
     * getSessionInfo.
     * </p>
     *
     * @should return session info correctly
     * @return a {@link java.lang.String} object.
     */
    @GET
    @Path("/info")
    @Produces({ MediaType.TEXT_PLAIN })
    @CORSBinding
    public String getSessionInfo() {
        if (servletRequest == null) {
            return "Servlet request not found";
        }

        StringBuilder sb = new StringBuilder();
        Map<String, String> sessionMap = DataManager.getInstance().getSessionMap().get(servletRequest.getSession().getId());
        if (sessionMap == null) {
            return "Session " + servletRequest.getSession().getId() + " not found";
        }
        for (String key : sessionMap.keySet()) {
            sb.append(key).append(": ").append(sessionMap.get(key)).append('\n');
        }

        return sb.toString();
    }
}

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
package io.goobi.viewer.servlets.rest.iiif.presentation;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.servlets.utils.ServletUtils;

/**
 * <p>
 * Abstract AbstractResource class.
 * </p>
 *
 * @author Florian Alpers
 */
public abstract class AbstractResource {

    private static final Logger logger = LoggerFactory.getLogger(AbstractResource.class);
    
    @Context
    protected HttpServletRequest servletRequest;
    @Context
    protected HttpServletResponse servletResponse;
    protected final AbstractApiUrlManager urls;

    /**
     * Default constructor
     */
    public AbstractResource() {
        urls = DataManager.getInstance().getRestApiManager().getDataApiManager();
    }

    /**
     * Unit test constructor injecting request and response
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     */
    public AbstractResource(HttpServletRequest request, HttpServletResponse response) {
        this();
        this.servletRequest = request;
        this.servletResponse = response;
    }


    /**
     * <p>
     * getServletURI.
     * </p>
     *
     * @return the servletURI
     */
    public URI getServletURI() {
        return URI.create(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest));
    }

    /**
     * <p>
     * getRequestURI.
     * </p>
     *
     * @return the requestURI
     */
    public URI getRequestURI() {
        return URI.create(ServletUtils.getServletPathWithoutHostAsUrlFromRequest(servletRequest) + servletRequest.getRequestURI() + "?"
                + servletRequest.getQueryString());
    }


}
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
package io.goobi.viewer.servlets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.HttpNotFoundException;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.ConfigurationTest;

public class MetsResolverTest extends AbstractDatabaseAndSolrEnabledTest {

    private static final String RESOLVER_NAME = "mets";

    private ServletRunner sr;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        sr = new ServletRunner();
        sr.registerServlet(RESOLVER_NAME, MetsResolver.class.getName());
    }

    /**
     * @see MetsResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return METS file correctly via pi
     */
    @Test
    public void doGet_shouldReturnMETSFileCorrectlyViaPi() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest(ConfigurationTest.APPLICATION_ROOT_URL + RESOLVER_NAME);
        request.setParameter("id", PI_KLEIUNIV);
        WebResponse response = sc.getResponse(request);
        Assert.assertNotNull(response);
    }

    /**
     * @see MetsResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return METS file correctly via urn
     */
    @Test
    public void doGet_shouldReturnMETSFileCorrectlyViaUrn() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest(ConfigurationTest.APPLICATION_ROOT_URL + RESOLVER_NAME);
        request.setParameter("urn", "urn:nbn:de:gbv:9-g-4882158");
        WebResponse response = sc.getResponse(request);
        Assert.assertNotNull(response);
    }

    /**
     * @see MetsResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return LIDO file correctly
     */
    @Test
    public void doGet_shouldReturnLIDOFileCorrectly() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest(ConfigurationTest.APPLICATION_ROOT_URL + RESOLVER_NAME);
        request.setParameter("id", "455820");
        WebResponse response = sc.getResponse(request);
        Assert.assertNotNull(response);
    }

    /**
     * @see MetsResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 404 if file not found
     */
    @Test(expected = HttpNotFoundException.class)
    public void doGet_shouldReturn404IfFileNotFound() throws Exception {
        ServletUnitClient sc = sr.newClient();
        {
            WebRequest request = new GetMethodWebRequest(ConfigurationTest.APPLICATION_ROOT_URL + RESOLVER_NAME);
            request.setParameter("id", "NOTFOUND");
            sc.getResponse(request);
        }
        {
            WebRequest request = new GetMethodWebRequest(ConfigurationTest.APPLICATION_ROOT_URL + RESOLVER_NAME);
            request.setParameter("urn", "NOTFOUND");
            sc.getResponse(request);
        }
    }

    /**
     * @see MetsResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 409 if more than one record matched
     */
    @Test(expected = HttpException.class)
    public void doGet_shouldReturn409IfMoreThanOneRecordMatched() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest(ConfigurationTest.APPLICATION_ROOT_URL + RESOLVER_NAME);
        request.setParameter("urn", "test:1234:goobi:3431");
        sc.getResponse(request);
    }

    /**
     * @see MetsResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 500 if record identifier bad
     */
    @Test(expected = HttpException.class)
    public void doGet_shouldReturn500IfRecordIdentifierBad() throws Exception {
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest(ConfigurationTest.APPLICATION_ROOT_URL + RESOLVER_NAME);
        request.setParameter("id", "a:b");
        sc.getResponse(request);
    }
}

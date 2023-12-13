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
package io.goobi.viewer.api.rest.model;

import static org.junit.Assert.*;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.goobi.viewer.model.security.user.User;

/**
 * @author florian
 *
 */
public class UserJsonFacadeTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        //
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        //
    }

    @Test
    public void testGetCorrectAvatarUrl() {

        User user = new User("nick");

        UserJsonFacade facade = new UserJsonFacade(user);
        String avatarUrl = facade.getAvatar();
        URI avatarURI = URI.create(avatarUrl);
        Assert.assertFalse(avatarURI.isAbsolute());

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("https://viewer.goobi.io/"));
        Mockito.when(request.getContextPath()).thenReturn("/viewer");

        facade = new UserJsonFacade(user, request);
        avatarUrl = facade.getAvatar();
        avatarURI = URI.create(avatarUrl);
        Assert.assertTrue(avatarUrl.startsWith("/viewer/resources"));

    }

}

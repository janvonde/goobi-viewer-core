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
package io.goobi.viewer.servlets.rest.search;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.search.SearchHit;

/**
 * @author florian
 *
 */
public class SearchHitsNotificationResourceTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link io.goobi.viewer.servlets.rest.search.SearchHitsNotificationResource#sendNewHitsNotifications()}.
     * @throws ViewerConfigurationException 
     * @throws DAOException 
     * @throws IndexUnreachableException 
     * @throws PresentationException 
     */
    @Test
    public void testcheckSearchUpdate() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        SearchHitsNotificationResource resource = new SearchHitsNotificationResource();
        Search search = new Search();
        search.setQuery("ISWORK:*");
        search.setLastHitsCount(2);
        search.setPage(1);
        List<SearchHit> newHits = resource.getNewHits(search);
        assertFalse(newHits.isEmpty());
        assertEquals(newHits.size(), search.getLastHitsCount() - 2);
    }

}

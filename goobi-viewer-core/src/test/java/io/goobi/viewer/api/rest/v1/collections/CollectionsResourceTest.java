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
package io.goobi.viewer.api.rest.v1.collections;

import static io.goobi.viewer.api.rest.v1.ApiUrls.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;

/**
 * @author florian
 *
 */
public class CollectionsResourceTest extends AbstractRestApiTest{

    private static final String SOLR_FIELD = "DC";
    private static final String COLLECTION = "dctei";
    private static final String GROUP = "MD2_VIEWERSUBTHEME";


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.collections.CollectionsResource#getAllCollections(java.lang.String)}.
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    @Test
    public void testGetAllCollections() throws JsonMappingException, JsonProcessingException {
        String url = urls.path(COLLECTIONS).params(SOLR_FIELD).build();
        try(Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            String entity = response.readEntity(String.class);
            assertEquals("Should return status 200; answer; " + entity, 200, response.getStatus());
            assertNotNull(entity);
            JSONObject collection = new JSONObject(entity);
            assertEquals(url, collection.getString("@id"));
            assertTrue(collection.getJSONArray("members").length() > 0);
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.collections.CollectionsResource#getCollection(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testGetCollection() {
        String url = urls.path(COLLECTIONS, COLLECTIONS_COLLECTION).params(SOLR_FIELD, COLLECTION).build();
        try(Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            String entity = response.readEntity(String.class);
            assertEquals("Should return status 200; answer; " + entity, 200, response.getStatus());
            assertNotNull(entity);
            JSONObject collection = new JSONObject(entity);
            assertEquals(url, collection.getString("@id"));
            assertEquals(4, collection.getJSONArray("members").length());
        }
    }

}

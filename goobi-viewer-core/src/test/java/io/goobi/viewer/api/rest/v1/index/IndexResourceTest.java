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
package io.goobi.viewer.api.rest.v1.index;

import static io.goobi.viewer.api.rest.v1.ApiUrls.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.goobi.viewer.api.rest.AbstractRestApiTest;
import io.goobi.viewer.api.rest.model.ErrorMessage;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.servlets.rest.content.RecordsRequestParameters;
/**
 * @author florian
 *
 */
public class IndexResourceTest extends AbstractRestApiTest{

    RecordsRequestParameters params = new RecordsRequestParameters();
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        params.offset = 10;
        params.count = 5;
        params.jsonFormat = "datecentric";
        params.randomize = false;
        params.sortFields = Stream.of(SolrConstants.SORTNUM_YEAR, SolrConstants.LABEL).collect(Collectors.toList());
        params.sortOrder = "desc";
        params.query = "DOCSTRCT:picture";
        params.includeChildHits = false;

        
        
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testInvalidQuery() throws JsonMappingException, JsonProcessingException {
        params.sortFields = Stream.of("BLA").collect(Collectors.toList());
        Entity<RecordsRequestParameters> entity = Entity.entity(params, MediaType.APPLICATION_JSON);
        try(Response response = target(urls.path(INDEX, INDEX_QUERY).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .post(entity)) {
            assertEquals("Should return status 400", 400, response.getStatus());
            String jsonString = response.readEntity(String.class);
            ErrorMessage error = mapper.readValue(jsonString, ErrorMessage.class);
            assertEquals(400, error.getStatus());
        }
    }
    
    @Test
    public void testQuery() throws JsonMappingException, JsonProcessingException {
        params.count = 4;
        params.jsonFormat = "recordcentric";
        Entity<RecordsRequestParameters> entity = Entity.entity(params, MediaType.APPLICATION_JSON);
        try(Response response = target(urls.path(INDEX, INDEX_QUERY).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .post(entity)) {
            assertEquals("Should return status 200", 200, response.getStatus());
            String jsonString = response.readEntity(String.class);
            JSONArray array = new JSONArray(jsonString);
            assertEquals(4, array.length());
        }
    }
    
    @Test
    public void testStatistics() throws JsonMappingException, JsonProcessingException {
        String url = urls.path(INDEX, INDEX_STATISTICS).build();
        try(Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            String jsonString = response.readEntity(String.class);
            JSONObject json = new JSONObject(jsonString);
            Object count = json.get("count");
            assertNotNull(count);
            assertTrue(count instanceof Integer);
            assertTrue(((Integer)count) > 0);
        }
    }

}
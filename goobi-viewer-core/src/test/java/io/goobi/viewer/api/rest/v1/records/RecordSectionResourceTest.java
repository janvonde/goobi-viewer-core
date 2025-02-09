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
package io.goobi.viewer.api.rest.v1.records;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_SECTIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_SECTIONS_RANGE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_SECTIONS_RIS_FILE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_SECTIONS_RIS_TEXT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.iiif.presentation.v2.Range2;
import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;

/**
 * @author florian
 *
 */
public class RecordSectionResourceTest extends AbstractRestApiTest {

    private static final String PI = "74241";
    private static final String DIVID ="LOG_0004";

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
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordResource#getRISAsFile()}.
     */
    @Test
    public void testGetRISAsFile() {
        try(Response response = target(urls.path(RECORDS_SECTIONS, RECORDS_SECTIONS_RIS_FILE).params(PI, DIVID).build())
                .request()
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            assertTrue(entity.contains("TY  - FIGURE"));
            assertTrue(entity.contains("TI  - Wappen 1"));
            String fileName = PI + "_LOG_0004.ris";
            assertEquals( "attachment; filename=\"" + fileName + "\"", response.getHeaderString("Content-Disposition"));
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.api.rest.v1.records.RecordResource#getRISAsText()}.
     */
    @Test
    public void testGetRISAsText() {
        try(Response response = target(urls.path(RECORDS_SECTIONS, RECORDS_SECTIONS_RIS_TEXT).params(PI, DIVID).build())
                .request()
                .accept(MediaType.TEXT_PLAIN)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            assertTrue(entity.contains("TY  - FIGURE"));
            assertTrue(entity.contains("TI  - Wappen 1"));
        }
    }

    @Test
    public void testGetRange() throws JsonMappingException, JsonProcessingException {
        String url = urls.path(RECORDS_SECTIONS, RECORDS_SECTIONS_RANGE).params(PI, DIVID).build();
        try(Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            assertNotNull(entity);
            Range2 range = mapper.readValue(entity, Range2.class);
            assertEquals(URI.create(url), range.getId());
        }
    }


}

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
package io.goobi.viewer.model.viewer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;

public class PageTypeTest extends AbstractTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @see PageType#determinePageType(String,String,String,boolean,boolean,boolean,boolean)
     * @verifies return configured page type correctly
     */
    @Test
    public void determinePageType_shouldReturnConfiguredPageTypeCorrectly() throws Exception {
        Assert.assertEquals(PageType.viewToc, PageType.determinePageType("Catalogue", null, false, true, false));
    }

    /**
     * @see PageType#determinePageType(String,String,String,boolean,boolean,boolean,boolean)
     * @verifies return metadata page type for application mime type
     */
    @Test
    public void determinePageType_shouldReturnMetadataPageTypeForApplicationMimeType() throws Exception {
        Assert.assertEquals(PageType.viewMetadata, PageType.determinePageType("Monograph", "application", false, false, false));
    }

    /**
     * @see PageType#determinePageType(String,String,String,boolean,boolean,boolean,boolean)
     * @verifies return toc page type for anchors
     */
    @Test
    public void determinePageType_shouldReturnTocPageTypeForAnchors() throws Exception {
        Assert.assertEquals(PageType.viewToc, PageType.determinePageType("Periodical", null, true, false, false));
    }

    /**
     * @see PageType#determinePageType(String,String,String,boolean,boolean,boolean,boolean)
     * @verifies return image page type correctly
     */
    @Test
    public void determinePageType_shouldReturnImagePageTypeCorrectly() throws Exception {
        Assert.assertEquals(PageType.viewObject, PageType.determinePageType("Monograph", null, false, true, false));
    }

    /**
     * @see PageType#determinePageType(String,String,String,boolean,boolean,boolean,boolean)
     * @verifies return medatata page type if nothing else matches
     */
    @Test
    public void determinePageType_shouldReturnMedatataPageTypeIfNothingElseMatches() throws Exception {
        Assert.assertEquals(PageType.viewMetadata, PageType.determinePageType("Monograph", null, false, false, false));
    }

    /**
     * @see PageType#getByName(String)
     * @verifies return correct type for raw names
     */
    @Test
    public void getByName_shouldReturnCorrectTypeForRawNames() throws Exception {
        Assert.assertEquals(PageType.viewFulltext, PageType.getByName("fulltext"));
    }

    /**
     * @see PageType#getByName(String)
     * @verifies return correct type for mapped names
     */
    @Test
    public void getByName_shouldReturnCorrectTypeForMappedNames() throws Exception {
        Assert.assertEquals(PageType.viewImage, PageType.getByName("image"));
    }

    /**
     * @see PageType#getByName(String)
     * @verifies return correct type for enum names
     */
    @Test
    public void getByName_shouldReturnCorrectTypeForEnumNames() throws Exception {
        Assert.assertEquals(PageType.viewFulltext, PageType.getByName("viewFulltext"));
    }

    /**
     * @see PageType#getByName(String)
     * @verifies return correct type if name starts with metadata
     */
    @Test
    public void getByName_shouldReturnCorrectTypeIfNameStartsWithMetadata() throws Exception {
        Assert.assertEquals(PageType.viewMetadata, PageType.getByName("metadata_other"));
    }
}

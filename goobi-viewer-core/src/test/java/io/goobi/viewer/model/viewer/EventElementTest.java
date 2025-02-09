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

import java.util.Locale;

import org.apache.solr.common.SolrDocument;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.solr.SolrConstants;

public class EventElementTest extends AbstractTest {

    /**
     * @see EventElement#EventElement(SolrDocument,Locale)
     * @verifies fill in missing dateStart from displayDate
     */
    @Test
    public void EventElement_shouldFillInMissingDateStartFromDisplayDate() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.EVENTDATE, "2018-11-23");
        EventElement ee = new EventElement(doc, null, false);
        Assert.assertNotNull(ee.getDateStart());
        Assert.assertEquals("2018-11-23", DateTools.format(ee.getDateStart(), DateTools.formatterISO8601Date, false));
    }

    /**
     * @see EventElement#EventElement(SolrDocument,Locale)
     * @verifies fill in missing dateEnd from dateStart
     */
    @Test
    public void EventElement_shouldFillInMissingDateEndFromDateStart() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.EVENTDATESTART, "2018-11-23");
        EventElement ee = new EventElement(doc, null, false);
        Assert.assertNotNull(ee.getDateEnd());
        Assert.assertEquals("2018-11-23", DateTools.format(ee.getDateEnd(), DateTools.formatterISO8601Date, false));
    }

    /**
     * @see EventElement#getLabel()
     * @verifies include type
     */
    @Test
    public void getLabel_shouldIncludeType() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.EVENTTYPE, "Creation");
        EventElement ee = new EventElement(doc, null, false);
        Assert.assertEquals("Creation", ee.getLabel());
    }

    /**
     * @see EventElement#getLabel()
     * @verifies not include date
     */
    @Test
    public void getLabel_shouldNotIncludeDate() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.EVENTTYPE, "Creation");
        doc.setField(SolrConstants.EVENTDATESTART, "2021-09-17");
        EventElement ee = new EventElement(doc, null, false);
        Assert.assertEquals("Creation", ee.getLabel());
    }

    /**
     * @see EventElement#EventElement(SolrDocument,Locale,boolean)
     * @verifies populate search hit metadata correctly
     */
    @Test
    public void EventElement_shouldPopulateSearchHitMetadataCorrectly() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.EVENTTYPE, "Creation");
        doc.setField(SolrConstants.EVENTDATESTART, "2021-09-17");
        EventElement ee = new EventElement(doc, null, true); // search mode
        Assert.assertNotNull(ee.getSearchHitMetadata());
        Assert.assertNull(ee.getMetadata());
        Assert.assertNull(ee.getSidebarMetadata());
        // TODO test case with actual metadata values
    }

    /**
     * @see EventElement#EventElement(SolrDocument,Locale,boolean)
     * @verifies populate non search metadata correctly
     */
    @Test
    public void EventElement_shouldPopulateNonSearchMetadataCorrectly() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.EVENTTYPE, "Creation");
        doc.setField(SolrConstants.EVENTDATESTART, "2021-09-17");
        EventElement ee = new EventElement(doc, null, false); // non search mode
        Assert.assertNull(ee.getSearchHitMetadata());
        Assert.assertNotNull(ee.getMetadata());
        Assert.assertNotNull(ee.getSidebarMetadata());
        // TODO test case with actual metadata values
    }
}

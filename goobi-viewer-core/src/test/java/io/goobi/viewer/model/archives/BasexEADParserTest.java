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
package io.goobi.viewer.model.archives;

import org.jdom2.Document;
import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.XmlTools;

public class BasexEADParserTest extends AbstractDatabaseAndSolrEnabledTest {

    /**
     * @see BasexEADParser#parseEadFile(Document)
     * @verifies parse document correctly
     */
    @Test
    public void parseEadFile_shouldParseDocumentCorrectly() throws Exception {
        // Make sure the XML file has a <collection> element around <ead> (as delivered by BaseX)
        Document doc = XmlTools.readXmlFile("src/test/resources/data/EAD_Export_Tektonik.XML");
        Assert.assertNotNull(doc);
        Assert.assertNotNull(doc.getRootElement());
        ArchiveEntry root =
                new BasexEADParser(null, null)
                        .readConfiguration(DataManager.getInstance().getConfiguration().getArchiveMetadataConfig())
                        .parseEadFile(doc);
        Assert.assertNotNull(root);
        Assert.assertEquals(1, root.getSubEntryList().size());
        ArchiveEntry topEntry = root.getSubEntryList().get(0);
        Assert.assertEquals("1_Tektonik", topEntry.getId());
    }
}

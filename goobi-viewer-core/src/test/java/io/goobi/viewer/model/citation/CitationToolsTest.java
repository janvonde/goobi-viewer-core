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
package io.goobi.viewer.model.citation;

import org.junit.Assert;
import org.junit.Test;

import de.undercouch.citeproc.csl.CSLType;

public class CitationToolsTest {

    /**
     * @see CitationTools#getCSLTypeForDocstrct(String)
     * @verifies return correct type
     */
    @Test
    public void getCSLTypeForDocstrct_shouldReturnCorrectType() throws Exception {
        Assert.assertEquals(CSLType.ARTICLE, CitationTools.getCSLTypeForDocstrct(null, null));
        Assert.assertEquals(CSLType.ARTICLE, CitationTools.getCSLTypeForDocstrct("article", null));
        Assert.assertEquals(CSLType.ARTICLE, CitationTools.getCSLTypeForDocstrct("article", "Other"));
        Assert.assertEquals(CSLType.ARTICLE_JOURNAL, CitationTools.getCSLTypeForDocstrct("article", "PeriodicalVolume"));
        Assert.assertEquals(CSLType.ARTICLE_NEWSPAPER, CitationTools.getCSLTypeForDocstrct("article", "NewspaperIssue"));
        Assert.assertEquals(CSLType.ARTICLE, CitationTools.getCSLTypeForDocstrct("object", null));
        Assert.assertEquals(CSLType.BOOK, CitationTools.getCSLTypeForDocstrct("monograph", null));
        Assert.assertEquals(CSLType.CHAPTER, CitationTools.getCSLTypeForDocstrct("chapter", null));
        Assert.assertEquals(CSLType.MANUSCRIPT, CitationTools.getCSLTypeForDocstrct("manuscript", null));
        Assert.assertEquals(CSLType.MAP, CitationTools.getCSLTypeForDocstrct("SingleMap", null));
    }
}

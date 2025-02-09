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
package io.goobi.viewer.managedbeans;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;

public class StatisticsBeanTest extends AbstractSolrEnabledTest {

    /**
     * @see StatisticsBean#getTopStructTypesByNumber()
     * @verifies return list of docstruct types
     */
    @Test
    public void getTopStructTypesByNumber_shouldReturnListOfDocstructTypes() throws Exception {
        StatisticsBean bean = new StatisticsBean();
        List<String> result = bean.getTopStructTypesByNumber();
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
    }

    /**
     * @see StatisticsBean#getImportedPages()
     * @verifies return a non zero number
     */
    @Test
    public void getImportedPages_shouldReturnANonZeroNumber() throws Exception {
        StatisticsBean bean = new StatisticsBean();
        Long num = bean.getImportedPages();
        Assert.assertNotNull(num);
        Assert.assertTrue(num > 0);
    }

    /**
     * @see StatisticsBean#getImportedFullTexts()
     * @verifies return a non zero number
     */
    @Test
    public void getImportedFullTexts_shouldReturnANonZeroNumber() throws Exception {
        StatisticsBean bean = new StatisticsBean();
        Long num = bean.getImportedFullTexts();
        Assert.assertNotNull(num);
        Assert.assertTrue(num > 0);
    }

    /**
     * @see StatisticsBean#isIndexEmpty()
     * @verifies return false if index online
     */
    @Test
    public void isIndexEmpty_shouldReturnFalseIfIndexOnline() throws Exception {
        StatisticsBean bean = new StatisticsBean();
        Assert.assertFalse(bean.isIndexEmpty());
    }
}

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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.cms.CMSStaticPage;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;
import io.goobi.viewer.model.search.HitType;
import io.goobi.viewer.model.search.SearchHit;
import io.goobi.viewer.model.search.SearchHitFactory;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.solr.SolrConstants;

public class CmsBeanTest extends AbstractDatabaseAndSolrEnabledTest {

    private static final Logger logger = LogManager.getLogger(CmsBeanTest.class); //NOSONAR Sometimes used for debugging

    private CMSTemplateManager templateManager;
    private NavigationHelper navigationHelper;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        File webContent = new File("WebContent/").getAbsoluteFile();
        String webContentPath = webContent.toURI().toString();
        templateManager = new CMSTemplateManager(webContentPath, null);
        navigationHelper = new NavigationHelper();
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testPage() {
        CMSPage page = new CMSPage();
        CmsBean bean = new CmsBean(templateManager, navigationHelper);
        bean.setCurrentPage(page);
        Assert.assertEquals(page, bean.getCurrentPage());
    }

    @Test
    public void testGetLuceneFields() {
        List<String> fields = new CmsBean().getLuceneFields();
        Assert.assertTrue("Lucene field 'DC' is missing", fields.contains("DC"));
        Assert.assertTrue("Lucene field 'LABEL' is missing", fields.contains("LABEL"));
        Assert.assertTrue("Lucene field 'FILENAME' is missing", fields.contains("FILENAME"));
    }

    @Test
    public void testGetStaticPages() throws DAOException {
        CmsBean bean = new CmsBean();
        List<CMSStaticPage> staticPages = bean.getStaticPages();
        Assert.assertFalse(staticPages.isEmpty());
    }

    @Test
    public void testGetAvailableCmsPages() throws DAOException {
        CmsBean bean = new CmsBean();
        List<CMSPage> allPages = DataManager.getInstance().getDao().getAllCMSPages();
        List<CMSPage> availablePages = bean.getAvailableCmsPages(null);
        Assert.assertEquals(2, allPages.size() - availablePages.size());
    }

    @Test
    public void testSaveCMSPages() throws DAOException {
        CmsBean bean = new CmsBean();

        CMSPage page = new CMSPage();
        assertTrue(DataManager.getInstance().getDao().addCMSPage(page));

        List<CMSStaticPage> staticPages = bean.getStaticPages();
        CMSStaticPage staticPage = staticPages.get(0);
        //    	Assert.assertNull(staticPage.getCmsPage());
        staticPage.setCmsPage(page);
        bean.saveStaticPages();

        staticPages = bean.getStaticPages();
        staticPage = staticPages.get(0);
        Assert.assertEquals(page.getId(), staticPage.getCmsPageOptional().map(p -> p.getId()).orElse(-1l));

        staticPage.setCmsPage(null);
        bean.saveStaticPages();

        staticPages = bean.getStaticPages();
        staticPage = staticPages.get(0);
        Assert.assertNull(staticPage.getCmsPageOptional().orElse(null));
    }

    @Test
    public void testGetGroupedQueryResults() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        CmsBean bean = new CmsBean();

        String groupField = "GROUPING";
        String value1 = "VALUE1";
        String value2 = "VALUE2";
        String value3 = "VALUE3";

        List<SearchHit> results = new ArrayList<>();
        results.add(createSearchHit(groupField, value1, value2));
        results.add(createSearchHit(groupField, value1, value2));
        results.add(createSearchHit(groupField, value1, value3));

        List<Entry<String, List<SearchHit>>> hitMap = bean.getGroupedQueryResults(results, groupField);
        Assert.assertEquals(3, hitMap.size());
        Assert.assertEquals(3, hitMap.get(0).getValue().size());
        Assert.assertEquals(2, hitMap.get(1).getValue().size());
        Assert.assertEquals(1, hitMap.get(2).getValue().size());

        //Test for no valid grouping field
        hitMap = bean.getGroupedQueryResults(results, "bla");
        Assert.assertEquals(1, hitMap.size());
        Assert.assertEquals(3, hitMap.get(0).getValue().size());
    }

    /**
     *
     * @param field Metadata field name
     * @param values Metadata field values
     * @return A mock SearchHit
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    private static SearchHit createSearchHit(String field, String... values)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        SolrDocument doc = new SolrDocument();
        String iddoc = Long.toString(System.nanoTime());
        doc.addField(field, Arrays.asList(values));
        doc.addField(SolrConstants.IDDOC, iddoc);
        doc.addField(SolrConstants.PI_TOPSTRUCT, UUID.randomUUID());
        doc.addField("LABEL", doc.getFieldValue(SolrConstants.PI_TOPSTRUCT));
        SearchHit hit =
                new SearchHitFactory(null, null, null, 0, null, Locale.GERMAN).createSearchHit(doc, null, null, HitType.DOCSTRCT);
        hit.getBrowseElement().setLabelShort(new SimpleMetadataValue(iddoc));
        // logger.debug("labelShort: {}", hit.getBrowseElement().getLabelShort());
        hit.setSolrDoc(doc);
        return hit;
    }

    /**
     * @see CmsBean#createStaticPageList()
     * @verifies return pages in specified order
     */
    @Test
    public void createStaticPageList_shouldReturnPagesInSpecifiedOrder() throws Exception {
        List<PageType> pageTypes = PageType.getTypesHandledByCms(); // Order specified in the enum
        List<CMSStaticPage> pages = CmsBean.createStaticPageList();
        Assert.assertEquals(pageTypes.size(), pages.size());
        for (int i = 0; i < pageTypes.size(); ++i) {
            Assert.assertEquals(pageTypes.get(i).getName(), pages.get(i).getPageName());
        }
    }

    /**
     * @see CmsBean#getPossibleSortFields()
     * @verifies add relevance and random values at beginning
     */
    @Test
    public void getPossibleSortFields_shouldAddRelevanceAndRandomValuesAtBeginning() throws Exception {
        CmsBean bean = new CmsBean(templateManager, navigationHelper);
        List<String> fields = bean.getPossibleSortFields();
        Assert.assertTrue(fields.size() > 2);
        Assert.assertEquals(SolrConstants.SORT_RELEVANCE, fields.get(0));
        Assert.assertEquals(SolrConstants.SORT_RANDOM, fields.get(1));
    }
}

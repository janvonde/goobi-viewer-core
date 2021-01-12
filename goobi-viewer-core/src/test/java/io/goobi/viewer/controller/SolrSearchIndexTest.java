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
package io.goobi.viewer.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.model.viewer.StringPair;

public class SolrSearchIndexTest extends AbstractSolrEnabledTest {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(SolrSearchIndexTest.class);

    /**
     * @see SolrSearchIndex#search(String,int,int,List,boolean,List,String,List)
     * @verifies return correct results
     */
    @Test
    public void search_shouldReturnCorrectResults() throws Exception {
        QueryResponse response = DataManager.getInstance()
                .getSearchIndex()
                .search(SolrConstants.PI + ":PPN517154005 " + SolrConstants.PI + ":34115495_1940", 0, Integer.MAX_VALUE, null, null, null);
        Assert.assertEquals(2, response.getResults().size());
    }

    /**
     * @see SolrSearchIndex#search(String,int,int,List,boolean,List,String,List)
     * @verifies return correct number of rows
     */
    @Test
    public void search_shouldReturnCorrectNumberOfRows() throws Exception {
        QueryResponse response = DataManager.getInstance().getSearchIndex().search(SolrConstants.PI + ":*", 100, 10, null, null, null);
        Assert.assertEquals(10, response.getResults().size());
    }

    /**
     * @see SolrSearchIndex#search(String,int,int,List,boolean,List,String,List)
     * @verifies sort results correctly
     */
    @Test
    public void search_shouldSortResultsCorrectly() throws Exception {
        QueryResponse response = DataManager.getInstance()
                .getSearchIndex()
                .search(SolrConstants.PI + ":*", 0, 10, Collections.singletonList(new StringPair(SolrConstants.DATECREATED, "desc")), null, null);
        Assert.assertEquals(10, response.getResults().size());
        long previous = -1;
        for (SolrDocument doc : response.getResults()) {
            Long datecreated = (Long) doc.getFieldValue(SolrConstants.DATECREATED);
            Assert.assertNotNull(datecreated);
            if (previous != -1) {
                Assert.assertTrue(previous >= datecreated);
            }
            previous = datecreated;
        }
    }

    /**
     * @see SolrSearchIndex#search(String,int,int,List,boolean,List,String,List)
     * @verifies facet results correctly
     */
    @Test
    public void search_shouldFacetResultsCorrectly() throws Exception {
        QueryResponse response = DataManager.getInstance()
                .getSearchIndex()
                .search(SolrConstants.PI + ":*", 0, 10, null, Collections.singletonList(SolrConstants.DC), null);
        Assert.assertEquals(10, response.getResults().size());
        Assert.assertNotNull(response.getFacetField(SolrConstants.DC));
        Assert.assertNotNull(response.getFacetField(SolrConstants.DC).getValues());
    }

    /**
     * @see SolrSearchIndex#search(String,int,int,List,boolean,List,String,List)
     * @verifies filter fields correctly
     */
    @Test
    public void search_shouldFilterFieldsCorrectly() throws Exception {
        QueryResponse response = DataManager.getInstance()
                .getSearchIndex()
                .search(SolrConstants.PI + ":*", 0, 10, null, null, Collections.singletonList(SolrConstants.PI));
        Assert.assertEquals(10, response.getResults().size());
        for (SolrDocument doc : response.getResults()) {
            Assert.assertEquals(1, doc.getFieldNames().size());
            Assert.assertTrue(doc.getFieldNames().contains(SolrConstants.PI));

        }
    }

    /**
     * @see SolrSearchIndex#getMetadataValues(SolrDocument,String)
     * @verifies return all values for the given field
     */
    @Test
    public void getMetadataValues_shouldReturnAllValuesForTheGivenField() throws Exception {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + PI_KLEIUNIV, null);
        Assert.assertNotNull(doc);
        List<String> values = SolrSearchIndex.getMetadataValues(doc, SolrConstants.DATEUPDATED);
        Assert.assertTrue(values.size() >= 2);
    }

    /**
     * @see SolrSearchIndex#getFieldValueMap(SolrDocument)
     * @verifies return all fields in the given doc except page urns
     */
    @Test
    public void getFieldValueMap_shouldReturnAllFieldsInTheGivenDocExceptPageUrns() throws Exception {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + PI_KLEIUNIV, null);
        Assert.assertNotNull(doc);
        Map<String, List<String>> fieldValueMap = SolrSearchIndex.getFieldValueMap(doc);
        Assert.assertFalse(fieldValueMap.containsKey(SolrConstants.IMAGEURN_OAI));
        Assert.assertFalse(fieldValueMap.containsKey("PAGEURNS"));
    }

    /**
     * @see SolrSearchIndex#searchFacetsAndStatistics(String,List,boolean)
     * @verifies generate facets correctly
     */
    @Test
    public void searchFacetsAndStatistics_shouldGenerateFacetsCorrectly() throws Exception {
        String[] facetFields = { SolrConstants._CALENDAR_YEAR, SolrConstants._CALENDAR_MONTH };
        QueryResponse resp = DataManager.getInstance()
                .getSearchIndex()
                .searchFacetsAndStatistics(SolrConstants._CALENDAR_YEAR + ":*", null, Arrays.asList(facetFields), 0, false);
        Assert.assertNotNull(resp.getFacetField(SolrConstants._CALENDAR_YEAR));
        Assert.assertNotNull(resp.getFacetField(SolrConstants._CALENDAR_MONTH));
    }

    /**
     * @see SolrSearchIndex#searchFacetsAndStatistics(String,List,boolean)
     * @verifies generate field statistics for every facet field if requested
     */
    @Test
    public void searchFacetsAndStatistics_shouldGenerateFieldStatisticsForEveryFacetFieldIfRequested() throws Exception {
        String[] facetFields = { SolrConstants._CALENDAR_YEAR };
        QueryResponse resp = DataManager.getInstance()
                .getSearchIndex()
                .searchFacetsAndStatistics(SolrConstants._CALENDAR_YEAR + ":*", null, Arrays.asList(facetFields), 0, true);
        Assert.assertNotNull(resp.getFieldStatsInfo());
        FieldStatsInfo info = resp.getFieldStatsInfo().get(SolrConstants._CALENDAR_YEAR);
        Assert.assertNotNull(info);
    }

    /**
     * @see SolrSearchIndex#searchFacetsAndStatistics(String,List,boolean)
     * @verifies not return any docs
     */
    @Test
    public void searchFacetsAndStatistics_shouldNotReturnAnyDocs() throws Exception {
        QueryResponse resp = DataManager.getInstance()
                .getSearchIndex()
                .searchFacetsAndStatistics(SolrConstants._CALENDAR_YEAR + ":*", null, Collections.singletonList(SolrConstants._CALENDAR_YEAR), 0,
                        false);
        Assert.assertTrue(resp.getResults().isEmpty());
    }

    /**
     * @see SolrSearchIndex#getImageOwnerIddoc(String,int)
     * @verifies retrieve correct IDDOC
     */
    @Test
    public void getImageOwnerIddoc_shouldRetrieveCorrectIDDOC() throws Exception {
        long iddoc = DataManager.getInstance().getSearchIndex().getImageOwnerIddoc(PI_KLEIUNIV, 1);
        Assert.assertNotEquals(-1, iddoc);
    }

    /**
     * @see SolrSearchIndex#getSolrSortFieldsAsList(String,String)
     * @verifies split fields correctly
     */
    @Test
    public void getSolrSortFieldsAsList_shouldSplitFieldsCorrectly() throws Exception {
        List<StringPair> result = SolrSearchIndex.getSolrSortFieldsAsList("SORT_A; SORT_B, desc;SORT_C,asc", ";", ",");
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("SORT_A", result.get(0).getOne());
        Assert.assertEquals("asc", result.get(0).getTwo());
        Assert.assertEquals("SORT_B", result.get(1).getOne());
        Assert.assertEquals("desc", result.get(1).getTwo());
        Assert.assertEquals("SORT_C", result.get(2).getOne());
        Assert.assertEquals("asc", result.get(2).getTwo());
    }

    /**
     * @see SolrSearchIndex#getSolrSortFieldsAsList(String,String)
     * @verifies split single field correctly
     */
    @Test
    public void getSolrSortFieldsAsList_shouldSplitSingleFieldCorrectly() throws Exception {
        List<StringPair> result = SolrSearchIndex.getSolrSortFieldsAsList("SORT_A , desc ", ";", ",");
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("SORT_A", result.get(0).getOne());
        Assert.assertEquals("desc", result.get(0).getTwo());
    }

    /**
     * @see SolrSearchIndex#getSolrSortFieldsAsList(String,String)
     * @verifies throw IllegalArgumentException if solrSortFields is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void getSolrSortFieldsAsList_shouldThrowIllegalArgumentExceptionIfSolrSortFieldsIsNull() throws Exception {
        SolrSearchIndex.getSolrSortFieldsAsList(null, ";", ",");
    }

    /**
     * @see SolrSearchIndex#getSolrSortFieldsAsList(String,String,String)
     * @verifies throw IllegalArgumentException if splitFieldsBy is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void getSolrSortFieldsAsList_shouldThrowIllegalArgumentExceptionIfSplitFieldsByIsNull() throws Exception {
        SolrSearchIndex.getSolrSortFieldsAsList("bla,blup", null, ",");
    }

    /**
     * @see SolrSearchIndex#getSolrSortFieldsAsList(String,String,String)
     * @verifies throw IllegalArgumentException if splitNameOrderBy is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void getSolrSortFieldsAsList_shouldThrowIllegalArgumentExceptionIfSplitNameOrderByIsNull() throws Exception {
        SolrSearchIndex.getSolrSortFieldsAsList("bla,blup", ";", null);
    }

    /**
     * @see SolrSearchIndex#getFirstDoc(String,List)
     * @verifies return correct doc
     */
    @Test
    public void getFirstDoc_shouldReturnCorrectDoc() throws Exception {
        SolrDocument doc = DataManager.getInstance()
                .getSearchIndex()
                .getFirstDoc(new StringBuilder(SolrConstants.PI_TOPSTRUCT).append(":")
                        .append(PI_KLEIUNIV)
                        .append(" AND ")
                        .append(SolrConstants.DOCTYPE)
                        .append(":PAGE")
                        .toString(), Collections.singletonList(SolrConstants.ORDER));
        Assert.assertNotNull(doc);
        Assert.assertEquals(1, doc.getFieldValue(SolrConstants.ORDER));
    }

    /**
     * @see SolrSearchIndex#getDocumentByIddoc(long)
     * @verifies return correct doc
     */
    @Test
    public void getDocumentByIddoc_shouldReturnCorrectDoc() throws Exception {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getDocumentByIddoc(String.valueOf(iddocKleiuniv));
        Assert.assertNotNull(doc);
        Assert.assertEquals(String.valueOf(iddocKleiuniv), doc.getFieldValue(SolrConstants.IDDOC));
    }

    /**
     * @see SolrSearchIndex#getIddocFromIdentifier(String)
     * @verifies retrieve correct IDDOC
     */
    @Test
    public void getIddocFromIdentifier_shouldRetrieveCorrectIDDOC() throws Exception {
        Assert.assertEquals(iddocKleiuniv, DataManager.getInstance().getSearchIndex().getIddocFromIdentifier(PI_KLEIUNIV));
    }

    /**
     * @see SolrSearchIndex#getIdentifierFromIddoc(long)
     * @verifies retrieve correct identifier
     */
    @Test
    public void getIdentifierFromIddoc_shouldRetrieveCorrectIdentifier() throws Exception {
        Assert.assertEquals(PI_KLEIUNIV, DataManager.getInstance().getSearchIndex().getIdentifierFromIddoc(iddocKleiuniv));
    }

    /**
     * @see SolrSearchIndex#getIddocByLogid(String,String)
     * @verifies retrieve correct IDDOC
     */
    @Test
    public void getIddocByLogid_shouldRetrieveCorrectIDDOC() throws Exception {
        Assert.assertNotEquals(-1, DataManager.getInstance().getSearchIndex().getIddocByLogid(PI_KLEIUNIV, "LOG_0001"));
    }

    @Test
    public void testGetMetadataValuesForLanguage() {
        SolrDocument doc = new SolrDocument();
        doc.addField("field_A", "value_A");
        doc.addField("field_B_LANG_EN", "field_B_en");
        doc.addField("field_B_LANG_DE", "field_B_de");
        doc.addField("field_B_LANG_EN", "field_B_en_2");

        Map<String, List<String>> mapA = SolrSearchIndex.getMetadataValuesForLanguage(doc, "field_A");
        Assert.assertEquals(1, mapA.size());
        Assert.assertEquals(1, mapA.get(MultiLanguageMetadataValue.DEFAULT_LANGUAGE).size());
        Assert.assertEquals("value_A", mapA.get(MultiLanguageMetadataValue.DEFAULT_LANGUAGE).get(0));

        Map<String, List<String>> mapB = SolrSearchIndex.getMetadataValuesForLanguage(doc, "field_B");
        Assert.assertEquals(2, mapB.size());
        Assert.assertEquals(mapB.get("en").size(), 2);
        Assert.assertEquals(mapB.get("de").size(), 1);
        Assert.assertEquals("field_B_de", mapB.get("de").get(0));
        Assert.assertEquals("field_B_en", mapB.get("en").get(0));
        Assert.assertEquals("field_B_en_2", mapB.get("en").get(1));

    }

    @Test
    public void testGetMultiLanguageFieldValueMap() {
        SolrDocument doc = new SolrDocument();
        doc.addField("field_A", "value_A");
        doc.addField("field_B", "value_B");
        doc.addField("field_B_LANG_EN", "field_B_en");
        doc.addField("field_B_LANG_DE", "field_B_de");
        doc.addField("field_B_LANG_EN", "field_B_en_2");

        Map<String, List<IMetadataValue>> map = SolrSearchIndex.getMultiLanguageFieldValueMap(doc);
        Assert.assertEquals(2, map.keySet().size());
        Assert.assertEquals("value_A", map.get("field_A").get(0).getValue().get());
        Assert.assertEquals("value_B", map.get("field_B").get(0).getValue().get());
        Assert.assertEquals("field_B_de", map.get("field_B").get(0).getValue("de").get());
        Assert.assertEquals("field_B_en", map.get("field_B").get(0).getValue("en").get());
        Assert.assertEquals("field_B_en_2", map.get("field_B").get(1).getValue("en").get());

        Assert.assertEquals("value_B", map.get("field_B").get(0).getValue("fr").orElse(map.get("field_B").get(0).getValue().orElse("")));

    }

    /**
     * @see SolrSearchIndex#getSingleFieldStringValue(SolrDocument,String)
     * @verifies return value as string correctly
     */
    @Test
    public void getSingleFieldStringValue_shouldReturnValueAsStringCorrectly() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.addField("NUM", 1337);
        Assert.assertEquals("1337", SolrSearchIndex.getSingleFieldStringValue(doc, "NUM"));
    }

    /**
     * @see SolrSearchIndex#getSingleFieldStringValue(SolrDocument,String)
     * @verifies not return null as string if value is null
     */
    @Test
    public void getSingleFieldStringValue_shouldNotReturnNullAsStringIfValueIsNull() throws Exception {
        SolrDocument doc = new SolrDocument();
        Assert.assertNull(SolrSearchIndex.getSingleFieldStringValue(doc, "MD_NOSUCHFIELD"));
    }

    /**
     * @see SolrSearchIndex#findDataRepositoryName(String)
     * @verifies return value from map if available
     */
    @Test
    public void findDataRepositoryName_shouldReturnValueFromMapIfAvailable() throws Exception {
        DataManager.getInstance().getSearchIndex().dataRepositoryNames.put("PPN123", "superrepo");
        Assert.assertEquals("superrepo", DataManager.getInstance().getSearchIndex().findDataRepositoryName("PPN123"));
    }

    /**
     * @see SolrSearchIndex#isHasImages(SolrDocument)
     * @verifies return correct value for page docs
     */
    @Test
    public void isHasImages_shouldReturnCorrectValueForPageDocs() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.FILENAME, "foo.jpg");
        Assert.assertTrue(SolrSearchIndex.isHasImages(doc));
        doc.setField(SolrConstants.FILENAME, "foo.txt");
        Assert.assertFalse(SolrSearchIndex.isHasImages(doc));
    }

    /**
     * @see SolrSearchIndex#isHasImages(SolrDocument)
     * @verifies return correct value for docsctrct docs
     */
    @Test
    public void isHasImages_shouldReturnCorrectValueForDocsctrctDocs() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.THUMBNAIL, "foo.jpg");
        Assert.assertTrue(SolrSearchIndex.isHasImages(doc));
    }

    /**
     * @see SolrSearchIndex#updateDataRepositoryNames(String,String)
     * @verifies update value correctly
     */
    @Test
    public void updateDataRepositoryNames_shouldUpdateValueCorrectly() throws Exception {
        Assert.assertNull(DataManager.getInstance().getSearchIndex().dataRepositoryNames.get("PPN123"));
        DataManager.getInstance().getSearchIndex().updateDataRepositoryNames("PPN123", "repo/a");
        Assert.assertEquals("repo/a", DataManager.getInstance().getSearchIndex().dataRepositoryNames.get("PPN123"));
    }

    /**
     * @see SolrSearchIndex#getLabelValuesForDrillDownField(String,String,Set)
     * @verifies return correct values
     */
    @Test
    public void getLabelValuesForDrillDownField_shouldReturnCorrectValues() throws Exception {
        String[] values = new String[] { "Groos, Karl", "Schubert, Otto", "Heinse, Gottlob Heinrich" };
        Map<String, String> result = DataManager.getInstance()
                .getSearchIndex()
                .getLabelValuesForDrillDownField("MD_AUTHOR", "MD_FIRSTNAME", new HashSet<>(Arrays.asList(values)));
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("Karl", result.get("MD_AUTHOR:Groos, Karl"));
        Assert.assertEquals("Otto", result.get("MD_AUTHOR:Schubert, Otto"));
        Assert.assertEquals("Gottlob Heinrich", result.get("MD_AUTHOR:Heinse, Gottlob Heinrich"));
    }
}
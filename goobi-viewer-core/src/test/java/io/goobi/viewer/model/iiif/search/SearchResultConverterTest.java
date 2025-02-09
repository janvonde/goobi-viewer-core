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
package io.goobi.viewer.model.iiif.search;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.jdom2.JDOMException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.api.annotation.oa.TextQuoteSelector;
import de.intranda.api.iiif.search.SearchHit;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.iiif.search.model.AnnotationResultList;
import io.goobi.viewer.model.iiif.search.parser.AbstractSearchParser;
import io.goobi.viewer.solr.SolrConstants;

import static io.goobi.viewer.api.rest.v1.ApiUrls.*;


/**
 * @author florian
 *
 */
public class SearchResultConverterTest extends AbstractSolrEnabledTest {

    String text = "A bird in the hand is worth\ntwo in the bush.";
    SearchResultConverter converter;
    String pi = "12345";
    String logId = "LOG_0000";
    int pageNo = 1;
    String restUrl;

    Path altoFile = Paths.get("src/test/resources/data/sample_alto.xml");

    ApiUrls urls = new ApiUrls();
    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        restUrl = DataManager.getInstance().getConfiguration().getRestApiUrl();
        converter = new SearchResultConverter(urls, pi, pageNo);

    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link io.goobi.viewer.model.iiif.search.SearchResultConverter#convertCommentToHit(java.lang.String, java.lang.String, io.goobi.viewer.model.annotation.comments.Comment)}.
     */
    @Test
    public void testConvertCommentToHit() {
        Comment comment = new Comment(pi, pageNo, null, text, null, null);
        comment.setId(1l);

        String query = "in";
        String queryRegex = AbstractSearchParser.getQueryRegex(query);

        SearchHit hit = converter.convertCommentToHit(queryRegex, pi, comment);

        String url = urls.path(ANNOTATIONS, ANNOTATIONS_COMMENT).params(comment.getId()).query("format", "oa").toString();

        Assert.assertNotNull(hit);
        Assert.assertEquals(url, hit.getAnnotations().get(0).getId().toString());
        Assert.assertEquals("in", hit.getMatch());
        TextQuoteSelector selector1 = (TextQuoteSelector) hit.getSelectors().get(0);
        TextQuoteSelector selector2 = (TextQuoteSelector) hit.getSelectors().get(1);
        Assert.assertEquals("A bird ", selector1.getPrefix());
        Assert.assertEquals(" the hand is worth\ntwo in the bush.", selector1.getSuffix());
        Assert.assertEquals("A bird in the hand is worth\ntwo ", selector2.getPrefix());
        Assert.assertEquals(" the bush.", selector2.getSuffix());
    }

    /**
     * Test method for
     * {@link io.goobi.viewer.model.iiif.search.SearchResultConverter#convertUGCToHit(java.lang.String, org.apache.solr.common.SolrDocument)}.
     */
    @Test
    public void testConvertUGCToHit() {
        SolrDocument ugc = new SolrDocument();
        ugc.setField(SolrConstants.UGCTERMS, text);
        ugc.setField(SolrConstants.UGCTYPE, "ADDRESS");
        ugc.setField(SolrConstants.PI_TOPSTRUCT, pi);
        ugc.setField(SolrConstants.ORDER, pageNo);
        ugc.setField(SolrConstants.IDDOC, 123456789);
        String query = "in";
        String queryRegex = AbstractSearchParser.getQueryRegex(query);
        SearchHit hit = converter.convertUGCToHit(queryRegex, ugc);
        String url = urls.path(ANNOTATIONS, ANNOTATIONS_ANNOTATION).params(123456789).build();
        Assert.assertNotNull(hit);
        Assert.assertEquals(url, hit.getAnnotations().get(0).getId().toString());
        Assert.assertEquals("in", hit.getMatch());
        TextQuoteSelector selector1 = (TextQuoteSelector) hit.getSelectors().get(0);
        TextQuoteSelector selector2 = (TextQuoteSelector) hit.getSelectors().get(1);
        Assert.assertEquals("A bird ", selector1.getPrefix());
        Assert.assertEquals(" the hand is worth\ntwo in the bush.", selector1.getSuffix());
        Assert.assertEquals("A bird in the hand is worth\ntwo ", selector2.getPrefix());
        Assert.assertEquals(" the bush.", selector2.getSuffix());
    }

    /**
     * Test method for
     * {@link io.goobi.viewer.model.iiif.search.SearchResultConverter#convertMetadataToHit(java.lang.String, java.lang.String, org.apache.solr.common.SolrDocument)}.
     */
    @Test
    public void testConvertMetadataToHit() {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.TITLE, text);
        doc.setField(SolrConstants.PI_TOPSTRUCT, pi);
        doc.setField(SolrConstants.LOGID, logId);
        doc.setField(SolrConstants.PI, pi);
        doc.setField(SolrConstants.THUMBPAGENO, pageNo);
        doc.setField(SolrConstants.IDDOC, 123456789);

        String query = "in";
        String queryRegex = AbstractSearchParser.getQueryRegex(query);
        SearchHit hit = converter.convertMetadataToHit(queryRegex, SolrConstants.TITLE, doc);

        String annoUrl = urls.path(ApiUrls.ANNOTATIONS, ApiUrls.ANNOTATIONS_METADATA).params(pi, logId, "MD_TITLE").query("format", "oa").build();

        Assert.assertNotNull(hit);
        Assert.assertEquals(annoUrl, hit.getAnnotations().get(0).getId().toString());
        Assert.assertEquals("in", hit.getMatch());
        TextQuoteSelector selector1 = (TextQuoteSelector) hit.getSelectors().get(0);
        TextQuoteSelector selector2 = (TextQuoteSelector) hit.getSelectors().get(1);
        Assert.assertEquals("A bird ", selector1.getPrefix());
        Assert.assertEquals(" the hand is worth\ntwo in the bush.", selector1.getSuffix());
        Assert.assertEquals("A bird in the hand is worth\ntwo ", selector2.getPrefix());
        Assert.assertEquals(" the bush.", selector2.getSuffix());
    }

    /**
     * Test method for
     * {@link io.goobi.viewer.model.iiif.search.SearchResultConverter#getAnnotationsFromAlto(de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument, java.lang.String)}.
     * @throws JDOMException
     * @throws IOException
     */
    @Test
    public void testGetAnnotationsFromAlto() throws IOException, JDOMException {


        String query = "Hollywood";
        String queryRegex = AbstractSearchParser.getQueryRegex(query);

        Assert.assertNotNull("Converter is null", converter);
        Assert.assertNotNull("Alto file is null", altoFile);
        Assert.assertTrue("Query regex is Blank", StringUtils.isNotBlank(queryRegex));

        AnnotationResultList results = converter.getAnnotationsFromAlto(altoFile, queryRegex);
        Assert.assertEquals(9, results.hits.size());

        SearchHit hit1 = results.hits.get(0);
        Assert.assertEquals("Hollywood!", hit1.getMatch());
        String url = urls.path(ApiUrls.ANNOTATIONS, ApiUrls.ANNOTATIONS_ALTO).params(pi, pageNo, "Word_14").query("format", "oa").build();
        Assert.assertEquals(url, hit1.getAnnotations().get(0).getId().toString());

    }

    /**
     * Test method for
     * {@link io.goobi.viewer.model.iiif.search.SearchResultConverter#getAnnotationsFromFulltext(java.lang.String, java.lang.String, java.lang.Integer, java.lang.String, long, int, int)}.
     */
    @Test
    public void testGetAnnotationsFromFulltext() {

        String query = "in";
        String queryRegex = AbstractSearchParser.getQueryRegex(query);

        AnnotationResultList results = converter.getAnnotationsFromFulltext(text, pi, pageNo, queryRegex, 0, 0, 1000);
        Assert.assertEquals(1, results.hits.size());
        Assert.assertEquals(2, results.hits.get(0).getSelectors().size());
    }

}

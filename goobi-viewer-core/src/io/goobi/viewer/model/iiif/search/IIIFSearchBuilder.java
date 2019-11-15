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
package io.goobi.viewer.model.iiif.search;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.iiif.search.AutoSuggestResult;
import de.intranda.api.iiif.search.SearchHit;
import de.intranda.api.iiif.search.SearchResult;
import de.intranda.api.iiif.search.SearchResultLayer;
import de.intranda.digiverso.ocr.alto.model.structureclasses.Line;
import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.Word;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import de.intranda.digiverso.ocr.alto.model.superclasses.GeometricData;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.annotation.Comment;
import io.goobi.viewer.model.iiif.presentation.builder.AbstractBuilder;
import io.goobi.viewer.model.iiif.search.model.AnnotationResultList;
import io.goobi.viewer.model.iiif.search.model.SearchTermList;
import io.goobi.viewer.model.iiif.search.parser.AbstractSearchParser;
import io.goobi.viewer.model.iiif.search.parser.AltoSearchParser;
import io.goobi.viewer.model.viewer.StringPair;

/**
 * Creates a IIIF Search API v1.0 response as {@link SearchResult}
 * 
 * @author florian
 *
 */
public class IIIFSearchBuilder {

    private static final Logger logger = LoggerFactory.getLogger(IIIFSearchBuilder.class);

    private static final List<String> FULLTEXTFIELDLIST =
            Arrays.asList(new String[] { SolrConstants.FILENAME_ALTO, SolrConstants.FILENAME_FULLTEXT, SolrConstants.ORDER });

    private final String query;
    private final String pi;
    private SearchResultConverter converter;
    private List<String> motivation = new ArrayList<>();
    private String user = null;
    private String date = null;
    private String min = null;
    private int page = 1;
    private int hitsPerPage = 20;

    private final String requestURI;

    /**
     * Initializes the builder with all required parameters
     * 
     * @param requestURI The request url, including all query parameters
     * @param query the query string
     * @param pi the pi of the manifest to search
     */
    public IIIFSearchBuilder(URI requestURI, String query, String pi) {
        this.requestURI = requestURI.toString().replaceAll("&page=\\d+", "");
        if (query != null) {
            query = query.replace("+", " ");
        }
        this.query = query;
        this.pi = pi;
        this.converter = new SearchResultConverter(URI.create(this.requestURI),
                URI.create(DataManager.getInstance().getConfiguration().getRestApiUrl()), pi, 0);
    }

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * @param motivation the motivation to set
     */
    public IIIFSearchBuilder setMotivation(String motivation) {
        if (StringUtils.isNotBlank(motivation)) {
            motivation = motivation.replace("+", " ");
            this.motivation = Arrays.asList(StringUtils.split(motivation, " "));
        }
        return this;
    }

    /**
     * @return the motivation
     */
    public List<String> getMotivation() {
        return motivation;
    }

    /**
     * @param user the user to set
     */
    public IIIFSearchBuilder setUser(String user) {
        this.user = user;
        return this;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param date the date to set
     */
    public IIIFSearchBuilder setDate(String date) {
        this.date = date;
        return this;
    }

    /**
     * @return the date
     */
    public String getDate() {
        return date;
    }

    /**
     * @return the min
     */
    public String getMin() {
        return min;
    }

    /**
     * @param min the min to set
     */
    public IIIFSearchBuilder setMin(String min) {
        this.min = min;
        return this;
    }

    /**
     * @param page the page to set
     */
    public IIIFSearchBuilder setPage(Integer page) {
        if (page != null) {
            this.page = page;
        }
        return this;
    }

    /**
     * @return the page
     */
    public int getPage() {
        return page;
    }

    /**
     * @return the hitsPerPage
     */
    public int getHitsPerPage() {
        return hitsPerPage;
    }

    /**
     * @param hitsPerPage the hitsPerPage to set
     */
    public IIIFSearchBuilder setHitsPerPage(int hitsPerPage) {
        this.hitsPerPage = hitsPerPage;
        return this;
    }

    /**
     * @return a list of all passed paramters that are ignored
     */
    private List<String> getIgnoredParameterList() {
        List<String> ignored = new ArrayList<>();
        if (StringUtils.isNotBlank(getUser())) {
            ignored.add("user");
        }
        if (StringUtils.isNotBlank(getDate())) {
            ignored.add("date");
        }
        if (StringUtils.isNotBlank(getMin())) {
            ignored.add("min");
        }
        return ignored;
    }

    /**
     * Creates a {@link SearchResult} containing annotations matching {@link #getQuery()} within {@link #getPi()}. The answer may contain more than
     * {@link #getHitsPerPage()} hits if more than one motivation is searched, but no more than {@link #getHitsPerPage()} hits per motivation.
     * 
     * @return the search result
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public SearchResult build() throws PresentationException, IndexUnreachableException {

        AnnotationResultList resultList = new AnnotationResultList();

        long mostHits = 0;
        long total = 0;
        if (StringUtils.isNotBlank(query)) {
            if (motivation.isEmpty() || motivation.contains("painting")) {
                AnnotationResultList fulltextAnnotations = searchFulltext(query, pi, getFirstHitIndex(getPage()), getHitsPerPage());
                resultList.add(fulltextAnnotations);
                mostHits = Math.max(mostHits, fulltextAnnotations.numHits);
                total += fulltextAnnotations.numHits;
            }
            if (motivation.isEmpty() || motivation.contains("non-painting") || motivation.contains("describing")) {
                AnnotationResultList annotations = searchAnnotations(query, pi, getFirstHitIndex(getPage()), getHitsPerPage());
                resultList.add(annotations);
                mostHits = Math.max(mostHits, annotations.numHits);
                total += annotations.numHits;

            }
            if (motivation.isEmpty() || motivation.contains("non-painting") || motivation.contains("describing")) {
                AnnotationResultList metadata = searchMetadata(query, pi, getFirstHitIndex(getPage()), getHitsPerPage());
                resultList.add(metadata);
                mostHits = Math.max(mostHits, metadata.numHits);
                total += metadata.numHits;

            }
            if (motivation.isEmpty() || motivation.contains("non-painting") || motivation.contains("commenting")) {
                AnnotationResultList annotations = searchComments(query, pi, getFirstHitIndex(getPage()), getHitsPerPage());
                resultList.add(annotations);
                mostHits = Math.max(mostHits, annotations.numHits);
                total += annotations.numHits;

            }
        }

        int lastPageNo = 1 + (int) mostHits / getHitsPerPage();

        SearchResult searchResult = new SearchResult(getURI(getPage()));
        searchResult.setResources(resultList.annotations);
        searchResult.setHits(resultList.hits);
        searchResult.setStartIndex(getFirstHitIndex(getPage()));

        if (getPage() > 1) {
            searchResult.setPrev(getURI(getPage() - 1));
        }
        if (getPage() < lastPageNo) {
            searchResult.setNext(getURI(getPage() + 1));
        }
        SearchResultLayer layer = new SearchResultLayer();
        layer.setTotal(total);
        layer.setIgnored(getIgnoredParameterList());
        layer.setFirst(getURI(1));
        layer.setLast(getURI(lastPageNo));
        searchResult.setWithin(layer);

        return searchResult;
    }

    /**
     * Creates a {@link AutoSuggestResult} containing searchTerms matching {@link #getQuery()} within {@link #getPi()}.
     * 
     * @return The searchTerm list
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public AutoSuggestResult buildAutoSuggest() throws PresentationException, IndexUnreachableException {

        SearchTermList terms = new SearchTermList();
        if (StringUtils.isNotBlank(query)) {
            if (motivation.isEmpty() || motivation.contains("painting")) {
                //add terms from fulltext?
            }
            if (motivation.isEmpty() || motivation.contains("non-painting") || motivation.contains("describing")) {
                terms.addAll(autoSuggestAnnotations(query, getPi()));
            }
            if (motivation.isEmpty() || motivation.contains("non-painting") || motivation.contains("describing")) {
                terms.addAll(autoSuggestMetadata(query, getPi()));
            }
            if (motivation.isEmpty() || motivation.contains("non-painting") || motivation.contains("commenting")) {
                terms.addAll(autoSuggestComments(query, getPi()));
            }
        }

        AutoSuggestResult result = new AutoSuggestResult(converter.getPresentationBuilder().getAutoSuggestURI(getPi(), getQuery(), getMotivation()));
        result.setIgnored(getIgnoredParameterList());
        result.setTerms(terms);
        return result;
    }

    private AnnotationResultList searchComments(String query, String pi, int firstHitIndex, int hitsPerPage) {

        AnnotationResultList results = new AnnotationResultList();
        String queryRegex = AbstractSearchParser.getQueryRegex(query);

        try {
            List<Comment> comments = DataManager.getInstance().getDao().getCommentsForWork(pi, false);
            comments = comments.stream()
                    .filter(c -> c.getText().matches(AbstractSearchParser.getContainedWordRegex(queryRegex)))
                    .collect(Collectors.toList());
            if (firstHitIndex < comments.size()) {
                comments = comments.subList(firstHitIndex, Math.min(firstHitIndex + hitsPerPage, comments.size()));
                for (Comment comment : comments) {
                    results.add(converter.convertCommentToHit(queryRegex, pi, comment));
                }
            }
        } catch (DAOException e) {
            logger.error(e.toString(), e);
        }
        return results;
    }

    private SearchTermList autoSuggestComments(String query, String pi) {

        SearchTermList terms = new SearchTermList();
        String queryRegex = AbstractSearchParser.getAutoSuggestRegex(query);

        try {
            List<Comment> comments = DataManager.getInstance().getDao().getCommentsForWork(pi, false);
            comments = comments.stream()
                    .filter(c -> c.getText().matches(AbstractSearchParser.getContainedWordRegex(queryRegex)))
                    .collect(Collectors.toList());
            for (Comment comment : comments) {
                terms.addAll(converter.getSearchTerms(queryRegex, comment.getText(), getMotivation()));
            }
        } catch (DAOException e) {
            logger.error(e.toString(), e);
        }
        return terms;
    }

    private AnnotationResultList searchMetadata(String query, String pi, int firstHitIndex, int hitsPerPage) {

        AnnotationResultList results = new AnnotationResultList();
        List<String> searchFields = getSearchFields();
        List<String> displayFields = DataManager.getInstance().getConfiguration().getIIIFMetadataFields();

        if (searchFields.isEmpty()) {
            return results;
        }

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" +PI_TOPSTRUCT:").append(pi);
        queryBuilder.append(" +DOCTYPE:DOCSTRCT");
        queryBuilder.append(" +( ");
        for (String field : searchFields) {
            queryBuilder.append(field).append(":").append(query).append(" ");
        }
        queryBuilder.append(")");

        try {
            SolrDocumentList docList = DataManager.getInstance()
                    .getSearchIndex()
                    .search(queryBuilder.toString(), SolrSearchIndex.MAX_HITS, getDocStructSortFields(),
                            converter.getPresentationBuilder().getSolrFieldList());
            for (SolrDocument doc : docList) {
                Map<String, List<String>> fieldNames = SolrSearchIndex.getFieldValueMap(doc);
                for (String fieldName : fieldNames.keySet()) {
                    if (fieldNameMatches(fieldName, displayFields)) {
                        String fieldValue = fieldNames.get(fieldName).stream().collect(Collectors.joining(" "));
                        String containesWordRegex = AbstractSearchParser.getContainedWordRegex(AbstractSearchParser.getQueryRegex(query));
                        if (fieldValue.matches(containesWordRegex)) {
                            if (firstHitIndex < results.numHits && firstHitIndex + hitsPerPage >= results.numHits) {
                                SearchHit hit = converter.convertMetadataToHit(AbstractSearchParser.getQueryRegex(query), fieldName, doc);
                                results.add(hit);
                                results.annotations.addAll(hit.getAnnotations());
                            }
                        }
                    }
                }
            }
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error(e.toString(), e);
        }
        return results;
    }

    private SearchTermList autoSuggestMetadata(String query, String pi) {

        SearchTermList terms = new SearchTermList();
        List<String> searchFields = getSearchFields();
        List<String> displayFields = DataManager.getInstance().getConfiguration().getIIIFMetadataFields();

        if (searchFields.isEmpty()) {
            return terms;
        }

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" +PI_TOPSTRUCT:").append(pi);
        queryBuilder.append(" +DOCTYPE:DOCSTRCT");
        queryBuilder.append(" +( ");
        for (String field : searchFields) {
            queryBuilder.append(field).append(":").append(query).append("* ");
        }
        queryBuilder.append(")");

        try {
            SolrDocumentList docList = DataManager.getInstance()
                    .getSearchIndex()
                    .search(queryBuilder.toString(), SolrSearchIndex.MAX_HITS, getDocStructSortFields(),
                            converter.getPresentationBuilder().getSolrFieldList());
            for (SolrDocument doc : docList) {
                Map<String, List<String>> fieldNames = SolrSearchIndex.getFieldValueMap(doc);
                for (String fieldName : fieldNames.keySet()) {
                    if (fieldNameMatches(fieldName, displayFields)) {
                        String fieldValue = fieldNames.get(fieldName).stream().collect(Collectors.joining(" "));
                        if (fieldValue.matches(AbstractSearchParser.getContainedWordRegex(AbstractSearchParser.getAutoSuggestRegex(query)))) {
                            terms.addAll(converter.getSearchTerms(AbstractSearchParser.getAutoSuggestRegex(query), fieldValue, getMotivation()));
                        }
                    }
                }
            }
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error(e.toString(), e);
        }
        return terms;
    }

    /**
     * @param queryRegex
     * @param pi2
     * @param firstHitIndex
     * @param hitsPerPage2
     * @return
     */
    private AnnotationResultList searchAnnotations(String query, String pi, int firstHitIndex, int hitsPerPage) {

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" +PI_TOPSTRUCT:").append(pi);
        queryBuilder.append(" +DOCTYPE:UGC");
        queryBuilder.append(" +UGCTERMS:").append(query);

        AnnotationResultList results = new AnnotationResultList();
        try {
            SolrDocumentList docList = DataManager.getInstance()
                    .getSearchIndex()
                    .search(queryBuilder.toString(), SolrSearchIndex.MAX_HITS, getPageSortFields(), Arrays.asList(AbstractBuilder.UGC_SOLR_FIELDS));
            if (firstHitIndex < docList.size()) {
                List<SolrDocument> filteredDocList = docList.subList(firstHitIndex, Math.min(firstHitIndex + hitsPerPage, docList.size()));
                for (SolrDocument doc : filteredDocList) {
                    results.add(converter.convertUGCToHit(AbstractSearchParser.getQueryRegex(query), doc));

                }
            }
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error(e.toString(), e);
        }
        return results;
    }

    private SearchTermList autoSuggestAnnotations(String query, String pi) {

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" +PI_TOPSTRUCT:").append(pi);
        queryBuilder.append(" +DOCTYPE:UGC");
        queryBuilder.append(" +UGCTERMS:").append(query).append("*");

        SearchTermList terms = new SearchTermList();
        try {
            SolrDocumentList docList = DataManager.getInstance()
                    .getSearchIndex()
                    .search(queryBuilder.toString(), SolrSearchIndex.MAX_HITS, getPageSortFields(), Arrays.asList(AbstractBuilder.UGC_SOLR_FIELDS));
            for (SolrDocument doc : docList) {
                terms.addAll(converter.getSearchTerms(AbstractSearchParser.getAutoSuggestRegex(query), doc,
                        Collections.singletonList(SolrConstants.UGCTERMS), getMotivation()));
            }
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error(e.toString(), e);
        }
        return terms;
    }

    private AnnotationResultList searchFulltext(String query, String pi, int firstIndex, int numHits)
            throws PresentationException, IndexUnreachableException {

        //replace search wildcards with word character regex and replace whitespaces with '|' to facilitate OR search
        String queryRegex = AbstractSearchParser.getQueryRegex(query);

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" +PI_TOPSTRUCT:").append(pi);
        queryBuilder.append(" +DOCTYPE:PAGE");
        queryBuilder.append(" +FULLTEXTAVAILABLE:true");
        queryBuilder.append(" +FULLTEXT:").append(query);

        AnnotationResultList results = new AnnotationResultList();

        //        QueryResponse response = DataManager.getInstance().getSearchIndex().search(queryBuilder.toString(), (page-1)*getHitsPerPage(), getHitsPerPage(), Collections.singletonList(sortField), null, FULLTEXTFIELDLIST);
        SolrDocumentList docList = DataManager.getInstance()
                .getSearchIndex()
                .search(queryBuilder.toString(), SolrSearchIndex.MAX_HITS, getPageSortFields(), FULLTEXTFIELDLIST);
        for (SolrDocument doc : docList) {
            Path altoFile = getPath(pi, SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.FILENAME_ALTO));
            Path fulltextFile = getPath(pi, SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.FILENAME_FULLTEXT));
            Integer pageNo = SolrSearchIndex.getAsInt(doc.getFieldValue(SolrConstants.ORDER));
            converter.setPageNo(pageNo);
            try {
                if (altoFile != null && Files.exists(altoFile)) {
                    AltoDocument altoDoc = AltoDocument.getDocumentFromFile(altoFile.toFile());
                    results.add(converter.getAnnotationsFromAlto(altoDoc, queryRegex));
                } else if (fulltextFile != null && Files.exists(fulltextFile)) {
                    String text = new String(Files.readAllBytes(fulltextFile), "utf-8");
                    results.add(converter.getAnnotationsFromFulltext(text, pi, pageNo, queryRegex, results.numHits, firstIndex, numHits));
                }
            } catch (IOException | JDOMException e) {
                logger.error("Error reading " + fulltextFile, e);
            }
        }
        return results;
    }

    /**
     * Test if the given fieldName is included in the configuredFields or matches any of the contained wildcard fieldNames
     */
    private boolean fieldNameMatches(String fieldName, List<String> configuredFields) {
        for (String configuredField : configuredFields) {
            if (configuredField.contains("*")) {
                String fieldRegex = AbstractSearchParser.getQueryRegex(configuredField);
                if (fieldName.matches(fieldRegex)) {
                    return true;
                }
            } else if (fieldName.equalsIgnoreCase(configuredField)) {
                return true;
            }
        }
        return false;
    }

    private List<StringPair> getPageSortFields() {
        StringPair sortField = new StringPair(SolrConstants.ORDER, "asc");
        return Collections.singletonList(sortField);
    }

    private List<StringPair> getDocStructSortFields() {
        StringPair sortField1 = new StringPair(SolrConstants.THUMBPAGENO, "asc");
        StringPair sortField2 = new StringPair(SolrConstants.ISANCHOR, "asc");
        StringPair sortField3 = new StringPair(SolrConstants.ISWORK, "asc");
        List<StringPair> pairs = new ArrayList<>();
        pairs.add(sortField1);
        pairs.add(sortField2);
        pairs.add(sortField3);
        return pairs;
    }

    private List<String> getSearchFields() {
        List<String> configuredFields = DataManager.getInstance().getConfiguration().getIIIFMetadataFields();
        if (configuredFields.stream().anyMatch(field -> field.contains("*"))) {
            configuredFields = configuredFields.stream().filter(field -> !field.contains("*")).collect(Collectors.toList());
            configuredFields.add(SolrConstants.DEFAULT);
        }
        return configuredFields;
    }

    private int getFirstHitIndex(int pageNo) {
        return (pageNo - 1) * getHitsPerPage();
    }

    private Path getPath(String pi, String filename) throws PresentationException, IndexUnreachableException {
        if (StringUtils.isBlank(filename)) {
            return null;
        }
        String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepository(pi);
        Path filePath = Paths.get(Helper.getRepositoryPath(dataRepository), filename);

        return filePath;
    }

    /**
     * @return
     */
    private URI getURI(int page) {
        return URI.create(requestURI + "&page=" + page);
    }
}

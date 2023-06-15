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
package io.goobi.viewer.model.cms.pages.content.types;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.itemfunctionality.Functionality;
import io.goobi.viewer.model.cms.itemfunctionality.SearchFunctionality;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import io.goobi.viewer.model.cms.pages.content.PagedCMSContent;
import io.goobi.viewer.model.search.HitListView;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.search.SearchAggregationType;
import io.goobi.viewer.model.search.SearchFacets;
import io.goobi.viewer.model.search.SearchHelper;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "cms_content_record_list")
@DiscriminatorValue("recordlist")
public class CMSRecordListContent extends CMSContent implements PagedCMSContent {

    private static final Logger logger = LogManager.getLogger(CMSRecordListContent.class); //NOSONAR Sometimes the logger is needed for debugging

    private static final String COMPONENT_NAME = "searchhitlist";

    @Column(name = "solr_query")
    private String solrQuery = "";
    @Column(name = "sort_field", length=40)
    private String sortField = "";
    @Column(name = "grouping_field", length=40)
    private String groupingField = "";
    @Column(name = "result_group", columnDefinition = "VARCHAR(40)")
    private String resultGroupName;
    @Column(name = "include_structure_elements")
    private boolean includeStructureElements = false;
    @Column(name = "elements_per_page")
    private int elementsPerPage = DataManager.getInstance().getConfiguration().getSearchHitsPerPageDefaultValue();
    @Column(name = "view")
    private HitListView view = HitListView.DETAILS;

    @Transient
    private SearchFunctionality search = null;

    public CMSRecordListContent() {
        super();
    }

    private CMSRecordListContent(CMSRecordListContent orig) {
        super(orig);
        this.solrQuery = orig.solrQuery;
        this.sortField = orig.sortField;
        this.groupingField = orig.groupingField;
        this.includeStructureElements = orig.includeStructureElements;
        this.elementsPerPage = orig.elementsPerPage;
        this.resultGroupName = orig.resultGroupName;
        this.view = orig.view;
    }

    private SearchFunctionality initSearch() {
        if (this.getOwningComponent() != null) {
            SearchFunctionality func = new SearchFunctionality(this.solrQuery, this.getOwningComponent().getOwningPage().getPageUrl());
            func.setPageNo(getCurrentListPage());
            return func;
        }
        return new SearchFunctionality(this.solrQuery, "");
    }

    @Override
    public String getBackendComponentName() {
        return COMPONENT_NAME;
    }

    public String getSolrQuery() {
        return solrQuery;
    }

    public void setSolrQuery(String solrQuery) {
        this.solrQuery = solrQuery;
    }

    public String getGroupingField() {
        return groupingField;
    }

    public void setGroupingField(String groupingField) {
        this.groupingField = groupingField;
    }

    /**
     * @return the resultGroupName
     */
    public String getResultGroupName() {
        return resultGroupName;
    }

    /**
     * @param resultGroupName the resultGroupName to set
     */
    public void setResultGroupName(String resultGroupName) {
        this.resultGroupName = resultGroupName;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public boolean isIncludeStructureElements() {
        return includeStructureElements;
    }

    public void setIncludeStructureElements(boolean includeStructureElements) {
        this.includeStructureElements = includeStructureElements;
    }

    public SearchFunctionality getSearch() {
        return search;
    }

    public int getElementsPerPage() {
        return elementsPerPage;
    }

    public void setElementsPerPage(int elementsPerPage) {
        this.elementsPerPage = elementsPerPage;
    }

    @Override
    public CMSContent copy() {
        return new CMSRecordListContent(this);
    }

    @Override
    public List<File> exportHtmlFragment(String outputFolderPath, String namingScheme) throws IOException, ViewerConfigurationException {
        return Collections.emptyList();
    }

    @Override
    public String handlePageLoad(boolean resetResults) throws PresentationException {
        logger.trace("handlePageLoad");
        if (this.search == null) {
            this.search = initSearch();
            //store search in session bean so it will be available when reloading a page
            //otherwise "submitSearch" button will not work properly because search isn't available anymore
            BeanUtils.getSessionBean().put("cmsSearch", this.search);
        }
        try {
            SearchBean searchBean = BeanUtils.getSearchBean();

            // Set configured result group on SearchBean, if available (before initializing Search)
            if (StringUtils.isNotBlank(resultGroupName)) {
                searchBean.setActiveResultGroupName(resultGroupName);
            }

            Search s = new Search(SearchHelper.SEARCH_TYPE_REGULAR, SearchHelper.SEARCH_FILTER_ALL, searchBean.getResultGroupsForSearchExecution());
            if (StringUtils.isNotBlank(this.getSortField())) {
                s.setSortString(this.getSortField());
                searchBean.setSortString(this.getSortField());
            } else if (StringUtils.isNotBlank(this.search.getSortString()) && !this.search.getSortString().equals("-")) {
                s.setSortString(this.search.getSortString());
                searchBean.setSortString(this.search.getSortString());
            } else if (StringUtils.isEmpty(s.getSortString()) && searchBean.getSortString().equals("-")) {
                s.setSortString(searchBean.getSortString());
            }
            //NOTE: Cannot sort by multivalued fields like DC.
            if (StringUtils.isNotBlank(this.getGroupingField())) {
                String sortString = s.getSortString() == null ? "" : s.getSortString().replace("-", "");
                sortString = this.getGroupingField() + ";" + sortString;
                s.setSortString(sortString);
            } else {
                String sortString = s.getSortString() == null ? "" : s.getSortString().replace("-", "");
                s.setSortString(sortString);
            }
            SearchFacets facets = searchBean.getFacets();
            s.setPage(getCurrentListPage());
            searchBean.setHitsPerPage(this.getElementsPerPage());
            searchBean.setLastUsedSearchPage();
            s.setCustomFilterQuery(this.solrQuery);
            s.execute(facets, null, searchBean.getHitsPerPage(), BeanUtils.getLocale(), true,
                    this.isIncludeStructureElements() ? SearchAggregationType.NO_AGGREGATION : SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);
            searchBean.setCurrentSearch(s);
            searchBean.setHitsPerPageSetterCalled(false);
            return null;
        } catch (PresentationException | IndexUnreachableException | DAOException | ViewerConfigurationException e) {
            throw new PresentationException("Error initializing search hit list on page load", e);
        }
    }

    @Override
    public String getData(Integer w, Integer h) {
        return "";
    }

    /**
     * Alias for {@link #getSearch()}. Used in legacy templates
     * 
     * @return
     */
    public Functionality getFunctionality() {
        return getSearch();
    }

    public HitListView getView() {
        return view;
    }

    public void setView(HitListView view) {
        this.view = view;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
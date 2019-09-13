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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.faces.url.URL;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.tabledata.TableDataFilter;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.CampaignVisibility;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CampaignRecordStatus;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.security.user.User;

@Named
@SessionScoped
public class CrowdsourcingBean implements Serializable {

    private static final long serialVersionUID = -6452528640177147828L;

    private static final Logger logger = LoggerFactory.getLogger(CrowdsourcingBean.class);

    private static final int DEFAULT_ROWS_PER_PAGE = 15;

    @Inject
    private NavigationHelper navigationHelper;
    @Inject
    private UserBean userBean;

    private TableDataProvider<Campaign> lazyModelCampaigns;
    /**
     * The campaign selected in backend
     */
    private Campaign selectedCampaign;
    /**
     * The campaign being annotated/reviewed
     */
    private Campaign targetCampaign = null;
    private String targetIdentifier;
    private boolean editMode = false;

    @PostConstruct
    public void init() {
        if (lazyModelCampaigns == null) {
            lazyModelCampaigns = new TableDataProvider<>(new TableDataSource<Campaign>() {

                private Optional<Long> numCreatedPages = Optional.empty();

                @Override
                public List<Campaign> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                    try {
                        if (StringUtils.isBlank(sortField)) {
                            sortField = "id";
                        }

                        List<Campaign> ret =
                                DataManager.getInstance().getDao().getCampaigns(first, pageSize, sortField, sortOrder.asBoolean(), filters);
                        return ret;
                    } catch (DAOException e) {
                        logger.error("Could not initialize lazy model: {}", e.getMessage());
                    }

                    return Collections.emptyList();
                }

                @Override
                public long getTotalNumberOfRecords(Map<String, String> filters) {
                    if (!numCreatedPages.isPresent()) {
                        try {
                            numCreatedPages = Optional.ofNullable(DataManager.getInstance().getDao().getCampaignCount(filters));
                        } catch (DAOException e) {
                            logger.error("Unable to retrieve total number of campaigns", e);
                        }
                    }
                    return numCreatedPages.orElse(0l);
                }

                @Override
                public void resetTotalNumberOfRecords() {
                    numCreatedPages = Optional.empty();
                }
            });
            lazyModelCampaigns.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
            //            lazyModelCampaigns.addFilter("CMSPageLanguageVersion", "title_menuTitle");
            //            lazyModelCampaigns.addFilter("classifications", "classification");
        }
    }

    /**
     * 
     * @param visibility
     * @return
     * @throws DAOException
     */
    public long getCampaignCount(CampaignVisibility visibility) throws DAOException {
        Map<String, String> filters = visibility != null ? Collections.singletonMap("visibility", visibility.name()) : null;
        return DataManager.getInstance().getDao().getCampaignCount(filters);
    }

    /**
     * 
     * @param visibility
     * @return
     */
    public String filterCampaignsAction(CampaignVisibility visibility) {
        lazyModelCampaigns.resetFilters();
        if (visibility != null) {
            lazyModelCampaigns.addFilter(new TableDataFilter("visibility", visibility.name()));
        }

        return "";
    }

    /**
     * @return
     */
    public static List<Locale> getAllLocales() {
        List<Locale> list = new LinkedList<>();
        list.add(ViewerResourceBundle.getDefaultLocale());
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getApplication() != null) {
            Iterator<Locale> iter = FacesContext.getCurrentInstance().getApplication().getSupportedLocales();
            while (iter.hasNext()) {
                Locale locale = iter.next();
                if (!list.contains(locale)) {
                    list.add(locale);
                }
            }
        }
        return list;
    }

    public String createNewCampaignAction() {
        selectedCampaign = new Campaign(ViewerResourceBundle.getDefaultLocale());
        return "pretty:adminCrowdAddCampaign";
    }

    public String editCampaignAction(Campaign campaign) {
        selectedCampaign = campaign;
        return "pretty:adminCrowdEditCampaign";
    }

    public String deleteCampaignAction(Campaign campaign) throws DAOException {
        if (campaign != null) {
            if (DataManager.getInstance().getDao().deleteCampaign(campaign)) {
                Messages.info("admin__crowdsoucing_campaign_deleteSuccess");
                lazyModelCampaigns.update();
            }
        }

        return "";
    }

    public String addNewQuestionAction() {
        if (selectedCampaign != null) {
            selectedCampaign.getQuestions().add(new Question(selectedCampaign));
            selectedCampaign.setDirty(true);
        }

        return "";
    }

    public String removeQuestionAction(Question question) {
        if (selectedCampaign != null && question != null) {
            selectedCampaign.getQuestions().remove(question);
            selectedCampaign.setDirty(true);
        }

        return "";
    }

    /**
     * Resets dateStart + dateEnd to null.
     * 
     * @return
     */
    public String resetDurationAction() {
        if (selectedCampaign != null) {
            selectedCampaign.setDateStart(null);
            selectedCampaign.setDateEnd(null);
        }

        return "";
    }

    /**
     * @return
     * @throws DAOException
     */
    public List<Campaign> getAllCampaigns() throws DAOException {
        List<Campaign> pages = DataManager.getInstance().getDao().getAllCampaigns();
        return pages;
    }

    public List<Campaign> getAllCampaigns(CampaignVisibility visibility) throws DAOException {
        List<Campaign> pages = DataManager.getInstance()
                .getDao()
                .getAllCampaigns()
                .stream()
                .filter(camp -> visibility.equals(camp.getVisibility()))
                .collect(Collectors.toList());
        return pages;
    }

    /**
     * 
     * @param user
     * @return
     * @throws DAOException
     */
    public List<Campaign> getAllowedCampaigns(User user) throws DAOException {
        logger.trace("getAllowedCampaigns");
        List<Campaign> allCampaigns = getAllCampaigns();
        if (allCampaigns.isEmpty()) {
            logger.trace("No campaigns found");
            return Collections.emptyList();
        }
        // Logged in
        if (user != null) {
            return user.getAllowedCrowdsourcingCampaigns(allCampaigns);
        }

        // Not logged in - only public campaigns
        List<Campaign> ret = new ArrayList<>(allCampaigns.size());
        for (Campaign campaign : allCampaigns) {
            if (CampaignVisibility.PUBLIC.equals(campaign.getVisibility())) {
                ret.add(campaign);
            }
        }

        return ret;
    }

    /**
     * Adds the current page to the database, if it doesn't exist or updates it otherwise
     *
     * @throws DAOException
     * @throws IndexUnreachableException 
     * @throws PresentationException 
     *
     */
    public void saveSelectedCampaign() throws DAOException, PresentationException, IndexUnreachableException {
        logger.trace("saveSelectedCampaign");
        try {
            if (userBean == null || !userBean.getUser().isSuperuser()) {
                // Only authorized admins may save
                return;
            }
            if (selectedCampaign == null) {
                return;
            }

            // Save
            boolean success = false;
            Date now = new Date();
            if (selectedCampaign.getDateCreated() == null) {
                selectedCampaign.setDateCreated(now);
            }
            selectedCampaign.setDateUpdated(now);
            if (selectedCampaign.getId() != null) {
                success = DataManager.getInstance().getDao().updateCampaign(selectedCampaign);
            } else {
                success = DataManager.getInstance().getDao().addCampaign(selectedCampaign);
            }
            if (success) {
                selectedCampaign.setDirty(false);
                Messages.info("crowdsoucing_campaignSaveSuccess");
                setSelectedCampaign(selectedCampaign);
                lazyModelCampaigns.update();
                updateActiveCampaigns();
            } else {
                Messages.error("crowdsourcing_campaignSaveFailure");
            }
        } finally {
        }
    }

    public String getCampaignsRootUrl() {
        return navigationHelper.getApplicationUrl() + "campaigns/";
    }

    /**
     * @return the lazyModelCampaigns
     */
    public TableDataProvider<Campaign> getLazyModelCampaigns() {
        return lazyModelCampaigns;
    }

    /**
     * @return the selectedCampaign
     */
    public Campaign getSelectedCampaign() {
        return selectedCampaign;
    }

    /**
     * @param selectedCampaign the selectedCampaign to set
     */
    public void setSelectedCampaign(Campaign selectedCampaign) {
        this.selectedCampaign = selectedCampaign;
    }

    /**
     * @return the editMode
     */
    public boolean isEditMode() {
        return editMode;
    }

    /**
     * @param editMode the editMode to set
     */
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public String getSelectedCampaignId() {
        Long id = Optional.ofNullable(getSelectedCampaign()).map(Campaign::getId).orElse(null);
        return id == null ? null : id.toString();
    }

    public void setSelectedCampaignId(String id) throws DAOException {
        if (id != null) {
            Campaign campaign = DataManager.getInstance().getDao().getCampaign(Long.parseLong(id));
            setSelectedCampaign(campaign);
        } else {
            setSelectedCampaign(null);
        }
    }

    /**
     * @return the targetCampaign
     */
    public Campaign getTargetCampaign() {
        return targetCampaign;
    }

    /**
     * @param targetCampaign the targetCampaign to set
     */
    public void setTargetCampaign(Campaign targetCampaign) {
        this.targetCampaign = targetCampaign;
    }

    /**
     * @return the targetCampaign
     */
    public String getTargetCampaignId() {
        Long id = Optional.ofNullable(getTargetCampaign()).map(Campaign::getId).orElse(null);
        return id == null ? null : id.toString();
    }

    /**
     * @param targetCampaign the targetCampaign to set
     * @throws DAOException
     * @throws NumberFormatException
     */
    public void setTargetCampaignId(String id) throws NumberFormatException, DAOException {
        if (id != null) {
            Campaign campaign = DataManager.getInstance().getDao().getCampaign(Long.parseLong(id));
            setTargetCampaign(campaign);
        } else {
            setTargetCampaign(null);
        }
    }

    public void setRandomIdentifierForAnnotation() throws PresentationException, IndexUnreachableException {
        if (getTargetCampaign() != null) {
            String pi = getTargetCampaign().getRandomizedTarget(CampaignRecordStatus.ANNOTATE);
            setTargetIdentifier(pi);

        }
    }

    public void setRandomIdentifierForReview() throws PresentationException, IndexUnreachableException {
        if (getTargetCampaign() != null) {
            String pi = getTargetCampaign().getRandomizedTarget(CampaignRecordStatus.REVIEW);
            setTargetIdentifier(pi);

        }
    }

    public String forwardToAnnotationTarget() {
        return "pretty:crowdCampaignAnnotate2";
    }

    public String forwardToReviewTarget() {
        return "pretty:crowdCampaignReview2";
    }

    /**
     * @return the PI of a work selected for editing
     */
    public String getTargetIdentifier() {
        return this.targetIdentifier;
    }

    /**
     * @return the PI of a work selected for editing or "-" if no targetIdentifier exists
     */
    public String getTargetIdentifierForUrl() {
        return StringUtils.isBlank(this.targetIdentifier) ? "-" : this.targetIdentifier;
    }

    public void setTargetIdentifierForUrl(String pi) {
        this.targetIdentifier = "-".equals(pi) ? null : pi;
    }

    /**
     * @param targetIdentifier the targetIdentifier to set
     */
    public void setTargetIdentifier(String targetIdentifier) {
        this.targetIdentifier = targetIdentifier;
    }

    public String forwardToCrowdsourcingAnnotation(Campaign campaign) {
        setTargetCampaign(campaign);
        return "pretty:crowdCampaignAnnotate1";
    }

    public String forwardToCrowdsourcingReview(Campaign campaign) {
        setTargetCampaign(campaign);
        return "pretty:crowdCampaignReview1";
    }

    public String forwardToCrowdsourcingAnnotation(Campaign campaign, String pi) {
        setTargetCampaign(campaign);
        setTargetIdentifier(pi);
        return "pretty:crowdCampaignAnnotate2";
    }

    public String forwardToCrowdsourcingReview(Campaign campaign, String pi) {
        setTargetCampaign(campaign);
        setTargetIdentifier(pi);
        return "pretty:crowdCampaignReview2";
    }

    public String getRandomItemUrl(Campaign campaign, CampaignRecordStatus status) {
        String mappingId = CampaignRecordStatus.REVIEW.equals(status) ? "crowdCampaignReview1" : "crowdCampaignAnnotate1";
        URL mappedUrl = PrettyContext.getCurrentInstance().getConfig().getMappingById(mappingId).getPatternParser().getMappedURL(campaign.getId());
        logger.debug("Mapped URL " + mappedUrl);
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + mappedUrl.toString();
    }

    public CampaignRecordStatus getTargetRecordStatus() {
        if (getTargetCampaign() != null && StringUtils.isNotBlank(getTargetIdentifier())) {
            return getTargetCampaign().getRecordStatus(getTargetIdentifier());
        }
        return null;
    }

    /**
     * 
     * @return Active campaigns for the given identifier that are visible to the current user
     * @throws IndexUnreachableException 
     * @throws PresentationException 
     * @throws DAOException 
     */
    public List<Campaign> getActiveCampaignsForRecord(String pi) throws DAOException, PresentationException, IndexUnreachableException {
        logger.trace("getActiveCampaignsForRecord: {}", pi);
        if (pi == null) {
            return Collections.emptyList();
        }

        // If the map has not yet been initialized during the application's life cycle, make it so
        if (DataManager.getInstance().getRecordCampaignMap() == null) {
            updateActiveCampaigns();
        }

        List<Campaign> allActiveCampaigns = DataManager.getInstance().getRecordCampaignMap().get(pi);
        if (allActiveCampaigns == null || allActiveCampaigns.isEmpty()) {
            logger.trace("No campaigns found for {}", pi);
            return Collections.emptyList();
        }
        logger.trace("Found {} total campaigns for {}", allActiveCampaigns.size(), pi);

        if (userBean.isLoggedIn()) {
            logger.trace("user logged in");
            return userBean.getUser().getAllowedCrowdsourcingCampaigns(allActiveCampaigns);
        }

        List<Campaign> ret = new ArrayList<>(allActiveCampaigns.size());
        for (Campaign campaign : allActiveCampaigns) {
            if (CampaignVisibility.PUBLIC.equals(campaign.getVisibility())) {
                ret.add(campaign);
            }
        }

        logger.trace("Returning {} public campaigns for {}", ret.size(), pi);
        return ret;
    }

    /**
     * Searches for all identifiers that are encompassed by the Solr query of each active campaign and initializes and fills a map of active campaigns
     * for each identifier. Should be called once after the application first starts (or upon first access) or when a campaign has been updated.
     * 
     * @throws DAOException
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public void updateActiveCampaigns() throws DAOException, PresentationException, IndexUnreachableException {
        logger.trace("updateActiveCampaigns");
        DataManager.getInstance().setRecordCampaignMap(new HashMap<>());

        List<Campaign> allCampaigns = getAllCampaigns();
        if (allCampaigns.isEmpty()) {
            return;
        }

        for (Campaign campaign : allCampaigns) {
            // Skip inactive campaigns
            if (!CampaignVisibility.PUBLIC.equals(campaign.getVisibility()) && !CampaignVisibility.RESTRICTED.equals(campaign.getVisibility())) {
                continue;
            }
            QueryResponse qr = DataManager.getInstance()
                    .getSearchIndex()
                    .searchFacetsAndStatistics(campaign.getSolrQuery(), Collections.singletonList(SolrConstants.PI_TOPSTRUCT), 1, false);
            if (qr.getFacetField(SolrConstants.PI_TOPSTRUCT) != null) {
                for (Count count : qr.getFacetField(SolrConstants.PI_TOPSTRUCT).getValues()) {
                    String pi = count.getName();
                    List<Campaign> list = DataManager.getInstance().getRecordCampaignMap().get(pi);
                    if (list == null) {
                        list = new ArrayList<>();
                        DataManager.getInstance().getRecordCampaignMap().put(pi, list);
                    }
                    if (!list.contains(campaign)) {
                        list.add(campaign);
                    }
                }
            }
        }
        logger.trace("Added {} identifiers to the map.",  DataManager.getInstance().getRecordCampaignMap().size());
    }
}
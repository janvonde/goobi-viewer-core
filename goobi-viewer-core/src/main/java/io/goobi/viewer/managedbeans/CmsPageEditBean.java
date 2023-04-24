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

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.IndexerTools;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.faces.utils.SelectItemBuilder;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.Selectable;
import io.goobi.viewer.model.cms.SelectableNavigationItem;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.CMSPageEditState;
import io.goobi.viewer.model.cms.pages.CMSPageTemplate;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.widgets.WidgetDisplayElement;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;

@Named
@ViewScoped
public class CmsPageEditBean implements Serializable {

    private static final long serialVersionUID = 7163586584773468296L;
    private static final Logger logger = LogManager.getLogger(CmsPageEditBean.class);

    @Inject
    transient IDAO dao;
    @Inject
    transient CMSTemplateManager templateManager;
    @Inject
    transient UserBean userBean;
    @Inject
    transient CmsBean cmsBean;
    @Inject
    transient CmsNavigationBean navigationBean;
    @Inject
    transient CMSSidebarWidgetsBean widgetsBean;
    @Inject
    transient CollectionViewBean collectionViewBean;
    @Inject
    transient FacesContext facesContext;

    private CMSPage selectedPage = null;
    private boolean editMode = false;
    private CMSPageEditState pageEditState = CMSPageEditState.CONTENT;
    private String selectedComponent = "";

    private Map<WidgetDisplayElement, Boolean> sidebarWidgets;

    private boolean saveAsTemplate = false;
    private String templateName = "";
    private boolean templateLockComponents = false;

    @PostConstruct
    public void setup() {
        try {
            long pageId = Long
                    .parseLong(facesContext.getExternalContext().getRequestParameterMap().getOrDefault("selectedPageId", "-1"));
            long templateId =
                    Long.parseLong(facesContext.getExternalContext().getRequestParameterMap().getOrDefault("templateId", "-1"));
            String title =
                    facesContext.getExternalContext().getRequestParameterMap().getOrDefault("title", "");
            String relatedPi =
                    facesContext.getExternalContext().getRequestParameterMap().getOrDefault("relatedPi", "");

            if (pageId > 0) {
                CMSPage page = this.dao.getCMSPage(pageId);
                this.setSelectedPage(page);
                this.editMode = true;
            } else if (templateId > -1) {
                this.editMode = false;
                this.setNewSelectedPage(templateId);
            } else {
                this.editMode = false;
                this.setNewSelectedPage();
            }
            if (!this.editMode && StringUtils.isNotBlank(title)) {
                this.selectedPage.getTitleTranslations().setValue(title, IPolyglott.getDefaultLocale());
            }
            if (!this.editMode && StringUtils.isNotBlank(relatedPi)) {
                this.selectedPage.setRelatedPI(relatedPi);
            }
        } catch (NullPointerException | NumberFormatException e) {
            this.editMode = false;
            this.setNewSelectedPage();
        } catch (DAOException e) {
            logger.error("Error retrieving cms page template from dao: {}", e.toString());
            this.editMode = false;
            this.setNewSelectedPage();
        }
        try {
            setUserRestrictedValues(selectedPage, userBean.getUser());
        } catch (PresentationException | IndexUnreachableException | DAOException e1) {
            logger.error("Error setting user specific subtheme and categories", e1);
        }
        try {
            this.sidebarWidgets = widgetsBean.getAllWidgets().stream().collect(Collectors.toMap(Function.identity(), w -> Boolean.FALSE));
        } catch (DAOException e) {
            this.sidebarWidgets = Collections.emptyMap();
        }
    }

    public void savePageAndForwardToEdit() throws DAOException {
        this.saveSelectedPage();
        if (this.selectedPage.getId() != null) {
            String url = PrettyUrlTools.getAbsolutePageUrl("adminCmsEditPage", this.selectedPage.getId());
            try {
                facesContext.getExternalContext().redirect(url);
            } catch (IOException | NullPointerException e) {
                logger.error("Error redirecting to database url {}: {}", url, e.toString());
            }
        }
    }

    /**
     * Adds the current page to the database, if it doesn't exist or updates it otherwise
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void saveSelectedPage() throws DAOException {
        logger.trace("saveSelectedPage");
        if (userBean == null || !userBean.getUser().isCmsAdmin() || selectedPage == null) {
            // Only authorized CMS admins may save
            return;
        }

        setSidebarElementOrder(selectedPage);
        selectedPage.writeSelectableCategories();
        // Save
        boolean success = false;
        selectedPage.setDateUpdated(LocalDateTime.now());

        logger.trace("update dao");
        if (selectedPage.getId() != null) {
            success = this.dao.updateCMSPage(selectedPage);
        } else {
            success = this.dao.addCMSPage(selectedPage);
        }

        if (saveAsTemplate) {
            success = saveTemplate(selectedPage, templateName, templateLockComponents);
            if (success) {
                saveAsTemplate = false;
                this.templateLockComponents = false;
                this.templateName = "";
            }
        }

        if (success) {
            Messages.info("cms_pageSaveSuccess");
            logger.trace("reload cms page");
            logger.trace("update pages");
            cmsBean.getLazyModelPages().update();

            if (selectedPage.isSearchable()) {
                // Re-index related record text as part of the record
                if (StringUtils.isNotEmpty(selectedPage.getRelatedPI())) {
                    try {
                        IndexerTools.reIndexRecord(selectedPage.getRelatedPI());
                        Messages.info("admin_recordReExported");
                    } catch (RecordNotFoundException e) {
                        logger.error(e.getMessage());
                    }
                } else {
                    // Index CMS page metadata and texts as standalone docs
                    IndexerTools.triggerReIndexCMSPage(selectedPage, null);
                }
            }
        } else {
            Messages.error("cms_pageSaveFailure");
        }
        logger.trace("reset collections");
        this.collectionViewBean.removeCollectionsForPage(selectedPage);
        if (navigationBean != null) {
            logger.trace("add navigation item");
            navigationBean.getItemManager().addAvailableItem(new SelectableNavigationItem(this.selectedPage));
        }
        logger.trace("Done saving page");
    }

    private boolean saveTemplate(CMSPage page, String name, boolean lockComponents) throws DAOException {
        CMSPageTemplate template = new CMSPageTemplate(page);
        TranslatedText title = new TranslatedText(IPolyglott.getLocalesStatic());
        title.setText(name, IPolyglott.getDefaultLocale());
        template.setTitleTranslations(title);
        template.setLockComponents(lockComponents);
        template.setPublished(true);
        return this.dao.addCMSPageTemplate(template);
    }

    /**
     * Action method for deleting selectedPage from the database.
     *
     * @return Return view
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String deleteSelectedPage() throws DAOException {
        deletePage(selectedPage);
        return "cmsOverview";
    }

    /**
     * Deletes given CMS page from the database.
     *
     * @param page Page to delete
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void deletePage(CMSPage page) throws DAOException {
        if (this.dao != null && page != null && page.getId() != null) {
            logger.info("Deleting CMS page: {}", page);
            if (this.dao.deleteCMSPage(page)) {
                // Delete files matching content item IDs of the deleted page and re-index record
                try {
                    if (page.deleteExportedTextFiles() > 0) {
                        try {
                            IndexerTools.reIndexRecord(page.getRelatedPI());
                            logger.debug("Re-indexing record: {}", page.getRelatedPI());
                        } catch (RecordNotFoundException e) {
                            logger.error(e.getMessage());
                        }
                    }
                } catch (ViewerConfigurationException e) {
                    logger.error(e.getMessage());
                    Messages.error(e.getMessage());
                }
                cmsBean.getLazyModelPages().update();
                Messages.info("cms_deletePage_success");
            } else {
                Messages.error("cms_deletePage_failure");
            }
        }

        selectedPage = null;
    }

    /**
     * <p>
     * Setter for the field <code>selectedPage</code>.
     * </p>
     *
     * @param currentPage a {@link io.goobi.viewer.model.cms.pages.CMSPage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void setSelectedPage(CMSPage currentPage) throws DAOException {
        if (currentPage != null) {
            if (currentPage.getId() != null) {
                //get page from DAO
                this.selectedPage = new CMSPage(this.dao.getCMSPage(currentPage.getId()));
            } else {
                this.selectedPage = currentPage;
            }
            this.selectedPage.initialiseCMSComponents(templateManager);
            logger.debug("Selected page: {}", currentPage);
        } else {
            this.selectedPage = null;
        }

    }

    /**
     *
     * @return
     */
    public String getSelectedPageId() {
        if (selectedPage == null) {
            return null;
        }

        return String.valueOf(selectedPage.getId());
    }

    /**
     *
     * @param id
     * @throws DAOException
     */
    public void setSelectedPageId(String id) throws DAOException {
        logger.trace("setSelectedPageId: {}", id);
        CMSPage page = cmsBean.findPage(id);
        setSelectedPage(page);
    }

    public CMSPage getSelectedPage() {
        return selectedPage;
    }

    /**
     * Create a new CMSPage based on the given template. title and relatedPI are set on the page if given Opens the view to create/edit the cmsPage
     *
     * @param templateId The id of the template to base the page on
     * @param title The title to be used for the current locale, optional
     * @param relatedPI The PI of a related work, optional
     * @return a {@link java.lang.String} object.
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String createAndOpenNewPage(String title, String relatedPI) throws PresentationException, IndexUnreachableException, DAOException {

        String createPageUrl = PrettyUrlTools.getAbsolutePageUrl("adminCmsNewPage");
        URI uri = UriBuilder.fromUri(createPageUrl).queryParam("title", title).queryParam("relatedPi", relatedPI).build();
        return uri.toString();

        //        CMSPage page = new CMSPage();
        //        page.getTitleTranslations().setValue(title, IPolyglott.getDefaultLocale());
        //        page.setRelatedPI(relatedPI);
        //        setUserRestrictedValues(page, userBean.getUser());
        //        setSelectedPage(page);
        //        return "pretty:adminCmsNewPage";
    }

    private static void setSidebarElementOrder(CMSPage page) {
        for (int i = 0; i < page.getSidebarElements().size(); i++) {
            page.getSidebarElements().get(i).setOrder(i);
        }
    }

    public Map<WidgetDisplayElement, Boolean> getSidebarWidgets() {
        return sidebarWidgets;
    }

    public void setSidebarWidgets(Map<WidgetDisplayElement, Boolean> sidebarWidgets) {
        this.sidebarWidgets = sidebarWidgets;
    }

    public List<WidgetDisplayElement> getSelectedWidgets() {
        return this.sidebarWidgets.entrySet().stream().filter(Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    public void resetSelectedWidgets() {
        this.sidebarWidgets.entrySet().forEach(e -> e.setValue(false));
    }

    public List<WidgetDisplayElement> getAndResetSelectedWidgets() {
        List<WidgetDisplayElement> selected = getSelectedWidgets();
        resetSelectedWidgets();
        return selected;
    }

    public String getSelectedComponent() {
        return selectedComponent;
    }

    public void setSelectedComponent(String selectedComponent) {
        this.selectedComponent = selectedComponent;
    }

    public List<SelectItem> getAvailableComponents(CMSPage page) {
        Stream<CMSComponent> stream = templateManager.getContentManager().getComponents().stream();
        if (page != null && page.isContainsPagedComponents()) {
            stream = stream.filter(c -> !c.isPaged());
        }
        Locale locale = BeanUtils.getLocale();
        List<CMSComponent> components = stream
                .sorted((c1, c2) -> StringUtils.compare(ViewerResourceBundle.getTranslation(c1.getLabel(), locale),
                        ViewerResourceBundle.getTranslation(c2.getLabel(), locale)))
                .collect(Collectors.toList());
        Map<String, List<CMSComponent>> sortedMap = SelectItemBuilder.getAsAlphabeticallySortedMap(components,
                component -> ViewerResourceBundle.getTranslation(component.getLabel(), locale));
        return SelectItemBuilder.getAsGroupedSelectItems(sortedMap, CMSComponent::getTemplateFilename,
                c -> ViewerResourceBundle.getTranslation(c.getLabel(), locale), c -> ViewerResourceBundle.getTranslation(c.getDescription(), locale));
    }

    /**
     * Get the list of metadata fields which may be displayed. This is the main metadata list
     *
     * @return the main metadata list
     */
    public List<String> getAvailableMetadataFields() {
        return DataManager.getInstance()
                .getConfiguration()
                .getMainMetadataForTemplate(0, null)
                .stream()
                .map(Metadata::getLabel)
                .map(md -> md.replaceAll("_LANG_.*", ""))
                .distinct()
                .collect(Collectors.toList());

    }

    /**
     * <p>
     * mayRemoveCategoryFromPage.
     * </p>
     *
     * @return false only if the user has limited privileges for categories and only one category is set for the selected page
     * @param cat a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean mayRemoveCategoryFromPage(CMSCategory cat) throws DAOException {
        if (this.selectedPage != null) {
            return userBean.getUser().hasPrivilegeForAllCategories()
                    || this.selectedPage.getSelectableCategories().stream().anyMatch(Selectable::isSelected);
        }

        return true;
    }

    /**
     * <p>
     * isEditMode.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isEditMode() {
        return editMode;
    }

    /**
     * <p>
     * Setter for the field <code>editMode</code>.
     * </p>
     *
     * @param editMode a boolean.
     */
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public void setNewSelectedPage() {
        this.selectedPage = new CMSPage();
    }

    public void setNewSelectedPage(Long templateId) {
        CMSPageTemplate template = loadTemplate(templateId);
        if (template == null) {
            this.selectedPage = new CMSPage();
        } else {
            this.selectedPage = new CMSPage(template);
            this.selectedPage.initialiseCMSComponents(templateManager);
        }
    }

    private CMSPageTemplate loadTemplate(Long templateId) {
        if (templateId != null) {
            try {
                CMSPageTemplate template = this.dao.getCMSPageTemplate(templateId);
                if (template != null) {
                    template.initialiseCMSComponents(templateManager);
                }
                return template;
            } catch (DAOException e) {
                logger.error("Error loading cms page template with id {}: {}", templateId, e.toString());
            }
        }
        return null;
    }

    public CMSPageEditState getPageEditState() {
        return pageEditState;
    }

    public void setPageEditState(CMSPageEditState pageEditState) {
        this.pageEditState = pageEditState;
    }

    public boolean deleteComponent(CMSComponent component) {
        return this.selectedPage.removeComponent(component);
    }

    public void addComponent() {
        if (addComponent(getSelectedPage(), getSelectedComponent())) {
            setSelectedComponent(null);
        }
    }

    private boolean addComponent(CMSPage page, String componentFilename) {
        if (page != null) {
            if (StringUtils.isNotBlank(componentFilename)) {
                try {
                    page.addComponent(componentFilename, templateManager);
                    return true;
                } catch (IllegalArgumentException e) {
                    logger.error("Cannot add component: No component found for filename {}.", componentFilename);
                    Messages.error(null, "cms__create_page__error_unknown_component_name", componentFilename);
                }
            } else {
                logger.error("Cannot add component: No component filename given");
                Messages.error("cms__create_page__error_no_component_name_given");
            }
        } else {
            logger.error("Cannot add component: No page given");
        }
        return false;
    }

    /**
     * Fills all properties of the page with values for which the user has privileges - but only if the user has restricted privileges for that
     * property
     *
     * @param page
     * @param user
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    private void setUserRestrictedValues(CMSPage page, User user) throws PresentationException, IndexUnreachableException, DAOException {
        if (!user.hasPrivilegeForAllSubthemeDiscriminatorValues()) {
            List<String> allowedSubThemeDiscriminatorValues = user.getAllowedSubthemeDiscriminatorValues(cmsBean.getSubthemeDiscriminatorValues());
            if (StringUtils.isBlank(page.getSubThemeDiscriminatorValue()) && !allowedSubThemeDiscriminatorValues.isEmpty()) {
                page.setSubThemeDiscriminatorValue(allowedSubThemeDiscriminatorValues.get(0));
            } else {
                logger.error("User has no access to any subtheme discriminator values and can therefore not create a page");
                //do something??
            }
        }
        if (!user.hasPrivilegeForAllCategories()) {
            List<CMSCategory> allowedCategories = user.getAllowedCategories(cmsBean.getAllCategories());
            if (page.getCategories().isEmpty() && !allowedCategories.isEmpty()) {
                page.setCategories(allowedCategories.subList(0, 1));
            }
        }

    }

    public void setSaveAsTemplate(boolean saveAsTemplate) {
        this.saveAsTemplate = saveAsTemplate;
    }

    public boolean isSaveAsTemplate() {
        return saveAsTemplate;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateName() {
        if (StringUtils.isBlank(this.templateName)) {
            return getSelectedPage().getTitle(IPolyglott.getDefaultLocale());
        }
        return templateName;
    }

    public boolean isTemplateLockComponents() {
        return templateLockComponents;
    }

    public void setTemplateLockComponents(boolean templateLockComponents) {
        this.templateLockComponents = templateLockComponents;
    }

}
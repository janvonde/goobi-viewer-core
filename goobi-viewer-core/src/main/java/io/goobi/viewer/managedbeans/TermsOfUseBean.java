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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.misc.IPolyglott;
import io.goobi.viewer.model.misc.Translation;
import io.goobi.viewer.model.security.TermsOfUse;

/**
 * @author florian
 *
 */
@Named
@ViewScoped
public class TermsOfUseBean implements Serializable, IPolyglott {

    private static final long serialVersionUID = -3105025774455196485L;

    private static final Logger logger = LoggerFactory.getLogger(TermsOfUseBean.class);
    
    private TermsOfUse termsOfUse;
    private String selectedLanguage = BeanUtils.getLocale().getLanguage();
    
    @PostConstruct
    public void init() throws DAOException {
        TermsOfUse fromDatabase = DataManager.getInstance().getDao().getTermsOfUse();
        termsOfUse = new TermsOfUse(fromDatabase);
    }
    
    
    public String getTitle() {
        Translation translation = this.termsOfUse.getTitle(selectedLanguage);
        if(translation == null) {
            translation = this.termsOfUse.setTitle(selectedLanguage, "");
        }
        return translation.getValue();
    }
        
    public void setTitle(String value)  {
        this.termsOfUse.setTitle(selectedLanguage, value);
    }
    
    public String getDescription() {
        Translation translation = this.termsOfUse.getDescription(selectedLanguage);
        if(translation == null) {
            translation = this.termsOfUse.setDescription(selectedLanguage, "");
        }
        return translation.getValue();
    }
    
    public void setDescription(String value)  {
        this.termsOfUse.setDescription(selectedLanguage, value);
    }
    
    public void setActivated(boolean active) {
        this.termsOfUse.setActive(active);
        this.save();
    }
    
    public boolean isActivated() {
        return this.termsOfUse.isActive();
    }

    
    
    /**
     * @param selectedLanguage the selectedLanguage to set
     */
    public void setSelectedLanguage(String selectedLanguage) {
        this.selectedLanguage = selectedLanguage;
    }
    
    /**
     * @return the selectedLanguage
     */
    public String getSelectedLanguage() {
        return selectedLanguage;
    }
    
    public void save() {
        boolean saved = false;
        try {
            this.termsOfUse.cleanTranslations();
            saved = DataManager.getInstance().getDao().saveTermsOfUse(this.termsOfUse);
        } catch (DAOException e) {
            logger.error("Error saving terms of use ", e);
        }
        if(saved) {
            Messages.info("admin__terms_of_use__save__success");
        } else {
            Messages.error("admin__terms_of_use__save__error");
        }
    }
    
    public void resetUserAcceptance() throws DAOException {
        DataManager.getInstance().getDao().resetUserAgreementsToTermsOfUse();
        Messages.info("admin__terms_of_use__reset__success");
    }


    /* (non-Javadoc)
     * @see io.goobi.viewer.model.misc.IPolyglott#isComplete(java.lang.String)
     */
    @Override
    public boolean isComplete(String language) {
        return !termsOfUse.getTitle(language).isEmpty() && !termsOfUse.getDescription(language).isEmpty();
    }
    
}

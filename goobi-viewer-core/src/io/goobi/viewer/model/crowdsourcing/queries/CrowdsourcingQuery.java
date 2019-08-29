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
package io.goobi.viewer.model.crowdsourcing.queries;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.eclipse.persistence.annotations.PrivateOwned;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;

/**
 * @author florian
 *
 */
@JsonInclude(Include.NON_NULL)
@Entity
@Table(name = "cs_queries")
public class CrowdsourcingQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "query_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private Campaign owner;

    /** Translated metadata. */
    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    @PrivateOwned
    private List<QueryTranslation> translations = new ArrayList<>();

    private QueryType queryType;
    private TargetFrequency targetFrequency;
    private TargetSelector targetSelector;

    public CrowdsourcingQuery() {

    }

    public CrowdsourcingQuery(QueryType queryType, TargetFrequency targetFrequency, TargetSelector targetSelector) {
        this.queryType = queryType;
        this.targetFrequency = targetFrequency;
        this.targetSelector = targetSelector;
    }

    public String getLabel(String lang) {
        return getTranslation(lang, "label");
    }

    public void setLabel(String lang, String value) {
        setTranslation(lang, value, "label");
    }

    public String getDescription(String lang) {
        return getTranslation(lang, "description");
    }

    public void setDescription(String lang, String value) {
        setTranslation(lang, value, "description");
    }

    public String getHelp(String lang) {
        return getTranslation(lang, "help");
    }

    public void setHelp(String lang, String value) {
        setTranslation(lang, value, "help");
    }

    /**
     * 
     * @param tag
     * @param lang
     * @return
     */
    String getTranslation(String lang, String tag) {
        if (tag == null || lang == null) {
            return null;
        }

        for (QueryTranslation translation : translations) {
            if (translation.getTag().equals(tag) && translation.getLanguage().equals(lang)) {
                return translation.getValue();
            }
        }

        return "";
    }

    /**
     * 
     * @param lang
     * @param value
     * @param tag
     */
    void setTranslation(String lang, String value, String tag) {
        if (lang == null) {
            throw new IllegalArgumentException("lang may not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value may not be null");
        }

        for (QueryTranslation translation : translations) {
            if (translation.getTag().equals(tag) && translation.getLanguage().equals(lang)) {
                translation.setValue(value);
                return;
            }
        }
        translations.add(new QueryTranslation(lang, tag, value));
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the owner
     */
    public Campaign getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(Campaign owner) {
        this.owner = owner;
    }

    /**
     * @return the translations
     */
    public List<QueryTranslation> getTranslations() {
        return translations;
    }

    /**
     * @param translations the translations to set
     */
    public void setTranslations(List<QueryTranslation> translations) {
        this.translations = translations;
    }

    /**
     * @return the queryType
     */
    public QueryType getQueryType() {
        return queryType;
    }

    /**
     * @param queryType the queryType to set
     */
    public void setQueryType(QueryType queryType) {
        this.queryType = queryType;
    }

    /**
     * @return the targetType
     */
    public TargetFrequency getTargetFrequency() {
        return targetFrequency;
    }

    /**
     * @param targetType the targetType to set
     */
    public void setTargetFrequency(TargetFrequency targetFrequency) {
        this.targetFrequency = targetFrequency;
    }

    /**
     * @return the targetSelector
     */
    public TargetSelector getTargetSelector() {
        return targetSelector;
    }

    /**
     * @param targetSelector the targetSelector to set
     */
    public void setTargetSelector(TargetSelector targetSelector) {
        this.targetSelector = targetSelector;
    }
}

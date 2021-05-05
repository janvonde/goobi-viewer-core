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
package io.goobi.viewer.model.translations.admin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.translations.admin.MessageEntry.TranslationStatus;

/**
 * Translation group configuration item.
 */
public class TranslationGroup {

    public enum TranslationGroupType {
        SOLR_FIELD_NAMES,
        SOLR_FIELD_VALUES,
        CORE_STRINGS;

        /**
         * 
         * @param name
         * @return
         */
        public static TranslationGroupType getByName(String name) {
            if (name == null) {
                return null;
            }

            for (TranslationGroupType type : TranslationGroupType.values()) {
                if (type.name().equals(name)) {
                    return type;
                }
            }

            return null;
        }
    }

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(TranslationGroup.class);

    private final int id;
    private final TranslationGroupType type;
    private final String name;
    private final String description;
    private final List<TranslationGroupItem> items;

    private List<MessageEntry> allEntries;
    private MessageEntry selectedEntry;
    private int selectedEntryIndex = 0;
    private Integer untranslatedEntryCount;
    private Integer fullyTranslatedEntryCount = null;
    private String filterString;

    /**
     * Factory method.
     * 
     * @param id unique ID number
     * @param type
     * @param name
     * @param description
     * @param numItems
     * @return
     */
    public static TranslationGroup create(int id, TranslationGroupType type, String name, String description, int numItems) {
        return new TranslationGroup(id, type, name, description, numItems);
    }

    /**
     * Private constructor.
     * 
     * @param id
     * @param type
     * @param name
     * @param description
     * @param numItems
     */
    private TranslationGroup(int id, TranslationGroupType type, String name, String description, int numItems) {
        if (numItems < 0) {
            throw new IllegalArgumentException("numKeys may not be negative");
        }
        this.id = id;
        this.type = type;
        this.name = name;
        this.description = description;
        this.items = new ArrayList<>(numItems);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TranslationGroup other = (TranslationGroup) obj;
        if (id != other.id)
            return false;
        return true;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @return the type
     */
    public TranslationGroupType getType() {
        return type;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the keys
     */
    public List<TranslationGroupItem> getItems() {
        return items;
    }

    /**
     * 
     * @return Number of unique message keys across all groups
     * @should return correct count
     */
    public int getEntryCount() {
        Set<MessageEntry> returnSet = new HashSet<>();
        for (TranslationGroupItem item : items) {
            returnSet.addAll(item.getEntries());
        }

        return returnSet.size();
    }

    /**
     * 
     * @return Number of entries with no translations at all
     */
    public Integer getUntranslatedEntryCount() {
        if (untranslatedEntryCount == null) {
            untranslatedEntryCount = getEntryStatusCount(TranslationStatus.NONE);
        }

        return untranslatedEntryCount;
    }

    /**
     * Returns the number of entries which finished (non-zzz) translations for at least one but less than all languages.
     * 
     * @return Number of partially translated entries
     */
    public Integer getPartiallyTranslatedEntryCount() {
        return getEntryCount() - getFullyTranslatedEntryCount() - getUntranslatedEntryCount();
    }

    /**
     * Returns the number of entries which finished (non-zzz) translations for all languages.
     * 
     * @return Number of fully translated entries
     */
    public Integer getFullyTranslatedEntryCount() {
        if (fullyTranslatedEntryCount == null) {
            fullyTranslatedEntryCount = getEntryStatusCount(TranslationStatus.FULL);
        }

        return fullyTranslatedEntryCount;
    }

    /**
     * 
     * @param status
     * @return Count of entries with the given <code>status</code>
     */
    int getEntryStatusCount(TranslationStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status may not be null");
        }

        int count = 0;
        for (MessageEntry key : getAllEntries()) {
            if (key.getTranslationStatus().equals(status)) {
                count++;
            }
        }

        return count;
    }

    /**
     * 
     * @return Percentage represented by untranslated entries
     */
    public double getUntranslatedEntryCountPercentage() {
        return getUntranslatedEntryCount() * 100.0 / getEntryCount();
    }

    /**
     * 
     * @return Percentage represented by partially translated entries
     */
    public double getPartiallyTranslatedEntryCountPercentage() {
        return getPartiallyTranslatedEntryCount() * 100.0 / getEntryCount();
    }

    /**
     * 
     * @return Percentage represented by fully translated entries
     */
    public double getFullyTranslatedEntryCountPercentage() {
        return getFullyTranslatedEntryCount() * 100.0 / getEntryCount();
    }

    /**
     * 
     * @return true if all entries are fully translated; false otherwise
     */
    public boolean isFullyTranslated() {
        return getFullyTranslatedEntryCount() == getEntryCount();
    }

    /**
     * 
     * @return true if all entries in the filtered list are fully translated; false otherwise
     */
    public boolean isAllFilteredEntriesFullyTranslated() {
        for (MessageEntry entry : getFilteredEntries()) {
            if (!TranslationStatus.FULL.equals(entry.getTranslationStatus())) {
                return false;
            }
        }

        return true;
    }

    /**
     * 
     * @return
     * @should filter by key correctly
     * @should filter by value correctly
     */
    public List<MessageEntry> getFilteredEntries() {
        if (StringUtils.isBlank(filterString)) {
            return getAllEntries();
        }

        List<MessageEntry> ret = new ArrayList<>(getAllEntries().size());
        String filterLowercase = filterString.toLowerCase();
        for (MessageEntry entry : getAllEntries()) {
            if (entry.getKey().toLowerCase().contains(filterLowercase)) {
                ret.add(entry);
            } else {
                for (MessageValue value : entry.getValues()) {
                    if (value.getValue() != null && value.getValue().toLowerCase().contains(filterLowercase)) {
                        ret.add(entry);
                        break;
                    }
                }
            }
        }

        return ret;
    }

    /**
     * 
     * @return Unique message keys across all groups
     */
    public List<MessageEntry> getAllEntries() {
        if (allEntries == null) {
            logger.trace("Loading entries...");
            Set<MessageEntry> retSet = new HashSet<>();
            for (TranslationGroupItem item : items) {
                retSet.addAll(item.getEntries());
            }

            allEntries = new ArrayList<>(retSet);
            Collections.sort(allEntries);
        }

        return allEntries;
    }

    /**
     * @return the selectedEntry
     */
    public MessageEntry getSelectedEntry() {
        if (selectedEntry == null && getFilteredEntries().size() > selectedEntryIndex) {
            setSelectedEntry(allEntries.get(selectedEntryIndex));
        }

        return selectedEntry;
    }

    /**
     * @param selectedEntry the selectedEntry to set
     */
    public void setSelectedEntry(MessageEntry selectedEntry) {
        saveSelectedEntry();
        this.selectedEntry = selectedEntry;
        this.selectedEntryIndex = getFilteredEntries().indexOf(selectedEntry);
        if (selectedEntryIndex == -1) {
            selectedEntryIndex = 0;
        }
    }

    /**
     * @return the selectedMessageEntryIndex
     */
    public int getSelectedEntryIndex() {
        return selectedEntryIndex;
    }

    /**
     * @param selectedEntryIndex the selectedEntryIndex to set
     */
    public void setSelectedEntryIndex(int selectedEntryIndex) {
        this.selectedEntryIndex = selectedEntryIndex;
    }

    /**
     * @return the filterString
     */
    public String getFilterString() {
        return filterString;
    }

    /**
     * @param filterString the filterString to set
     */
    public void setFilterString(String filterString) {
        logger.trace("setFilterString: {}", filterString);
        this.filterString = filterString;
    }

    /**
     * Sets the previous {@link MessageEntry} element in the list active.
     */
    public void prevEntry() {
        selectEntry(-1);
    }

    /**
     * Sets the next {@link MessageEntry} element in the list active.
     */
    public void nextEntry() {
        selectEntry(1);
    }

    /**
     * 
     * @param step Positive or negative increment
     * @should only select unfinished entries
     * @should select fully translated entries if all are fully translated
     * @should resume at the end when moving past first element
     * @should resume at the beginning when moving past last element
     * 
     */
    void selectEntry(int step) {
        logger.trace("selectEntry: {}", step);
        if (getFilteredEntries() == null || getFilteredEntries().isEmpty()) {
            return;
        }

        if (selectedEntryIndex >= getFilteredEntries().size()) {
            selectedEntryIndex = 0;
        }
        int startIndex = selectedEntryIndex;
        MessageEntry entry = selectedEntry;
        Set<Integer> selected = new HashSet<>(getFilteredEntries().size());
        // Skip currently selected entry or any entries that are already fully translated (unless all are fully translated)
        while (entry == null || entry.equals(selectedEntry)
                || (entry.getTranslationStatus().equals(TranslationStatus.FULL) && !isAllFilteredEntriesFullyTranslated())) {
            selectedEntryIndex += step;
            logger.trace("selectedEntryIndex: {}", selectedEntryIndex);
            // After a full circle (no unfinished entries left), reset to the current entry
            if (selectedEntryIndex == startIndex) {
                logger.trace("circle");
                entry = selectedEntry;
                break;
            }
            // Resume from the end if moving past the first entry
            if (step < 0 && selectedEntryIndex == -1) {
                selectedEntryIndex = getFilteredEntries().size() - 1;
                logger.trace("left reset: {}", selectedEntryIndex);
            }
            // Resume from the beginning if moving past the last entry
            if (step > 0 && selectedEntryIndex == getFilteredEntries().size()) {
                selectedEntryIndex = 0;
                logger.trace("right reset: {}", selectedEntryIndex);
            }
            entry = getFilteredEntries().get(selectedEntryIndex);
            selected.add(selectedEntryIndex);
            logger.trace("entry selected: {}", selectedEntryIndex);
            // If all available entries have already been selected, then all entries are probably fully translated
            if (selected.size() == getFilteredEntries().size()) {
                logger.trace("fully: {}", isFullyTranslated());
                logger.trace("All entries checked, aborting");
                break;
            }
        }
        setSelectedEntry(entry);
    }

    /**
     * Persists the edited values of <code>selectedEntry</code> to all language messages.properties files.
     */
    public synchronized void saveSelectedEntry() {
        if (selectedEntry == null) {
            return;
        }

        Map<String, PropertiesConfiguration> configMap = new HashMap<>();
        for (MessageValue value : selectedEntry.getValues()) {
            if (!value.isDirty()) {
                continue;
            }

            PropertiesConfiguration config = configMap.get(value.getLanguage());

            // Load config
            if (config == null) {
                File file = getLocalTranslationFile(value.getLanguage());
                if (!file.exists()) {
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                }

                config = new PropertiesConfiguration();
                try {
                    config.load(file);
                    configMap.put(value.getLanguage(), config);
                } catch (ConfigurationException e) {
                    logger.error(e.getMessage(), e);
                }
            }

            if (StringUtils.isNotEmpty(value.getValue())) {
                config.setProperty(selectedEntry.getKey(), value.getValue());
                logger.trace("value set ({}): {}:{}->{}", config.getFile().getName(), selectedEntry.getKey(), value.getLoadedValue(),
                        config.getProperty(selectedEntry.getKey()));
            }
            value.resetDirtyStatus();
        }

        for (String key : configMap.keySet()) {
            try {
                configMap.get(key)
                        .setFile(getLocalTranslationFile(key));
                configMap.get(key).save();
                logger.trace("File written: {}", configMap.get(key).getFile().getAbsolutePath());
            } catch (ConfigurationException e) {
                logger.error(e.getMessage());
            }
        }
    }

    private static File getLocalTranslationFile(String language) {
        return new File(DataManager.getInstance().getConfiguration().getConfigLocalPath(),
                "messages_" + language + ".properties");
    }
    
    /**
     * Check whether the application has write access to all local messages files 
     * as well as the containing folder if any languages have no local message file.
     * The tested languages are taken from {@link ViewerResourceBundle#getAllLocales()}
     * 
     * @return true if all required access rights to edit messages are present. false otherwise
     */
    public static boolean isHasFileAccess() {
        /* Using Files#isWritable(Path path) because it's supposedly more reliable for Unix systems with network folders 
         * Reports vary, though...
         */
        for (Locale locale : ViewerResourceBundle.getAllLocales()) {
            Path path = getLocalTranslationFile(locale.getLanguage()).toPath();
            if( Files.exists(path) ) {
                if( Files.isWritable(path) ) {
                    continue;
                } else {
                    return false;
                }
            } else if( Files.isWritable(path.getParent()) ){
                continue;
            } else {
                return false;
            }
        } 
        return true;
    }
    
}

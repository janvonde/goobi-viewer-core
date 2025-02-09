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
package io.goobi.viewer.model.citation;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLType;
import de.undercouch.citeproc.output.Bibliography;

public class Citation {

    private static final Logger logger = LogManager.getLogger(Citation.class);

    private String id;
    private final CSLType type;
    private final Map<String, List<String>> fields;
    private final CSL processor;
    private final CitationDataProvider itemDataProvider;

    /**
     * Constructor.
     *
     * @param id
     * @param processor
     * @param itemDataProvider
     * @param type
     * @param fields Map containing metadata fields
     */
    public Citation(String id, CSL processor, CitationDataProvider itemDataProvider, CSLType type, Map<String, List<String>> fields) {
        if (id == null) {
            throw new IllegalArgumentException("id may not be null");
        }
        if (processor == null) {
            throw new IllegalArgumentException("processor may not be null");
        }
        if (itemDataProvider == null) {
            throw new IllegalArgumentException("itemDataProvider may not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type may not be null");
        }
        if (fields == null) {
            throw new IllegalArgumentException("fields may not be null");
        }

        this.id = id;
        this.processor = processor;
        this.itemDataProvider = itemDataProvider;
        this.type = type;
        this.fields = fields;
    }

    /**
     *
     * @param outputFormat
     * @param items
     * @return
     * @throws IOException
     */
    Bibliography makeAdhocBibliography(String outputFormat, CSLItemData... items) throws IOException {
        // logger.trace("makeAdhocBibliography");
        synchronized (processor) {
            processor.reset();
            processor.setOutputFormat(outputFormat);
            String[] ids = new String[items.length];
            for (int i = 0; i < items.length; ++i) {
                ids[i] = items[i].getId();
                // logger.trace("Item data id: {}", items[i].getId());
            }
            processor.registerCitationItems(ids);

            return processor.makeBibliography();
        }
    }

    /**
     * @param outputFormat
     * @return Citation string
     * @throws IOException
     * @should return apa html citation correctly
     * @should return apa html plaintext correctly
     */
    public String getCitationString(String outputFormat) throws IOException {
        CSLItemData itemData = itemDataProvider.addItemData(id, fields, type);
        try {
            return makeAdhocBibliography(outputFormat, itemData).makeString().trim();
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return "";
        }
    }
}

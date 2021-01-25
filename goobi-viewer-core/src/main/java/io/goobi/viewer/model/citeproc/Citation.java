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
package io.goobi.viewer.model.citeproc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.csl.CSLDateBuilder;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLType;

public class Citation {

    public static final String AUTHOR = "author";
    public static final String DOI = "DOI";
    public static final String ISBN = "ISBN";
    public static final String ISSN = "ISSN";
    public static final String ISSUED = "issued";
    public static final String LANGUAGE = "language";
    public static final String PUBLISHER_PLACE = "placepublish";
    public static final String PUBLISHER = "publisher";
    public static final String TITLE = "title";

    private final String style;
    private CSLItemData item;
    private Map<String, String> fields = new HashMap<>();

    /**
     * Constructor.
     * 
     * @param style
     * @param fields Map containing metadata fields
     */
    public Citation(String style, Map<String, String> fields) {
        if (style == null) {
            throw new IllegalArgumentException("style may not be null");
        }
        if (fields == null) {
            throw new IllegalArgumentException("fields may not be null");
        }

        this.style = style;
        this.fields = fields;
    }

    /**
     * Builds item data from the metadata fields.
     * 
     * @return this object
     */
    public Citation build() {
        CSLItemDataBuilder builder = new CSLItemDataBuilder().type(CSLType.WEBPAGE);

        for (String key : fields.keySet()) {
            switch (key) {
                case AUTHOR:
                    String name = fields.get(key);
                    if (name.contains(",")) {
                        String[] nameSplit = name.split(",");
                        if (nameSplit.length > 1) {
                            builder.author(nameSplit[1].trim(), nameSplit[0].trim());
                        } else {
                            builder.author("", nameSplit[0].trim());
                        }
                    } else {
                        builder.author("", name);
                    }
                    break;
                case DOI:
                    builder.DOI(fields.get(key));
                    break;
                case ISBN:
                    builder.ISBN(fields.get(key));
                    break;
                case ISSN:
                    builder.ISSN(fields.get(key));
                    break;
                case ISSUED:
                    builder.issued(new CSLDateBuilder().raw(fields.get(key)).build());
                    break;
                case LANGUAGE:
                    builder.language(fields.get(key));
                    break;
                case PUBLISHER:
                    builder.publisher(fields.get(key));
                    break;
                case PUBLISHER_PLACE:
                    builder.publisherPlace(fields.get(key));
                    break;
                case TITLE:
                    builder.title(fields.get(key));
                    break;
            }
        }

        this.item = builder.build();

        return this;
    }

    public String getCitationString() throws IOException {
        if (item == null) {
            throw new IllegalStateException("Item data not yet built");
        }

        return CSL.makeAdhocBibliography(style, item).makeString();
    }
}

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
package io.goobi.viewer.model.archives;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author florian
 *
 */
public class ArchiveResource implements Serializable {

    private static final long serialVersionUID = -234029216818092944L;

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    private final String databaseName;
    private final String resourceName;
    private final LocalDateTime modifiedDate;
    private final Long size;

    public ArchiveResource(String databaseName, String resourceName, String modifiedDate, String size) {
        this.databaseName = databaseName;
        this.resourceName = resourceName;
        this.modifiedDate = LocalDateTime.parse(modifiedDate, DATE_TIME_FORMATTER);
        this.size = Long.parseLong(size);
    }

    /**
     * @return the databaseName
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * @return the resourceName
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * @return the modifiedDate
     */
    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    /**
     * @return the size
     */
    public Long getSize() {
        return size;
    }

    /**
     * @return
     */
    public String getCombinedName() {
        return databaseName + " - " + resourceName.replaceAll("(?i)\\.xml", "");
    }

    public String getCombinedId() {
        return getDatabaseId() + " - " + getResourceId();
    }

    public String getDatabaseId() {
        return databaseName;
        //        try {
        //            return URLEncoder.encode(databaseName, "utf-8");
        //        } catch (UnsupportedEncodingException e) {
        //            throw new IllegalStateException("'utf-8' is unsupported encoding");
        //        }
    }

    public String getResourceId() {
        //        try {
        String id = resourceName.replaceAll("(?i)\\.xml", "");
        return id;
        //        } catch (UnsupportedEncodingException e) {
        //            throw new IllegalStateException("'utf-8' is unsupported encoding");
        //        }
    }

    @Override
    public String toString() {
        return getCombinedName();
    }

}

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
package io.goobi.viewer.api.rest.resourcebuilders;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.toc.TOC;
import io.goobi.viewer.model.toc.export.pdf.TocWriter;
import io.goobi.viewer.model.viewer.ViewManager;

/**
 * @author florian
 *
 */
public class TocResourceBuilder {

    HttpServletRequest request;
    HttpServletResponse response;

    public TocResourceBuilder(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public String getToc(String pi)
            throws ContentNotFoundException, PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        try {
            if (!AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, null, IPrivilegeHolder.PRIV_DOWNLOAD_METADATA, request)) {
                throw new ContentNotFoundException("Resource not found");
            }
        } catch (RecordNotFoundException e1) {
            throw new ContentNotFoundException("Resource not found");
        }

        ViewManager viewManager;
        try {
            viewManager = ViewManager.createViewManager(pi);
        } catch (RecordNotFoundException e) {
            throw new ContentNotFoundException("Resource not found: " + pi);
        }
        TOC toc = new TOC();
        toc.generate(viewManager.getTopStructElement(), viewManager.isListAllVolumesInTOC(), viewManager.getMainMimeType(), 1);
        TocWriter writer = new TocWriter("", viewManager.getTopStructElement().getLabel().toUpperCase());
        writer.setLevelIndent(5);

        return writer.getAsText(toc.getTocElements());
    }

}

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
package io.goobi.viewer.servlets.rest.search;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.search.SearchFacets;
import io.goobi.viewer.model.search.SearchHit;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.servlets.rest.security.AuthenticationBinding;

/**
 * <p>
 * SearchHitsNotificationResource class.
 * </p>
 */
@Path(SearchHitsNotificationResource.RESOURCE_PATH)
@ViewerRestServiceBinding
public class SearchHitsNotificationResource {

    private static final Logger logger = LoggerFactory.getLogger(SearchHitsNotificationResource.class);

    /** Constant <code>RESOURCE_PATH="/search"</code> */
    public static final String RESOURCE_PATH = "/search";

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * <p>
     * sendNewHitsNotifications.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    @GET
    @Path("/sendnotifications/")
    @Produces({ MediaType.TEXT_HTML })
    @AuthenticationBinding
    public String sendNewHitsNotifications() throws DAOException, PresentationException, IndexUnreachableException, ViewerConfigurationException {
        logger.trace("sendNewHitsNotifications");
        Map<String, String> filters = new HashMap<>();
        filters.put("newHitsNotification", "1");
        long searchCount = DataManager.getInstance().getDao().getSearchCount(null, filters);
        logger.info("Found {} saved searches with notifications enabled.", searchCount);
        int pageSize = 100;

        StringBuilder sbDebug = new StringBuilder();
        for (int i = 0; i < searchCount; i += pageSize) {
            logger.debug("Getting searches {}-{}", i, i + pageSize);
            List<Search> searches = DataManager.getInstance().getDao().getSearches(null, i, pageSize, null, false, filters);
            for (Search search : searches) {
                // TODO access condition filters for each user
                List<SearchHit> newHits = getNewHits(search);
                if(newHits.size() > 0) {   
                    String email = search.getOwner().getEmail();
                    sendEmailNotification(newHits, search.getName(), email);
                    DataManager.getInstance().getDao().updateSearch(search);
                }
            }
        }

        return sbDebug.toString();
    }

    /**
     * @param newHits
     * @param owner
     */
    private void sendEmailNotification(List<SearchHit> newHits, String searchName, String address) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table>");
        int count = 0;
        for (SearchHit newHit : newHits) {
            logger.trace("New hit: {}", newHit.getBrowseElement().getPi());
            count++;
            sb.append(newHit.generateNotificationFragment(count));
        }
        sb.append("</table>");

        // TODO Send notification
        String subject = ViewerResourceBundle.getTranslation("user_mySearches_notificationMailSubject", null);
        subject = subject.replace("{0}", searchName);
        String body = ViewerResourceBundle.getTranslation("user_mySearches_notificationMailBody", null);
        body = body.replace("{0}", searchName);
        body = body.replace("{1}", sb.toString());
        body = body.replace("{2}", "Goobi viewer");

        if (StringUtils.isNotEmpty(address)) {
            try {
                NetTools.postMail(Collections.singletonList(address), subject, body);
            } catch (UnsupportedEncodingException | MessagingException e) {
                logger.error(e.getMessage(), e);
            }
        }
        
    }

    /**
     * Executes the given search. If after {@link Search#execute(SearchFacets, Map, int, int, java.util.Locale, boolean, boolean) execution} 
     * the {@link Search#getHitsCount()} is larger than {@link Search#getLastHitsCount()}
     * the newest (hitsCount - lastHitsCount) hits are returned and the lastHitsCount of the search is updated
     * 
     * 
     * @param search
     * @return  A list of new hits (based on {@link Search#getLastHitsCount()}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    public List<SearchHit> getNewHits(Search search)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        //clone the search so any alterations are discarded later
        Search tempSearch = new Search(search);
        SearchFacets facets = new SearchFacets();
        facets.setCurrentFacetString(tempSearch.getFacetString());
        tempSearch.execute(facets, null, 0, 0, null, DataManager.getInstance().getConfiguration().isAggregateHits());
        // TODO what if there're >100 new hits?
        if (tempSearch.getHitsCount() > tempSearch.getLastHitsCount()) {
            int newHitsCount = (int) (tempSearch.getHitsCount() - tempSearch.getLastHitsCount());
            newHitsCount = Math.min(100, newHitsCount);  //don't query more than 100 hits
            //sort so newest hits come first
            tempSearch.setSortString('!' + SolrConstants.DATECREATED);
            //after last execution, page is 0, set back to 1 to actually get some results
            tempSearch.setPage(1);
            //reset hits count to 0 to actually perform search
            tempSearch.setHitsCount(0);
            tempSearch.execute(facets, null, newHitsCount, 0, null, DataManager.getInstance().getConfiguration().isAggregateHits());
            List<SearchHit> newHits = tempSearch.getHits();

            // Update last count
            search.setLastHitsCount(tempSearch.getHitsCount());
            return newHits;
        }
        return new ArrayList<>();
    }
}
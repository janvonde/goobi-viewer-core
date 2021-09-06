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

import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.solr.common.SolrDocument;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.IAnnotationCollection;
import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.annotation.wa.WebAnnotation;
import de.intranda.api.annotation.wa.collection.AnnotationCollection;
import de.intranda.api.annotation.wa.collection.AnnotationCollectionBuilder;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.v2.AnnotationList;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.Comment;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.iiif.presentation.v2.builder.OpenAnnotationBuilder;
import io.goobi.viewer.model.iiif.presentation.v2.builder.WebAnnotationBuilder;

/**
 * @author florian
 *
 */
public class AnnotationsResourceBuilder {

    private final AbstractApiUrlManager urls;
    private final HttpServletRequest request;
    private final WebAnnotationBuilder waBuilder;
    private final OpenAnnotationBuilder oaBuilder;
    private final AnnotationConverter converter;


    private final static Logger logger = LoggerFactory.getLogger(AnnotationsResourceBuilder.class);

    private final static int MAX_ANNOTATIONS_PER_PAGE = 100;

    /**
     * Default constructor
     * 
     * @param urls TheApiUrlManager handling the creation of annotation urls/ids
     * @param request Used to check access to restricted annotations. May be null, which prevents delivering any annotations with accessconditions
     *            other than OPENACCESS
     */
    public AnnotationsResourceBuilder(AbstractApiUrlManager urls, HttpServletRequest request) {
        if (urls == null) {
            throw new IllegalArgumentException("ApiUrlManager must not be null. Configure a rest api with a current ('/api/v1') endpoint");
        }
        this.urls = urls;
        this.request = request;
        this.waBuilder = new WebAnnotationBuilder(urls);
        this.oaBuilder = new OpenAnnotationBuilder(urls);
        converter = new AnnotationConverter(urls);
    }

    public AnnotationCollection getWebAnnotationCollection() throws PresentationException, IndexUnreachableException {
        long count = waBuilder.getAnnotationCount(request);
        URI uri = URI.create(urls.path(ANNOTATIONS).build());
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, count);
        AnnotationCollection collection = builder.setItemsPerPage(MAX_ANNOTATIONS_PER_PAGE).buildCollection();
        return collection;
    }

    public AnnotationPage getWebAnnotationPage(Integer page) throws IllegalRequestException, PresentationException, IndexUnreachableException {
        if (page == null || page < 1) {
            throw new IllegalRequestException("Page number must be at least 1");
        }
        int first = (page - 1) * MAX_ANNOTATIONS_PER_PAGE;
        String sortField = "id";
        
        DataManager.getInstance().getDao().getAnnotations(first, MAX_ANNOTATIONS_PER_PAGE, sortField, false, null)
        
        List<SolrDocument> data = waBuilder.getAnnotationDocuments(waBuilder.getAnnotationQuery(), first, MAX_ANNOTATIONS_PER_PAGE, null, request);
        //        List<PersistentAnnotation> data = DataManager.getInstance().getDao().getAnnotations(first, MAX_ANNOTATIONS_PER_PAGE, sortField, true, null);
        if (data.isEmpty()) {
            throw new IllegalRequestException("Page number is out of bounds");
        }
        URI uri = URI.create(urls.path(ANNOTATIONS).build());
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        List<IAnnotation> annos = data.stream().map(doc -> waBuilder.createUGCWebAnnotation(doc, false)).collect(Collectors.toList());
        AnnotationPage annoPage = builder.setItemsPerPage(MAX_ANNOTATIONS_PER_PAGE).buildPage(annos, page);
        return annoPage;
    }

    /**
     * @param pi
     * @param uri
     * @return
     * @throws DAOException
     */
    public AnnotationCollection getWebAnnotationCollectionForRecord(String pi, URI uri) throws DAOException {
        long count = DataManager.getInstance().getDao().getAnnotationCountForWork(pi);
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, count);
        AnnotationCollection collection = builder.setItemsPerPage((int) count).buildCollection();
        if (count > 0) {
            try {
                collection.setFirst(getWebAnnotationPageForRecord(pi, uri, 1));
            } catch (IllegalRequestException e) {
                //no items
            }
        }
        return collection;
    }

    public AnnotationCollection getWebAnnotationCollectionForPage(String pi, Integer pageNo, URI uri) throws DAOException {
        long count = DataManager.getInstance().getDao().getAnnotationCountForTarget(pi, pageNo);
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, count);
        AnnotationCollection collection = builder.setItemsPerPage((int) count).buildCollection();
        if (count > 0) {
            try {
                collection.setFirst(getWebAnnotationPageForPage(pi, pageNo, uri, 1));
            } catch (IllegalRequestException e) {
                //no items
            }
        }
        return collection;
    }

    /**
     * @param pi
     * @param uri
     * @return
     * @throws DAOException
     */
    public AnnotationList getOAnnotationListForRecord(String pi, URI uri) throws DAOException {
        List<PersistentAnnotation> data = DataManager.getInstance().getDao().getAnnotationsForWork(pi);
        AnnotationList list = new AnnotationList(uri);
        data.stream().map(converter::getAsOpenAnnotation).forEach(oa -> list.addResource(oa));
        return list;
    }

    /**
     * @param pi
     * @param pageNo
     * @param uri
     * @return
     * @throws DAOException
     */
    public IAnnotationCollection getOAnnotationListForPage(String pi, Integer pageNo, URI uri) throws DAOException {
        List<PersistentAnnotation> data = DataManager.getInstance().getDao().getAnnotationsForTarget(pi, pageNo);
        AnnotationList list = new AnnotationList(uri);
        data.stream().map(converter::getAsOpenAnnotation).forEach(oa -> list.addResource(oa));
        return list;
    }

    /**
     * @param uri
     * @param page
     * @return
     * @throws DAOException
     * @throws IllegalRequestException
     */
    public AnnotationPage getWebAnnotationPageForRecord(String pi, URI uri, Integer page) throws DAOException, IllegalRequestException {
        if (page == null || page < 1) {
            throw new IllegalRequestException("Page number must be at least 1");
        }
        List<PersistentAnnotation> data = DataManager.getInstance().getDao().getAnnotationsForWork(pi);
        if (data.isEmpty()) {
            throw new IllegalRequestException("Page number is out of bounds");
        }
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        List<IAnnotation> annos = data.stream().map(converter::getAsWebAnnotation).collect(Collectors.toList());
        AnnotationPage annoPage = builder.setItemsPerPage(annos.size()).buildPage(annos, page);
        return annoPage;
    }

    public AnnotationPage getWebAnnotationPageForPage(String pi, Integer pageNo, URI uri, Integer page) throws DAOException, IllegalRequestException {
        if (page == null || page < 1) {
            throw new IllegalRequestException("Page number must be at least 1");
        }
        List<PersistentAnnotation> data = DataManager.getInstance().getDao().getAnnotationsForTarget(pi, pageNo);
        if (data.isEmpty()) {
            throw new IllegalRequestException("Page number is out of bounds");
        }
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        List<IAnnotation> annos = data.stream().map(converter::getAsWebAnnotation).collect(Collectors.toList());
        AnnotationPage annoPage = builder.setItemsPerPage(annos.size()).buildPage(annos, page);
        return annoPage;
    }

    /**
     * @param format
     * @param uri
     * @return
     * @throws DAOException
     */
    public AnnotationCollection getWebAnnotationCollectionForRecordComments(String pi, URI uri) throws DAOException {
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForWork(pi);

        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        AnnotationCollection collection = builder.setItemsPerPage(data.size()).buildCollection();
        if (data.size() > 0) {
            try {
                collection.setFirst(getWebAnnotationPageForRecordComments(pi, uri, 1));
            } catch (IllegalRequestException e) {
                //no items
            }
        }
        return collection;
    }

    public AnnotationPage getWebAnnotationPageForRecordComments(String pi, URI uri, Integer page) throws DAOException, IllegalRequestException {
        if (page == null || page < 1) {
            throw new IllegalRequestException("Page number must be at least 1");
        }
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForWork(pi);
        if (data.isEmpty()) {
            throw new IllegalRequestException("Page number is out of bounds");
        }
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        AnnotationPage annoPage = builder.setItemsPerPage(data.size())
                .buildPage(
                        data.stream()
                                .map(converter::getAsWebAnnotation)
                                .collect(Collectors.toList()),
                        page);

        return annoPage;
    }

    public AnnotationList getOAnnotationListForRecordComments(String pi, URI uri) throws DAOException {
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForWork(pi);

        AnnotationList list = new AnnotationList(uri);
        data.stream().map(converter::getAsOpenAnnotation).forEach(oa -> list.addResource(oa));
        return list;
    }

    public AnnotationList getOAnnotationListForPageComments(String pi, Integer pageNo, URI uri) throws DAOException {
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForPage(pi, pageNo);

        AnnotationList list = new AnnotationList(uri);
        data.stream().map(converter::getAsOpenAnnotation).forEach(oa -> list.addResource(oa));
        return list;
    }

    public AnnotationPage getWebAnnotationPageForPageComments(String pi, Integer pageNo, URI uri)
            throws DAOException, IllegalRequestException {
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForPage(pi, pageNo);
        AnnotationPage page = new AnnotationPage(uri);
        data.stream()
        .map(converter::getAsOpenAnnotation)
        .collect(Collectors.toList())
        .forEach(c -> page.addItem(c));

        return page;
    }


    /**
     * @param id
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public Optional<WebAnnotation> getWebAnnotation(long id) throws PresentationException, IndexUnreachableException {
        return waBuilder.getAnnotationDocument(id, request).map(doc -> waBuilder.createUGCWebAnnotation(doc, false));
    }

    /**
     * @param id
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public Optional<OpenAnnotation> getOpenAnnotation(Long id) throws PresentationException, IndexUnreachableException {
        return oaBuilder.getAnnotationDocument(id, request).map(doc -> oaBuilder.createUGCOpenAnnotation(doc, false));
    }

}

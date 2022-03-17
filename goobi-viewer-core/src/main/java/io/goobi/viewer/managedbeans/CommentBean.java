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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.util.Faces;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.AjaxResponseException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.PublicationStatus;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.annotation.comments.CommentGroup;
import io.goobi.viewer.model.annotation.comments.CommentManager;
import io.goobi.viewer.model.annotation.notification.CommentMailNotificator;
import io.goobi.viewer.model.annotation.notification.JsfMessagesNotificator;
import io.goobi.viewer.model.annotation.serialization.SolrAndSqlAnnotationSaver;
import io.goobi.viewer.model.annotation.serialization.SqlAnnotationDeleter;
import io.goobi.viewer.model.annotation.serialization.SqlCommentLister;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.solr.SolrConstants;

/**
 * @author florian
 *
 */
@Named
@SessionScoped
public class CommentBean implements Serializable {

    /**
     * 
     */
    private static final String REQUIRES_COMMENT_RIGHTS = "REQUIRES_COMMENT_RIGHTS";
    private static final long serialVersionUID = -3653100353345867739L;

    private final CommentManager commentManager;

    @Inject
    private ActiveDocumentBean activeDocumentBean;
    @Inject
    private UserBean userBean;

    private Boolean userCommentsEnabled;

    private Map<String, Boolean> editCommentPermissionMap = new HashMap<>();
    private Map<String, Boolean> deleteCommentPermissionMap = new HashMap<>();

    /**
     * 
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public CommentBean() throws IndexUnreachableException, DAOException {
        commentManager = new CommentManager(
                new SolrAndSqlAnnotationSaver(),
                new SqlAnnotationDeleter(),
                new SqlCommentLister(),
                new CommentMailNotificator(),
                new JsfMessagesNotificator());
    }

    /**
     * 
     * @param text
     * @param restricted
     * @throws AjaxResponseException
     */
    public void createComment(String text, boolean restricted) throws AjaxResponseException {
        try {
            this.commentManager.createComment(text, userBean.getUser(), activeDocumentBean.getViewManager().getPi(),
                    activeDocumentBean.getViewManager().getCurrentImageOrder(), getLicense(restricted), getInitialPublicationStatus());
        } catch (IndexUnreachableException e) {
            throw new AjaxResponseException(e.toString());
        }
    }

    /**
     * 
     * @throws PresentationException
     */
    public void editComment() throws PresentationException {
        String idString = Faces.getRequestParameter("id");
        String text = Faces.getRequestParameter("text");
        try {
            Long id = Long.parseLong(idString);
            Comment comment = this.commentManager.getAnnotation(id).orElseThrow(() -> new DAOException("No comment found with id " + id));
            editComment(comment, text, comment.getAccessCondition() == null || SolrConstants.OPEN_ACCESS_VALUE.equals(comment.getAccessCondition()));
        } catch (NumberFormatException e) {
            throw new AjaxResponseException("Cannot load comment with id " + idString);
        } catch (DAOException | IndexUnreachableException e) {
            throw new AjaxResponseException("Error updating comment: " + e.getMessage());
        }
    }

    /**
     * 
     * @param original
     * @param text
     * @param restricted
     * @throws DAOException
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public void editComment(Comment original, String text, boolean restricted) throws DAOException, PresentationException, IndexUnreachableException {
        if (isMayEditCommentsForRecord(original.getTargetPI())) {
            this.commentManager.editComment(original, text, userBean.getUser(), getLicense(restricted), getInitialPublicationStatus());
        }
    }

    /**
     * 
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public void deleteComment() throws PresentationException, IndexUnreachableException {
        String idString = Faces.getRequestParameter("id");
        try {
            Long id = Long.parseLong(idString);
            Comment comment = this.commentManager.getAnnotation(id).orElseThrow(() -> new DAOException("No comment found with id " + id));
            deleteComment(comment);
        } catch (NumberFormatException e) {
            throw new AjaxResponseException("Cannot load comment with id " + idString);
        } catch (DAOException e) {
            throw new AjaxResponseException("Error deleting comment: " + e.getMessage());
        }
    }

    /**
     * 
     * @param annotation
     * @throws DAOException
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public void deleteComment(Comment annotation) throws DAOException, PresentationException, IndexUnreachableException {
        if (isMayDeleteCommentsForRecord(annotation.getTargetPI())) {
            this.commentManager.deleteComment(annotation);
        }
    }

    /**
     * 
     * @param startIndex
     * @param numItems
     * @param filter
     * @param user
     * @param sortField
     * @param descending
     * @return
     */
    public List<Comment> getComments(int startIndex, int numItems, String filter, User user, String sortField, boolean descending) {
        return this.commentManager.getAnnotations(startIndex, numItems, filter, null, null, Collections.singletonList(user.getId()), null, null,
                sortField, descending);
    }

    /**
     * 
     * @return
     * @throws IndexUnreachableException
     */
    public List<Comment> getCommentsForCurrentPage() throws IndexUnreachableException {
        if (!activeDocumentBean.isRecordLoaded()) {
            return Collections.emptyList();
        }

        return this.commentManager
                .getAnnotations(0, Integer.MAX_VALUE, null, null, null, null, activeDocumentBean.getViewManager().getPi(),
                        activeDocumentBean.getViewManager().getCurrentImageOrder(), null, false)
                .stream()
                .filter(c -> PublicationStatus.PUBLISHED.equals(c.getPublicationStatus())
                        || Optional.ofNullable(c.getCreatorId()).map(id -> id.equals(getCurrentUserId())).orElse(false))
                //TODO: Check privilege for viewing comment
                //.filter(c -> Optional.ofNullable(userBean).map(UserBean::getUser).map(u -> u.isHasAnnotationPrivilege(REQUIRES_COMMENT_RIGHTS)).orElse(false))
                .collect(Collectors.toList());
    }

    /**
     * 
     * @return
     */
    private Long getCurrentUserId() {
        return Optional.ofNullable(userBean).map(UserBean::getUser).map(User::getId).orElse(null);
    }

    /**
     * 
     * @param anno
     * @return
     */
    public boolean isRestricted(CrowdsourcingAnnotation anno) {
        return REQUIRES_COMMENT_RIGHTS.equals(anno.getAccessCondition());
    }

    /**
     * 
     * @param restricted
     * @return
     */
    private String getLicense(boolean restricted) {
        return restricted ? getRestrictedLicense() : getPublicLicense();
    }

    /**
     * @return
     */
    private PublicationStatus getInitialPublicationStatus() {
        if (DataManager.getInstance().getConfiguration().reviewEnabledForComments()) {
            return PublicationStatus.REVIEW;
        }

        return PublicationStatus.PUBLISHED;
    }

    /**
     * @return
     */
    private String getPublicLicense() {
        return SolrConstants.OPEN_ACCESS_VALUE;
    }

    /**
     * @return
     */
    private String getRestrictedLicense() {
        return REQUIRES_COMMENT_RIGHTS;
    }

    /**
     * <p>
     * isUserCommentsEnabled.
     * </p>
     *
     * @return a boolean.
     * @throws DAOException
     */
    public boolean isUserCommentsEnabled() throws DAOException {
        if (userCommentsEnabled == null) {
            CommentGroup commentGroupAll = DataManager.getInstance().getDao().getCommentGroupUnfiltered();
            if (commentGroupAll != null) {
                userCommentsEnabled = commentGroupAll.isEnabled();
            } else {
                userCommentsEnabled = false;
            }
        }

        return userCommentsEnabled;
    }

    /**
     * Checks whether the current user may edit comments for the given record identifier, based on their admin status or membership in any user group
     * that has such permission via comment groups.
     * 
     * @param pi Record identifier
     * @return
     * @throws DAOException
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public boolean isMayEditCommentsForRecord(String pi) throws DAOException, PresentationException, IndexUnreachableException {
        if (userBean == null || userBean.getUser() == null || pi == null) {
            return false;
        }

        if (editCommentPermissionMap.get(pi) == null) {
            editCommentPermissionMap.put(pi, false);
            checkEditDeletePermissionsForRecord(pi);
        }

        return editCommentPermissionMap.get(pi);
    }

    /**
     * Checks whether the current user may delete comments for the given record identifier, based on their admin status or membership in any user
     * group that has such permission via comment groups.
     * 
     * @param pi Record identifier
     * @return
     * @throws DAOException
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public boolean isMayDeleteCommentsForRecord(String pi) throws DAOException, PresentationException, IndexUnreachableException {
        if (userBean == null || userBean.getUser() == null || pi == null) {
            return false;
        }

        if (deleteCommentPermissionMap.get(pi) == null) {
            deleteCommentPermissionMap.put(pi, false);
            checkEditDeletePermissionsForRecord(pi);
        }

        return deleteCommentPermissionMap.get(pi);
    }

    /**
     * Enables edit/delete privileges for the current session for the given record identifier, based on admin status or {@link CommentGroup} settings.
     * 
     * @param pi
     * @throws DAOException
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    void checkEditDeletePermissionsForRecord(String pi) throws DAOException, PresentationException, IndexUnreachableException {
        if (userBean == null || userBean.getUser() == null) {
            return;
        }

        // Always allow for admins
        if (userBean.getUser().isSuperuser()) {
            editCommentPermissionMap.put(pi, true);
            deleteCommentPermissionMap.put(pi, true);
            return;
        }

        // Check permissions granted via CommentGroups
        Set<CommentGroup> commentGroups = CommentManager.getRelevantCommentGroupsForRecord(pi);
        for (CommentGroup commentGroup : commentGroups) {
            if (commentGroup.getUserGroup() != null && (userBean.getUser().equals(commentGroup.getUserGroup().getOwner())
                    || commentGroup.getUserGroup().getMembers().contains(userBean.getUser()))) {
                if (commentGroup.isMembersMayEditComments()) {
                    editCommentPermissionMap.put(pi, true);
                }
                if (commentGroup.isMembersMayDeleteComments()) {
                    deleteCommentPermissionMap.put(pi, true);
                }
            }
        }
    }
}

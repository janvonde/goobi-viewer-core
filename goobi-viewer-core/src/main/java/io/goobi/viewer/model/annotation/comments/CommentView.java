package io.goobi.viewer.model.annotation.comments;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import io.goobi.viewer.model.security.user.UserGroup;

/**
 * Filtered view on collections.
 */
@Entity
@Table(name = "annotations_comment_views")
public class CommentView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_view_id")
    private Long id;

    @Column(name = "title", nullable = true)
    private String title;

    @Column(name = "solr_query", nullable = true)
    private String solrQuery;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "user_group_id")
    private UserGroup userGroup;

    @Column(name = "send_email_notifications")
    private boolean sendEmailNotifications = false;

    @Column(name = "members_may_edit_comments")
    private boolean membersMayEditComments = false;

    @Column(name = "members_may_delete_comments")
    private boolean membersMayDeleteComments = false;

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
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the solrQuery
     */
    public String getSolrQuery() {
        return solrQuery;
    }

    /**
     * @param solrQuery the solrQuery to set
     */
    public void setSolrQuery(String solrQuery) {
        this.solrQuery = solrQuery;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the userGroup
     */
    public UserGroup getUserGroup() {
        return userGroup;
    }

    /**
     * @param userGroup the userGroup to set
     */
    public void setUserGroup(UserGroup userGroup) {
        this.userGroup = userGroup;
    }

    /**
     * @return the sendEmailNotifications
     */
    public boolean isSendEmailNotifications() {
        return sendEmailNotifications;
    }

    /**
     * @param sendEmailNotifications the sendEmailNotifications to set
     */
    public void setSendEmailNotifications(boolean sendEmailNotifications) {
        this.sendEmailNotifications = sendEmailNotifications;
    }

    /**
     * @return the membersMayEditComments
     */
    public boolean isMembersMayEditComments() {
        return membersMayEditComments;
    }

    /**
     * @param membersMayEditComments the membersMayEditComments to set
     */
    public void setMembersMayEditComments(boolean membersMayEditComments) {
        this.membersMayEditComments = membersMayEditComments;
    }

    /**
     * @return the membersMayDeleteComments
     */
    public boolean isMembersMayDeleteComments() {
        return membersMayDeleteComments;
    }

    /**
     * @param membersMayDeleteComments the membersMayDeleteComments to set
     */
    public void setMembersMayDeleteComments(boolean membersMayDeleteComments) {
        this.membersMayDeleteComments = membersMayDeleteComments;
    }
}

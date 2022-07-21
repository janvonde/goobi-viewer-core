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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.BCrypt;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.DownloadTicket;

@Named
@SessionScoped
public class BornDigitalBean implements Serializable {

    private static final long serialVersionUID = -371794671604543166L;

    @Inject
    private ActiveDocumentBean activeDocumentBean;

    private transient String downloadTicketPassword;
    private String downloadTicketEmail;
    private String downloadTicketRequestMessage;

    /**
     * 
     * @return true if session contains permission for current record, false otherwise;
     * @throws IndexUnreachableException
     */
    public boolean isHasDownloadTicket() throws IndexUnreachableException {
        return AccessConditionUtils.isHasDownloadTicket(activeDocumentBean.getPersistentIdentifier(), BeanUtils.getSession());
    }

    /**
     * Checks the given download ticket password for validity for the current record and persists valid permission in the agent session.
     * 
     * @param password Ticket password to check
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    public String checkDownloadTicketPasswordAction() throws DAOException, IndexUnreachableException {
        if (StringUtils.isEmpty(downloadTicketPassword)) {
            Messages.error("errPassword");
            return "";
        }

        try {
            String hash = BCrypt.hashpw(downloadTicketPassword, DownloadTicket.SALT);
            DownloadTicket ticket = DataManager.getInstance().getDao().getDownloadTicketByPasswordHash(hash);
            String pi = activeDocumentBean.getPersistentIdentifier();
            if ("-".equals(pi)) {
                Messages.error("errPassword");
                return "";
            }
            if (ticket != null && ticket.isActive() && ticket.getPi().equals(pi) && ticket.checkPassword(downloadTicketPassword)
                    && AccessConditionUtils.addPermissionToSession(pi, BeanUtils.getSession())) {
                Messages.info("");
                return "";
            }

            Messages.error("errPassword");
            return "";
        } finally {
            downloadTicketPassword = null;
        }
    }

    /**
     * 
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    public String requestNewDownloadTicketAction() throws DAOException, IndexUnreachableException {
        if (StringUtils.isEmpty(downloadTicketEmail)) {
            Messages.error(StringConstants.MSG_ADMIN_SAVE_ERROR);
            return "";
        }

        DownloadTicket ticket = new DownloadTicket();
        if (activeDocumentBean != null && activeDocumentBean.isRecordLoaded()) {
            ticket.setPi(activeDocumentBean.getPersistentIdentifier());
            ticket.setTitle(activeDocumentBean.getViewManager().getTopDocumentTitle());
        }
        ticket.setEmail(downloadTicketEmail);
        if (StringUtils.isNotEmpty(downloadTicketRequestMessage)) {
            ticket.setRequestMessage(downloadTicketRequestMessage);
        }

        if (DataManager.getInstance().getDao().addDownloadTicket(ticket)) {
            Messages.info("download_ticket_request_created");
        } else {
            Messages.error(StringConstants.MSG_ADMIN_SAVE_ERROR);
        }

        return "";
    }

    /**
     * @return the downloadTicketPassword
     */
    public String getDownloadTicketPassword() {
        return downloadTicketPassword;
    }

    /**
     * @param downloadTicketPassword the downloadTicketPassword to set
     */
    public void setDownloadTicketPassword(String downloadTicketPassword) {
        this.downloadTicketPassword = downloadTicketPassword;
    }

    /**
     * @return the downloadTicketEmail
     */
    public String getDownloadTicketEmail() {
        return downloadTicketEmail;
    }

    /**
     * @param downloadTicketEmail the downloadTicketEmail to set
     */
    public void setDownloadTicketEmail(String downloadTicketEmail) {
        this.downloadTicketEmail = downloadTicketEmail;
    }

    /**
     * @return the downloadTicketRequestMessage
     */
    public String getDownloadTicketRequestMessage() {
        return downloadTicketRequestMessage;
    }

    /**
     * @param downloadTicketRequestMessage the downloadTicketRequestMessage to set
     */
    public void setDownloadTicketRequestMessage(String downloadTicketRequestMessage) {
        this.downloadTicketRequestMessage = downloadTicketRequestMessage;
    }
}

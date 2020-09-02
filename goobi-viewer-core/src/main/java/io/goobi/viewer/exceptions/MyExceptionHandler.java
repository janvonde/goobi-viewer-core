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
package io.goobi.viewer.exceptions;

import java.net.SocketException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import javax.faces.FacesException;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ViewExpiredException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ocpsoft.pretty.PrettyException;

import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;

/*
 * taken from here:
 * http://www.facebook.com/note.php?note_id=125229397708&comments&ref=mf
 */
/**
 * <p>
 * MyExceptionHandler class.
 * </p>
 */
public class MyExceptionHandler extends ExceptionHandlerWrapper {

    private static final Logger logger = LoggerFactory.getLogger(MyExceptionHandler.class);

    /**
     * <p>
     * Constructor for MyExceptionHandler.
     * </p>
     *
     * @param wrapped a {@link javax.faces.context.ExceptionHandler} object.
     */
    public MyExceptionHandler(ExceptionHandler wrapped) {
        super(wrapped);
    }

    /** {@inheritDoc} */
    @Override
    public ExceptionHandler getWrapped() {
        return super.getWrapped();
    }

    /** {@inheritDoc} */
    @Override
    public void handle() throws FacesException {
        for (Iterator<ExceptionQueuedEvent> i = getUnhandledExceptionQueuedEvents().iterator(); i.hasNext();) {
            ExceptionQueuedEvent event = i.next();
            ExceptionQueuedEventContext context = (ExceptionQueuedEventContext) event.getSource();
            Throwable t = context.getException();
            // Handle ViewExpiredExceptions here ... or even others :)
            if (!t.getClass().equals(ViewExpiredException.class) && !t.getClass().equals(PrettyException.class)) {
                logger.error("CLASS: {}", t.getClass().getName());
            } else {
                logger.trace(t.getClass().getSimpleName());
            }
            FacesContext fc = FacesContext.getCurrentInstance();
            if (fc == null) {
                return;
            }
            Map<String, Object> requestMap = fc.getExternalContext().getRequestMap();
            NavigationHandler nav = fc.getApplication().getNavigationHandler();
            Flash flash = fc.getExternalContext().getFlash();
            NavigationHelper navigationHelper = BeanUtils.getNavigationHelper();
            flash.setKeepMessages(true);
            if (navigationHelper != null) {
                requestMap.put("sourceUrl", navigationHelper.getCurrentUrl());
                // Flash data can only be read once
                flash.put("sourceUrl", navigationHelper.getCurrentUrl());
                flash.put("sourceUrl2", navigationHelper.getCurrentUrl());
            }
            if (t instanceof ViewExpiredException) {
                ViewExpiredException vee = (ViewExpiredException) t;
                try {
                    // Push some useful stuff to the request scope for use in the page
                    requestMap.put("currentViewId", vee.getViewId());
                    requestMap.put("errorType", "viewExpired");
                    flash.put("errorType", "viewExpired");
                    HttpSession session = (HttpSession) fc.getExternalContext().getSession(false);
                    if (session != null) {
                        StringBuilder details = new StringBuilder();

                        details.append("Session ID: ").append(session.getId());
                        details.append("</br>");
                        details.append("Session created: ").append(new Date(session.getCreationTime()));
                        details.append("</br>");
                        details.append("Session last accessed: ").append(new Date(session.getLastAccessedTime()));

                        Optional<Map<Object, Map>> logicalViews =
                                Optional.ofNullable((Map) session.getAttribute("com.sun.faces.renderkit.ServerSideStateHelper.LogicalViewMap"));
                        Integer numberOfLogicalViews = logicalViews.map(map -> map.keySet().size()).orElse(0);
                        Integer numberOfTotalViews =
                                logicalViews.map(map -> map.values().stream().mapToInt(value -> value.keySet().size()).sum()).orElse(0);
                        details.append("</br>");
                        details.append("Logical Views stored in session: ").append(numberOfLogicalViews.toString());
                        details.append("</br>");
                        details.append("Total views stored in session: ").append(numberOfTotalViews.toString());

                        flash.put("errorDetails", details.toString());
                    } else {
                        flash.put("errorDetails", "No session details available");
                    }

                    nav.handleNavigation(fc, null, "pretty:error");
                    fc.renderResponse();
                } finally {
                    i.remove();
                }
            } else if (t instanceof RecordNotFoundException || isCausedByExceptionType(t, RecordNotFoundException.class.getName())
                    || (t instanceof PrettyException && t.getMessage().contains(RecordNotFoundException.class.getSimpleName()))) {
                try {
                    String pi =
                            t.getMessage().substring(t.getMessage().indexOf("RecordNotFoundException: ")).replace("RecordNotFoundException: ", "");
                    String msg = ViewerResourceBundle.getTranslation("errRecordNotFoundMsg", null).replace("{0}", pi);
                    flash.put("errorDetails", msg);
                    requestMap.put("errMsg", msg);
                    requestMap.put("errorType", "recordNotFound");
                    flash.put("errorType", "recordNotFound");
                    nav.handleNavigation(fc, null, "pretty:error");
                    fc.renderResponse();
                } finally {
                    i.remove();
                }
            } else if (t instanceof RecordDeletedException || isCausedByExceptionType(t, RecordDeletedException.class.getName())
                    || (t instanceof PrettyException && t.getMessage().contains(RecordDeletedException.class.getSimpleName()))) {
                try {
                    String pi = t.getMessage().substring(t.getMessage().indexOf("RecordDeletedException: ")).replace("RecordDeletedException: ", "");
                    String msg = ViewerResourceBundle.getTranslation("errRecordDeletedMsg", null).replace("{0}", pi);
                    flash.put("errorDetails", msg);
                    requestMap.put("errMsg", msg);
                    requestMap.put("errorType", "recordDeleted");
                    flash.put("errorType", "recordDeleted");
                    nav.handleNavigation(fc, null, "pretty:error");
                    fc.renderResponse();
                } finally {
                    i.remove();
                }
            } else if (t instanceof IndexUnreachableException || isCausedByExceptionType(t, IndexUnreachableException.class.getName())
                    || (t instanceof PrettyException && t.getMessage().contains(IndexUnreachableException.class.getSimpleName()))) {
                logger.trace("Caused by IndexUnreachableException");
                logger.error(t.getMessage());
                try {
                    requestMap.put("errorType", "indexUnreachable");
                    flash.put("errorType", "indexUnreachable");
                    nav.handleNavigation(fc, null, "pretty:error");
                    fc.renderResponse();
                } finally {
                    i.remove();
                }
            } else if (t instanceof DAOException || isCausedByExceptionType(t, DAOException.class.getName())
                    || (t instanceof PrettyException && t.getMessage().contains(IndexUnreachableException.class.getSimpleName()))) {
                logger.trace("Caused by DAOException");
                try {
                    requestMap.put("errorType", "dao");
                    flash.put("errorType", "dao");
                    nav.handleNavigation(fc, null, "pretty:error");
                    fc.renderResponse();
                } finally {
                    i.remove();
                }
            } else if (t instanceof ViewerConfigurationException || isCausedByExceptionType(t, ViewerConfigurationException.class.getName())
                    || (t instanceof PrettyException && t.getMessage().contains(ViewerConfigurationException.class.getSimpleName()))) {
                logger.trace("Caused by ViewerConfigurationException");
                String msg = getRootCause(t).getMessage();
                logger.error(getRootCause(t).getMessage());
                try {
                    flash.put("errorDetails", msg);
                    requestMap.put("errMsg", msg);
                    requestMap.put("errorType", "configuration");
                    flash.put("errorType", "configuration");
                    nav.handleNavigation(fc, null, "pretty:error");
                    fc.renderResponse();
                } finally {
                    i.remove();
                }
            } else if (t instanceof SocketException || isCausedByExceptionType(t, SocketException.class.getName())
                    || (t instanceof PrettyException && t.getMessage().contains(SocketException.class.getSimpleName()))) {

                try {
                } finally {
                    i.remove();
                }
            } else if (t instanceof DownloadException || isCausedByExceptionType(t, DownloadException.class.getName())
                    || (t instanceof PrettyException && t.getMessage().contains(DownloadException.class.getSimpleName()))) {
                logger.error(getRootCause(t).getMessage());
                String msg = getRootCause(t).getMessage();
                if (msg.contains(DownloadException.class.getSimpleName() + ":")) {
                    msg = msg.substring(StringUtils.lastIndexOf(msg, ":") + 1).trim();
                }
                try {
                    flash.put("errorDetails", msg);
                    requestMap.put("errMsg", msg);
                    requestMap.put("errorType", "download");
                    flash.put("errorType", "download");
                    nav.handleNavigation(fc, null, "pretty:error");
                    fc.renderResponse();
                } finally {
                    i.remove();
                }
            } else {
                // All other exceptions
                logger.error(t.getMessage(), t);
                try {
                    // Put the exception in the flash scope to be displayed in the error page if necessary ...
                    String msg = DateTools.format(new Date(), DateTools.formatterISO8601DateTime, false) + ": " + t.getMessage();
                    // flash.put("errorDetails", msg);
                    requestMap.put("errMsg", msg);
                    requestMap.put("errorType", "general");
                    flash.put("errorType", "general");
                    nav.handleNavigation(fc, null, "pretty:error");
                    fc.renderResponse();
                } finally {
                    i.remove();
                }
            }

        }

        // At this point, the queue will not contain any ViewExpiredEvents.
        // Therefore, let the parent handle them.
        getWrapped().handle();

    }

    /**
     * Checks whether the given Throwable was at some point caused by an IndexUnreachableException.
     *
     * @param t
     * @return true if the root cause of the exception is className
     */
    @SuppressWarnings("rawtypes")
    private static boolean isCausedByExceptionType(Throwable t, String className) {
        Throwable cause = t;
        Class clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        while (cause != null) {
            if (cause.getClass().equals(clazz)) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
    }

}

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
package io.goobi.viewer.api.rest.v1.tasks;

import static io.goobi.viewer.api.rest.v1.ApiUrls.TASKS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.TASKS_TASK;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.jms.JMSException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.filters.AdminLoggedInFilter;
import io.goobi.viewer.api.rest.filters.AuthorizationFilter;
import io.goobi.viewer.api.rest.model.SitemapRequestParameters;
import io.goobi.viewer.api.rest.model.ToolsRequestParameters;
import io.goobi.viewer.api.rest.model.tasks.Task;
import io.goobi.viewer.api.rest.model.tasks.Task.TaskType;
import io.goobi.viewer.api.rest.model.tasks.TaskManager;
import io.goobi.viewer.api.rest.model.tasks.TaskParameter;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageGenerator;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.model.job.mq.IndexUsageHandler;
import io.goobi.viewer.model.job.mq.NotifySearchUpdateHandler;
import io.goobi.viewer.servlets.utils.ServletUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * Create and monitor (possibly time consuming) {@link Task tasks} within the viewer. These tasks are managed by the {@link TaskManager}
 *
 * @author florian
 *
 */
@Path(TASKS)
public class TasksResource {

    private static final Logger logger = LogManager.getLogger(TasksResource.class);
    private final HttpServletRequest request;

    @Context
    private HttpServletRequest httpRequest;

    public TasksResource(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        this.request = request;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "tasks" }, summary = "Create a (possibly time consuming) task to execute in a limited thread pool. See javadoc for details")
    public Task addTask(TaskParameter desc) throws WebApplicationException {
        if (desc == null || desc.type == null) {
            throw new WebApplicationException(new IllegalRequestException("Must provide job type"));
        }
        if (isAuthorized(desc.type, Optional.empty(), request)) {

            if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
                ViewerMessage message = null;
                switch (desc.type) {
                    case NOTIFY_SEARCH_UPDATE:
                        message = MessageGenerator.generateSimpleMessage(NotifySearchUpdateHandler.NAME);
                        break;

                    case PURGE_EXPIRED_DOWNLOAD_TICKETS:
                        message = MessageGenerator.generateSimpleMessage("PURGE_EXPIRED_DOWNLOAD_TICKETS");
                        break;
                    case SEARCH_EXCEL_EXPORT:
                        message = MessageGenerator.generateSimpleMessage("SEARCH_EXCEL_EXPORT");
                        break;
                    case UPDATE_SITEMAP:
                        message = MessageGenerator.generateSimpleMessage("UPDATE_SITEMAP");

                        SitemapRequestParameters params = Optional.ofNullable(desc)
                                .filter(SitemapRequestParameters.class::isInstance)
                                .map(SitemapRequestParameters.class::cast)
                                .orElse(null);

                        String viewerRootUrl = ServletUtils.getServletPathWithHostAsUrlFromRequest(request);
                        String outputPath = request.getServletContext().getRealPath("/");
                        if (params != null && StringUtils.isNotBlank(params.getOutputPath())) {
                            outputPath = params.getOutputPath();
                        }
                        message.getProperties().put("viewerRootUrl", viewerRootUrl);
                        message.getProperties().put("baseurl", outputPath);

                        break;
                    case UPDATE_DATA_REPOSITORY_NAMES:

                        ToolsRequestParameters tools = Optional.ofNullable(desc)
                                .filter(ToolsRequestParameters.class::isInstance)
                                .map(ToolsRequestParameters.class::cast)
                                .orElse(null);
                        if (tools == null) {
                            return null;
                        }

                        message = MessageGenerator.generateSimpleMessage("UPDATE_DATA_REPOSITORY_NAMES");
                        String identifier = tools.getPi();
                        String dataRepositoryName = tools.getDataRepositoryName();

                        message.getProperties().put("identifier", identifier);
                        message.getProperties().put("dataRepositoryName", dataRepositoryName);

                        break;
                    case UPDATE_UPLOAD_JOBS:
                        message = MessageGenerator.generateSimpleMessage("UPDATE_UPLOAD_JOBS");
                        break;
                    case INDEX_USAGE_STATISTICS:
                        message = MessageGenerator.generateSimpleMessage(IndexUsageHandler.NAME);
                        break;
                    default:
                        // unknown type
                        return null;
                }

                String identifier = UUID.randomUUID().toString();
                message.setPi(identifier);
                try {
                    MessageGenerator.submitInternalMessage(message, "viewer", message.getTaskName(), identifier);
                } catch (JMSException | JsonProcessingException e) {
                    // mq is not reachable
                    logger.error(e);
                }

                // TODO create useful response, containing the message id
                return null;
            } else {
                Task job = new Task(desc, TaskManager.createTask(desc.type));
                logger.debug("Created new task REST API task '{}'", job);
                DataManager.getInstance().getRestApiJobManager().addTask(job);
                DataManager.getInstance().getRestApiJobManager().triggerTaskInThread(job.id, request);
                return job;
            }
        }

        throw new WebApplicationException(new AccessDeniedException("Not authorized to create this type of job"));
    }

    @GET
    @Path(TASKS_TASK)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "tasks" },
            summary = "Return the task with the given id, provided it is accessibly by the request (determined by session or access token)")
    public Task getTask(@Parameter(description = "The id of the task") @PathParam("id") Long id) throws ContentNotFoundException {
        Task job = DataManager.getInstance().getRestApiJobManager().getTask(id);
        if (job == null || !isAuthorized(job.type, job.sessionId, request)) {
            throw new ContentNotFoundException("No Job found with id " + id);
        }

        return job;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "tasks" }, summary = "Return a list of all tasks accessible to the request (determined by session or access token)")
    public List<Task> getTasks() {
        return DataManager.getInstance()
                .getRestApiJobManager()
                .getTasks()
                .stream()
                .filter(job -> this.isAuthorized(job.type, job.sessionId, request))
                .collect(Collectors.toList());
    }

    public boolean isAuthorized(TaskType type, Optional<String> jobSessionId, HttpServletRequest request) {
        Task.Accessibility access = Task.getAccessibility(type);
        switch (access) {
            case PUBLIC:
                return true;
            case TOKEN:
                return AuthorizationFilter.isAuthorized(request);
            case ADMIN:
                return AdminLoggedInFilter.isAdminLoggedIn(request);
            case SESSION:
                String sessionId = request.getSession().getId();
                if (sessionId != null) {
                    return jobSessionId.map(id -> id.equals(sessionId)).orElse(false);
                }
                return false;
            default:
                return false;
        }
    }
}

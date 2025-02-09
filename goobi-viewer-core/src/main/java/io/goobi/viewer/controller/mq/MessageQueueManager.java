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

package io.goobi.viewer.controller.mq;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.ExportException;
import java.rmi.server.RMIServerSocketFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import javax.inject.Singleton;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.jmx.QueueViewMBean;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.cdi.Startup;
import org.reflections.Reflections;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.MessageQueueException;
import io.goobi.viewer.managedbeans.MessageQueueBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.job.TaskType;

/**
 * Manages handling of messages by their respective MessageHandlers. Main method is {@link #handle(ViewerMessage message)} which accepts a
 * {@link ViewerMessage} and calls a {@link MessageHandler} instance to process the message, returning a {@link MessageStatus} result. #handle may
 * either be called directly to handle the message instantly, or from a {@link MessageQueue
 * 
 * @author florian
 *
 */
@Singleton
@Startup
public class MessageQueueManager {

    static final String ACTIVE_MQ_CONFIG_FILENAME = "config_activemq.xml";

    private static final int SERVER_REGISTRY_PORT = 1095;

    public static final String QUEUE_NAME_VIEWER = "viewer";
    public static final String QUEUE_NAME_PDF = "pdf";

    private static final Logger logger = LogManager.getLogger(MessageQueueManager.class);

    private final Map<String, MessageHandler<MessageStatus>> instances;
    private final IDAO dao;
    private boolean queueRunning = false;
    private ActiveMQConfig config = null;
    private RMIConnectorServer rmiServer = null;
    private BrokerService broker = null;
    private List<DefaultQueueListener> listeners = new ArrayList<>();

    public MessageQueueManager() throws DAOException, IOException {
        this.instances = generateTicketHandlers();
        this.dao = DataManager.getInstance().getDao();
        try {
            this.config = new ActiveMQConfig(ACTIVE_MQ_CONFIG_FILENAME);
        } catch (FileNotFoundException e) {
            this.config = null;
        }
    }

    public MessageQueueManager(ActiveMQConfig config, IDAO dao) {
        this.dao = dao;
        this.instances = generateTicketHandlers();
        this.config = config;
    }

    public MessageQueueManager(ActiveMQConfig config, IDAO dao, Map<String, MessageHandler<MessageStatus>> instances) {
        this.instances = instances;
        this.dao = dao;
        this.config = config;
    }

    /**
     * Add the message to the internal message queue to be handled later
     * 
     * @param message
     * @return
     * @throws MessageQueueException
     */
    public String addToQueue(ViewerMessage message) throws MessageQueueException {
        if (this.isQueueRunning()) {
            try (Connection conn = startConnection()) {
                return submitTicket(message, getQueueForMessageType(message.getTaskName()), conn, message.getTaskName());
            } catch (JsonProcessingException | JMSException e) {
                logger.error("Error adding message {}/{} to queue: {}", message.getTaskName(), message.getMessageId(), e.toString(), e);
                return null;
            } finally {
                notifyMessageQueueStateUpdate();
            }
        } else {
            throw new MessageQueueException("Message queue is not running");
        }
    }

    public static String getQueueForMessageType(String taskName) {
        try {
            TaskType type = TaskType.valueOf(taskName);
            switch (type) {
                case PRERENDER_PDF:
                    return QUEUE_NAME_PDF;
                default:
                    return QUEUE_NAME_VIEWER;
            }
        } catch (NullPointerException | IllegalArgumentException e) {
            logger.error("Error parsing TaskType for name {}", taskName);
            return QUEUE_NAME_VIEWER;
        }
    }

    /**
     * Send a notification to the "messageQueueState" WebSocket to update message lists in the admin backend
     */
    public static void notifyMessageQueueStateUpdate() {
        MessageQueueBean mqBean = (MessageQueueBean) BeanUtils.getBeanByName("messageQueueBean", MessageQueueBean.class);
        if (mqBean != null) {
            mqBean.updateMessageQueueState();
        }
    }

    /**
     * Finds the appropriate MessageHandler for a message, lets the handler handle the message and update the message in the database
     * 
     * @param message
     * @return the result of the handler calling the message
     */
    public MessageStatus handle(ViewerMessage message) {

        MessageHandler<MessageStatus> handler = instances.get(message.getTaskName());
        if (handler == null) {
            return MessageStatus.ERROR;
        }
        MessageStatus rv = handler.call(message);
        updateMessageStatus(message, rv);

        return rv;
    }

    public boolean initializeMessageServer() {
        return initializeMessageServer("localhost", SERVER_REGISTRY_PORT, 0);
    }

    public boolean initializeMessageServer(String address, int namingPort, int protocolPort) {

        // JMX/RMI part taken from: https://vafer.org/blog/20061010091658/
        try {
            RMIServerSocketFactory serverFactory = new RMIServerSocketFactoryImpl(InetAddress.getByName(address));
            try {
                LocateRegistry.createRegistry(namingPort, null, serverFactory);
            } catch (ExportException e) {
                logger.trace("Cannot create registry, already in use");
            }

            StringBuilder url = new StringBuilder();
            url.append("service:jmx:");
            url.append("rmi://").append(address).append(':').append(protocolPort).append("/jndi/");
            url.append("rmi://").append(address).append(':').append(namingPort).append("/connector");

            Map<String, Object> env = new HashMap<>();
            env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, serverFactory);

            rmiServer = new RMIConnectorServer(
                    new JMXServiceURL(url.toString()),
                    env,
                    ManagementFactory.getPlatformMBeanServer());

            rmiServer.start();

        } catch (IOException e1) {
            logger.error("error starting JMX connector. Will not start internal MessageBroker. Exception: {}", e1.toString(), e1);
            return false;
        }
        try {
            broker = BrokerFactory.createBroker("xbean:" + PathConverter.toURI(this.config.getConfigFilePath().toAbsolutePath()).toString(), false);
            broker.setUseJmx(true);
            broker.start();

        } catch (Exception e) {
            logger.error(e);
            return false;
        }

        try {
            for (int i = 0; i < DataManager.getInstance().getConfiguration().getNumberOfParallelMessages(); i++) {
                DefaultQueueListener listener = new DefaultQueueListener(this);
                listener.register(QUEUE_NAME_VIEWER);
                listeners.add(listener);
            }
            for (int i = 0; i < DataManager.getInstance().getConfiguration().getNumberOfParallelMessages(); i++) {
                DefaultQueueListener listener = new DefaultQueueListener(this);
                listener.register(QUEUE_NAME_PDF);
                listeners.add(listener);
            }

        } catch (JMSException e) {
            logger.error(e);
            return false;
        }
        this.queueRunning = true;
        return true;
    }

    public void closeMessageServer() {
        try {
            for (DefaultQueueListener l : listeners) {
                l.close(); //includes a join for the listener thread
            }
            if (broker != null) {
                broker.stop();
            }
            if (rmiServer != null) {
                rmiServer.stop();
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private void updateMessageStatus(ViewerMessage message, MessageStatus rv) {
        message.setMessageStatus(rv);
        message.setQueue(MessageQueueManager.getQueueForMessageType(message.getTaskName()));
        message.setLastUpdateTime(LocalDateTime.now());
        try {
            if (message.getId() == null) {
                dao.addViewerMessage(message);
            } else {
                dao.updateViewerMessage(message);
            }
        } catch (DAOException e) {
            logger.error(e);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Map<String, MessageHandler<MessageStatus>> generateTicketHandlers() {
        Map<String, MessageHandler<MessageStatus>> handlers = new HashMap<>();
        Set<Class<? extends MessageHandler>> ticketHandlers = new Reflections("io.goobi.viewer.model.job.mq.*").getSubTypesOf(MessageHandler.class);
        for (Class<? extends MessageHandler> clazz : ticketHandlers) {
            try {
                MessageHandler<MessageStatus> handler = clazz.getDeclaredConstructor().newInstance();
                handlers.put(handler.getMessageHandlerName(), handler);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException
                    | SecurityException e) {
                logger.error(e);
            }
        }
        return handlers;
    }

    private static String submitTicket(ViewerMessage ticket, String queueName, Connection conn, String ticketType)
            throws JMSException, JsonProcessingException {

        Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final Destination dest = sess.createQueue(queueName);
        MessageProducer producer = sess.createProducer(dest);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        TextMessage message = sess.createTextMessage();
        // we set a random UUID here, because otherwise tickets will not be processed in parallel in an SQS fifo queue.
        // we still need a fifo queue for message deduplication, though.
        // See: https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-additional-fifo-queue-recommendations.html
        message.setStringProperty("JMSXGroupID", UUID.randomUUID().toString());

        message.setText(new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(ticket));
        message.setStringProperty("JMSType", ticketType);
        for (Map.Entry<String, String> entry : ticket.getProperties().entrySet()) {
            message.setStringProperty(entry.getKey(), entry.getValue());
        }
        producer.send(message);
        return message.getJMSMessageID();
    }

    public Optional<ViewerMessage> getMessageById(String messageId) {

        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker() && StringUtils.isNotBlank(messageId)) {
            try (QueueConnection connection = startConnection()) {
                return getMessageById(messageId, QUEUE_NAME_VIEWER, connection).or(() -> getMessageById(messageId, QUEUE_NAME_PDF, connection));
            } catch (JMSException e) {
                logger.error(e);
            }
        }
        return Optional.empty();
    }

    private Optional<ViewerMessage> getMessageById(String messageId, String queueName, QueueConnection connection) {
        try (QueueSession queueSession = connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
                QueueBrowser browser = queueSession.createBrowser(queueSession.createQueue(queueName), "JMSMessageID='" + messageId + "'");) {
            Enumeration<?> messagesInQueue = browser.getEnumeration();
            if (messagesInQueue.hasMoreElements()) {
                ActiveMQTextMessage queueMessage = (ActiveMQTextMessage) messagesInQueue.nextElement();
                ViewerMessage ticket = ViewerMessage.parseJSON(queueMessage.getText());
                ticket.setMessageId(queueMessage.getJMSMessageID());
                return Optional.of(ticket);
            }
        } catch (JMSException | JacksonException e) {
            logger.error(e);
        }
        return Optional.empty();
    }

    public BrokerService getBroker() {
        return broker;
    }

    /**
     * Check if the queue has been successfully initialized
     * 
     * @return true if the queue is running
     */
    public boolean isQueueRunning() {
        return queueRunning;
    }

    public Map<String, Integer> countMessagesInQueue(String queueName) {
        Map<String, Integer> fastQueueContent = new TreeMap<>();
        try (QueueConnection connection = startConnection();
                QueueSession queueSession = connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
                QueueBrowser browser = queueSession.createBrowser(queueSession.createQueue(queueName));) {

            Enumeration<?> messagesInQueue = browser.getEnumeration();
            while (messagesInQueue.hasMoreElements()) {
                ActiveMQTextMessage queueMessage = (ActiveMQTextMessage) messagesInQueue.nextElement();

                String type = queueMessage.getStringProperty("JMSType");
                if (fastQueueContent.containsKey(type)) {
                    fastQueueContent.put(type, fastQueueContent.get(type) + 1);
                } else {
                    fastQueueContent.put(type, 1);
                }
            }
        } catch (JMSException e) {
            logger.error(e);
        }
        return fastQueueContent;
    }

    public ActiveMQConnection startConnection() throws JMSException {
        ActiveMQConnection connection = getConnection();
        connection.start();
        return connection;
    }

    public ActiveMQConnection getConnection() throws JMSException {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(config.getConnectorURI());
        connectionFactory.setTrustedPackages(Arrays.asList("io.goobi.viewer.managedbeans", "io.goobi.viewer.model.job.mq"));
        ActiveMQConnection connection =
                (ActiveMQConnection) connectionFactory.createConnection(this.config.getUsernameAdmin(), this.config.getPasswordAdmin()); //NOSONAR: Connection is closed in calling methods
        return connection;
    }

    public boolean pauseQueue(String queueName) {
        if (this.broker == null) {
            logger.error("Attempted to pause queue before initializing broker server");
            return false;
        }
        try {
            ObjectName queueViewMBeanName = getQueueViewBeanName(queueName);
            QueueViewMBean mbean =
                    (QueueViewMBean) broker.getManagementContext().newProxyInstance(queueViewMBeanName, QueueViewMBean.class, true);
            mbean.pause();
            return true;
        } catch (MalformedObjectNameException e) {
            logger.error(e);
            return false;
        }

    }

    public boolean resumeQueue(String queueName) {
        if (this.broker == null) {
            logger.error("Attempted to resume queue before initializing broker server");
            return false;
        }
        try {
            ObjectName queueViewMBeanName = getQueueViewBeanName(queueName);
            QueueViewMBean mbean =
                    (QueueViewMBean) broker.getManagementContext().newProxyInstance(queueViewMBeanName, QueueViewMBean.class, true);
            mbean.resume();
            return true;
        } catch (MalformedObjectNameException e) {
            logger.error(e);
            return false;
        }
    }

    public boolean clearQueue(String queueName) {
        if (this.broker == null) {
            logger.error("Attempted to clear queue before initializing broker server");
            return false;
        }
        try {
            ObjectName queueViewMBeanName = getQueueViewBeanName(queueName);
            QueueViewMBean mbean =
                    (QueueViewMBean) broker.getManagementContext().newProxyInstance(queueViewMBeanName, QueueViewMBean.class, true);
            mbean.purge();
            return true;
        } catch (Exception e) {
            logger.error(e);
            return false;
        }
    }

    public List<ViewerMessage> getWaitingMessages(String messageType) {
        List<ViewerMessage> messages = new ArrayList<>();
        String queueName = MessageQueueManager.getQueueForMessageType(messageType);

        try (QueueConnection connection = startConnection();
                QueueSession queueSession = connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
                QueueBrowser browser = queueSession.createBrowser(queueSession.createQueue(queueName), "JMSType = '" + messageType + "'");) {

            Enumeration<?> messagesInQueue = browser.getEnumeration();

            while (messagesInQueue.hasMoreElements() && messages.size() < 100) {
                ActiveMQTextMessage queueMessage = (ActiveMQTextMessage) messagesInQueue.nextElement();
                ViewerMessage ticket = ViewerMessage.parseJSON(queueMessage.getText());
                ticket.setMessageId(queueMessage.getJMSMessageID());
                messages.add(ticket);
            }
        } catch (JMSException | JacksonException e) {
            logger.error(e);
        }
        return messages;
    }

    public boolean deleteMessage(ViewerMessage ticket) {
        try {
            String queueName = getQueueForMessageType(ticket.getTaskName());
            ObjectName queueViewMBeanName = getQueueViewBeanName(queueName);
            QueueViewMBean mbean = (QueueViewMBean) broker.getManagementContext().newProxyInstance(queueViewMBeanName, QueueViewMBean.class, true);
            int removed = mbean.removeMatchingMessages("JMSMessageID='" + ticket.getMessageId() + "'");
            logger.debug("Removed {} messages with id {} from queue", removed, ticket.getMessageId());
            return removed > 0;
        } catch (Exception e) {
            logger.error(e);
            return false;
        }
    }

    public int deleteMessages(String type) {
        try {
            String queueName = getQueueForMessageType(type);
            ObjectName queueViewMBeanName = getQueueViewBeanName(queueName);
            QueueViewMBean mbean = (QueueViewMBean) broker.getManagementContext().newProxyInstance(queueViewMBeanName, QueueViewMBean.class, true);
            int removed = mbean.removeMatchingMessages("JMSType='" + type + "'");
            logger.debug("Removed {} messages of type {} from queue", removed, type);
            return removed;
        } catch (Exception e) {
            logger.error(e);
            return 0;
        }
    }

    private ObjectName getQueueViewBeanName(String queueName) throws MalformedObjectNameException {
        String name = String.format("org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName=%s", queueName);
        return new ObjectName(name);
    }

    public boolean hasConfig() {
        return this.config != null;
    }

}

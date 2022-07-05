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
package io.goobi.viewer.model.statistics.usage;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

/**
 * @author florian
 *
 */
@Entity
@Table(name = "session_statistics")
public class SessionUsageStatistics {
   
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_statistics_id")
    private long id;
    
    @Column(name = "session_id")
    private String sessionId;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "client_ip")
    private String clientIP;
    
    @ElementCollection
    @CollectionTable(name = "session_statistics_record_requests", 
      joinColumns = {@JoinColumn(name = "session_statistics_id", referencedColumnName = "session_statistics_id")})
    @MapKeyColumn(name = "record_identifier")
    @Column(name = "count")
    private Map<String, Long> recordRequests = new HashMap<>();
    

    public SessionUsageStatistics() {
        
    }
    
    public SessionUsageStatistics(String sessionId, String userAgent, String clientIP) {
        this.sessionId = sessionId;
        this.userAgent = userAgent;
        this.clientIP = clientIP;
    }
    
    public SessionUsageStatistics(SessionUsageStatistics orig) {
        this.sessionId = orig.sessionId;
        this.userAgent = orig.userAgent;
        this.clientIP = orig.clientIP;
        this.recordRequests.putAll(orig.recordRequests);
    }

    /**
     * @return the sessionId
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * @return the userAgent
     */
    public String getUserAgent() {
        return userAgent;
    }
    
    /**
     * @return the clientIP
     */
    public String getClientIP() {
        return clientIP;
    }
    
    public long getRecordRequests(S)
}

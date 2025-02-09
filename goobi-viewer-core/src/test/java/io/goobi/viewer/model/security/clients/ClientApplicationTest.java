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
package io.goobi.viewer.model.security.clients;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;

/**
 * @author florian
 *
 */
public class ClientApplicationTest extends AbstractDatabaseEnabledTest {

    IDAO dao;
    
    @Before
    public void setup() throws Exception {
        super.setUp();
        dao = DataManager.getInstance().getDao();
    }
    
    @Test
    public void testSave() throws DAOException {
        ClientApplication client = new ClientApplication();
        client.setClientIdentifier("abcd");
        dao.saveClientApplication(client);
        
        Long id = client.getId();
        assertNotNull(id);
        
        ClientApplication loadedClient = dao.getClientApplication(id);
        assertNotNull(loadedClient);
        assertEquals(loadedClient.getClientIdentifier(), "abcd"); 
        
        LocalDateTime updated = LocalDateTime.of(1, 2, 3, 4, 5);
        
        loadedClient.setDateLastAccess(updated);
        dao.saveClientApplication(loadedClient);
        ClientApplication loadedClient2 = dao.getClientApplication(id);
        
        assertEquals(updated, loadedClient2.getDateLastAccess());
    }

}

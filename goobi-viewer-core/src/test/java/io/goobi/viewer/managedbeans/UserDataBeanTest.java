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

import java.util.List;

import org.apache.poi.xssf.model.Comments;
import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.security.user.User;

public class UserDataBeanTest extends AbstractDatabaseEnabledTest {

    /**
     * @see UserDataBean#getAnnotationCount()
     * @verifies return correct value
     */
    @Test
    public void getAnnotationCount_shouldReturnCorrectValue() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        UserDataBean udb = new UserDataBean();
        UserBean ub = new UserBean();
        ub.setUser(user);
        udb.setBreadcrumbBean(ub);

        Assert.assertEquals(1, udb.getAnnotationCount());
    }

    /**
     * @see UserDataBean#getLatestComments(User,int)
     * @verifies return the latest comments
     */
    @Test
    public void getLatestComments_shouldReturnTheLatestComments() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        UserDataBean udb = new UserDataBean();
        List<Comment> comments = udb.getLatestComments(user, 2);
        Assert.assertNotNull(comments);
        Assert.assertEquals(2, comments.size());
        Assert.assertEquals(Long.valueOf(4), comments.get(0).getId());
        Assert.assertEquals(Long.valueOf(3), comments.get(1).getId());
    }
}

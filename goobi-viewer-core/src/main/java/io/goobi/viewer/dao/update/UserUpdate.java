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
package io.goobi.viewer.dao.update;

import java.sql.SQLException;
import java.util.List;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.security.user.User;

/**
 * @author florian
 *
 */
public class UserUpdate implements IModelUpdate {

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public boolean update(IDAO dao) throws DAOException, SQLException {
        if (dao.columnsExists("users", "use_gravatar")) {
            List<Long> userIds = dao.getNativeQueryResults("SELECT user_id FROM users WHERE use_gravatar=1");
            for (Long userId : userIds) {
                dao.executeUpdate("UPDATE users SET avatar_type='GRAVATAR' WHERE users.user_id=" + userId);
            }
            dao.executeUpdate("ALTER TABLE users DROP COLUMN use_gravatar");
        }

        return true;
    }

}

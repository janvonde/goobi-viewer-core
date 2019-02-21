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
package de.intranda.digiverso.presentation.model.security.authentication;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.security.Role;
import de.intranda.digiverso.presentation.model.security.authentication.model.LitteraAuthenticationResponse;
import de.intranda.digiverso.presentation.model.security.authentication.model.VuAuthenticationRequest;
import de.intranda.digiverso.presentation.model.security.authentication.model.VuAuthenticationResponse;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.model.security.user.UserGroup;

/**
 * External authentication provider for the LITTERA reader authentication api (www.littera.eu).
 * This provider requests requests authentication from the configured url and an 'id' and 'pw' provided as query parameters.
 * The response is a text/xml document containing a root element <Response> with an attribute "authenticationSuccessful" 
 * which is either true or false depending on the validity of the passed query params.
 * If the authentication is successful, an existing viewer user is newly created is required with the nickname of the login id and
 * an email of {id}@nomail.com.
 * The user may still be suspended, given admin rights ect. as any other viewer user
 * 
 * 
 * @author Florian Alpers
 *
 */
public class LitteraProvider extends HttpAuthenticationProvider {

    private static final Logger logger = LoggerFactory.getLogger(LitteraProvider.class);
    protected static final String DEFAULT_EMAIL = "{username}@nomail.com";
    protected static final String TYPE_USER_PASSWORD = "userPassword";
    private static final String QUERY_PARAMETER_ID = "id";
    private static final String QUERY_PARAMETER_PW = "pw";
    
    /**
     * @param name
     * @param url
     * @param image
     */
    public LitteraProvider(String name, String url, String image, long timeoutMillis) {
        super(name, TYPE_USER_PASSWORD, url, image, timeoutMillis);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#logout()
     */
    @Override
    public void logout() throws AuthenticationProviderException {
        //noop
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#login(java.lang.String, java.lang.String)
     */
    @Override
    public CompletableFuture<LoginResult> login(String loginName, String password) throws AuthenticationProviderException {
        try {
        	LitteraAuthenticationResponse response = get(new URI(getUrl()), loginName, password);
            Optional<User> user = getUser(loginName, response);
            LoginResult result = new LoginResult(BeanUtils.getRequest(), BeanUtils.getResponse(), user, !response.isAuthenticationSuccessful());
            return CompletableFuture.completedFuture(result);
        } catch (URISyntaxException e) {
            throw new AuthenticationProviderException("Cannot resolve authentication api url " + getUrl(), e);
        } catch(WebApplicationException e) {
            throw new AuthenticationProviderException("Error requesting authorisation for user " + loginName, e);
        }
    }

	protected LitteraAuthenticationResponse get(URI url, String username, String password) throws WebApplicationException{
        Client client = ClientBuilder.newClient();
        try {            
            client.property(ClientProperties.CONNECT_TIMEOUT, (int)getTimeoutMillis());
            client.property(ClientProperties.READ_TIMEOUT, (int)getTimeoutMillis());
            url = UriBuilder.fromUri(url).queryParam(QUERY_PARAMETER_ID, username).queryParam(QUERY_PARAMETER_PW, password).build();
            WebTarget target = client.target(url);
            LitteraAuthenticationResponse response = target.request().accept(MediaType.TEXT_XML).get(LitteraAuthenticationResponse.class);
            return response;
        } finally {
            client.close();
        }
    }

    /**
     * @param request
     * @param response
     * @return
     * @throws AuthenticationProviderException
     */
    private Optional<User> getUser(String loginName, LitteraAuthenticationResponse response) throws AuthenticationProviderException {

        if (StringUtils.isBlank(loginName) || !response.isAuthenticationSuccessful()) {
            return Optional.empty();
        }

        User user = null;
        try {
        user = DataManager.getInstance().getDao().getUserByNickname(loginName);
        if (user != null) {
            logger.debug("Found user {} via vuFind username '{}'.", user.getId(), loginName);
        }
        // If not found, try email
        if (user == null) {
            user = DataManager.getInstance().getDao().getUserByEmail(loginName);
            if (user != null) {
                logger.debug("Found user {} via vuFind username '{}'.", user.getId(), loginName);
            }
        }
        
        // If still not found, create a new user
        if (user == null) {
            user = new User();
            user.setNickName(loginName);
            user.setActive(true);
            user.setEmail(DEFAULT_EMAIL.replace("{username}",loginName));
            logger.debug("Created new user with nickname " + loginName);
        }
        
        // Add to bean and persist
        if (user.getId() == null) {
            if (!DataManager.getInstance().getDao().addUser(user)) {
                throw new AuthenticationProviderException("Could not add user to DB.");
            }
        } else {
            if (!DataManager.getInstance().getDao().updateUser(user)) {
                throw new AuthenticationProviderException("Could not update user in DB.");
            }
        }
        
        } catch(DAOException  e) {
            throw new AuthenticationProviderException(e);
        }
        return Optional.ofNullable(user);
    }
    
    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#allowsPasswordChange()
     */
    @Override
    public boolean allowsPasswordChange() {
        return false;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#allowsNicknameChange()
     */
    @Override
    public boolean allowsNicknameChange() {
        return false;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider#allowsEmailChange()
     */
    @Override
    public boolean allowsEmailChange() {
        return true;
    }

}

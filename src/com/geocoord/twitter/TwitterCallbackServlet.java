package com.geocoord.twitter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.util.Base64;
import org.apache.thrift.TException;
import org.json.JSONException;
import org.json.JSONObject;

import com.geocoord.server.ServiceFactory;
import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.thrift.data.User;
import com.geocoord.thrift.data.UserAliasRequest;
import com.geocoord.thrift.data.UserAliasResponse;
import com.geocoord.thrift.data.UserCreateRequest;
import com.geocoord.thrift.data.UserCreateResponse;
import com.geocoord.thrift.data.UserRetrieveRequest;
import com.geocoord.thrift.data.UserRetrieveResponse;
import com.geocoord.thrift.data.UserUpdateRequest;
import com.geocoord.thrift.data.UserUpdateResponse;
import com.geocoord.util.CookieUtil;
import com.geocoord.util.CryptoUtil;
import com.geocoord.util.HttpUtil;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.signature.AuthorizationHeaderSigningStrategy;

public class TwitterCallbackServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    Exception error = null;
    
    try {
      //
      // Prepare OAuth parties
      //
      
      OAuthConsumer consumer = new DefaultOAuthConsumer(Twitter.CONSUMER_KEY, Twitter.CONSUMER_SECRET);      
      consumer.setSigningStrategy(new AuthorizationHeaderSigningStrategy());
      OAuthProvider provider = new DefaultOAuthProvider(Twitter.REQUEST_TOKEN_URL, Twitter.ACCESS_TOKEN_URL, Twitter.AUTHORIZE_URL);
      
      //
      // The following MUST be done as we created a new provider (the one
      // used to get the request token had this flag set when Twitter answered).
      //
      // @see http://code.google.com/p/oauth-signpost/wiki/TwitterAndSignpost
      // Comment by talsalmona,  Nov 12, 2009
      //
      provider.setOAuth10a(true);
    
      //
      // Extract token and token secret from Cookie
      //
        
      String secret = new String(CryptoUtil.unpad(CryptoUtil.unwrap(Base64.decode(HttpUtil.getCookieValue(req, "cts").getBytes()))));
        
      //consumer.setTokenWithSecret(token, secret);
        
      consumer.setTokenWithSecret(req.getParameter("oauth_token"), secret);
        
      // user must have granted authorization at this point
      provider.retrieveAccessToken(consumer, req.getParameter("oauth_verifier"));
        
      //
      // Retrieve Twitter name
      //
      
      URL url = new URL(Twitter.API_ACCOUNT_VERIFY_CREDENTIALS_URL);
      HttpURLConnection request = (HttpURLConnection) url.openConnection();

      // sign the request
      consumer.sign(request);
      // send the request
      request.connect();
      
      InputStream is = request.getInputStream();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      
      byte[] buf = new byte[128];
      
      do {
        int res = is.read(buf);
        if (res <= 0) {
          break;
        }
        baos.write(buf, 0, res);
      } while (true);
      
      is.close();
      
      request.disconnect();
      
      //
      // Convert buffer to JSON
      //
      
      String jsonstr = baos.toString();
      JSONObject json = new JSONObject(jsonstr);
      
      //
      // Attempt to retrieve user
      //
          
      // Use the twitter 'id' as the screen name can be changed
      
      UserRetrieveRequest urequest = new UserRetrieveRequest();
      urequest.setIncludeLayers(false);
      
      String alias = "twitter:" + json.getString("id");
      urequest.setAlias(alias);
      
      UserRetrieveResponse uresponse = null;
      
      try {
        uresponse = ServiceFactory.getInstance().getUserService().retrieve(urequest);
      } catch (GeoCoordException gce) {
        // Propagate exception if it's anything else than USER_NOT_FOUND
        if (GeoCoordExceptionCode.USER_NOT_FOUND.equals(gce.getCode())) {
          throw gce;
        }
      }

      //
      // If user was not found, create one
      //

      User user = null;
      boolean create = false;
      
      if (null == uresponse) {
        user = new User();
        create = true;
      } else {
        user = uresponse.getUser();
        create = false;
      }
      
      //
      // Update Twitter related fields
      //

      user.putToTwitterInfo(Constants.TWITTER_ACCOUNT_DETAILS, jsonstr);
      user.putToTwitterInfo(Constants.TWITTER_ID, json.getString("id"));
      user.putToTwitterInfo(Constants.TWITTER_SCREEN_NAME, json.getString("screen_name"));
      user.putToTwitterInfo(Constants.TWITTER_PHOTO_URL, json.getString("profile_image_url"));
      user.putToTwitterInfo(Constants.TWITTER_ACCESS_TOKEN, consumer.getToken());
      user.putToTwitterInfo(Constants.TWITTER_ACCESS_TOKEN_SECRET, consumer.getTokenSecret());
      
      //
      // Store/update user
      //

      if (create) {
        UserCreateRequest ucr = new UserCreateRequest();
        ucr.setUser(user);
        
        UserCreateResponse ucrsp = ServiceFactory.getInstance().getUserService().create(ucr);
        
        user = ucrsp.getUser();
        
        //
        // Record the alias
        //
        
        UserAliasRequest areq = new UserAliasRequest();
        areq.setUserId(user.getUserId());
        areq.setAlias(alias);
        UserAliasResponse aresp = ServiceFactory.getInstance().getUserService().alias(areq);        
      } else {
        UserUpdateRequest uur = new UserUpdateRequest();
        uur.setUser(user);
        
        UserUpdateResponse uursp = ServiceFactory.getInstance().getUserService().update(uur);
        
        user = uursp.getUser();        
      }
      
      //
      // Set Cookie
      //

      resp.addCookie(CookieUtil.getAuthCookie(user));
      
      //
      // Redirect to home page
      //
      
      resp.sendRedirect(Constants.GEOCOORD_HOME_PAGE_URL);
    } catch (TException te) {
      error = te;
    } catch (JSONException jse) {
      error = jse;
    } catch (GeoCoordException gce) {
      error = gce;
    } catch (OAuthMessageSignerException oamse) {
      error = oamse;
    } catch (OAuthCommunicationException oace) {
      error = oace;
    } catch (OAuthExpectationFailedException oaefe) {
      error = oaefe;
    } catch (OAuthNotAuthorizedException oanae) {
      error = oanae;
    } finally {      
      if (null != error) {
        //
        // Reset Cookie
        //
        
        Cookie cookie = new Cookie(Constants.GEOCOORD_AUTH_COOKIE_NAME, "");
        cookie.setMaxAge(0);
        resp.addCookie(cookie);
        resp.sendRedirect(Twitter.ERROR_URL);
        return;
      }
    }    
  }
}

package com.geoxp.twitter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.ParameterStyle;
import net.oauth.client.OAuthClient;
import net.oauth.client.httpclient4.HttpClient4;

import org.apache.catalina.util.Base64;
import org.apache.thrift.TException;
import org.json.JSONException;
import org.json.JSONObject;

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
import com.geoxp.server.ServiceFactory;
import com.geoxp.util.CookieUtil;
import com.geoxp.util.CryptoUtil;
import com.geoxp.util.HttpUtil;
import com.google.inject.Singleton;

@Singleton
public class TwitterCallbackServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    Exception error = null;
    
    try {
      //
      // Prepare OAuth parties
      //
      
      OAuthServiceProvider sp = new OAuthServiceProvider(Twitter.REQUEST_TOKEN_URL, Twitter.AUTHORIZE_URL, Twitter.ACCESS_TOKEN_URL);
      OAuthConsumer consumer = new OAuthConsumer(Twitter.CALLBACK_URL, Twitter.CONSUMER_KEY, Twitter.CONSUMER_SECRET, sp);      
      consumer.setProperty(OAuthClient.PARAMETER_STYLE, ParameterStyle.AUTHORIZATION_HEADER);
      
      //
      // The following MUST be done as we created a new provider (the one
      // used to get the request token had this flag set when Twitter answered).
      //
      // @see http://code.google.com/p/oauth-signpost/wiki/TwitterAndSignpost
      // Comment by talsalmona,  Nov 12, 2009
      //
    
      //
      // Extract token and token secret from Cookie
      //
        
      String secret = new String(CryptoUtil.unpad(CryptoUtil.unwrap(Base64.decode(HttpUtil.getCookieValue(req, "cts").getBytes()))));
        
      //consumer.setTokenWithSecret(token, secret);

      OAuthAccessor accessor = new OAuthAccessor(consumer);
      accessor.requestToken = req.getParameter("oauth_token");
      accessor.tokenSecret = secret;
        
      // user must have granted authorization at this point
      OAuthClient client = new OAuthClient(new HttpClient4());
      client.getAccessToken(accessor, null, null);
      
      //
      // Retrieve Twitter name
      //
      
      URL url = new URL(Twitter.API_ACCOUNT_VERIFY_CREDENTIALS_URL);
      HttpURLConnection request = (HttpURLConnection) url.openConnection();

      OAuthMessage message = new OAuthMessage("GET", Twitter.API_ACCOUNT_VERIFY_CREDENTIALS_URL, null);
      message.addRequiredParameters(accessor);

      OAuthMessage response = client.invoke(message, ParameterStyle.AUTHORIZATION_HEADER);
      
      InputStream is = response.getBodyAsStream();
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
      

      //
      // Convert buffer to JSON
      //
      
      String jsonstr = baos.toString();
      JSONObject json = new JSONObject(jsonstr);
      
      System.out.println(jsonstr);
      
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
        if (!GeoCoordExceptionCode.USER_NOT_FOUND.equals(gce.getCode())) {
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
      user.putToTwitterInfo(Constants.TWITTER_ACCESS_TOKEN, accessor.accessToken);
      user.putToTwitterInfo(Constants.TWITTER_ACCESS_TOKEN_SECRET, accessor.tokenSecret);
      
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
    } catch (URISyntaxException use) {
      error = use;
    } catch (OAuthException oae) {
      error = oae;
    } finally {      
      if (null != error) {
        error.printStackTrace();
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

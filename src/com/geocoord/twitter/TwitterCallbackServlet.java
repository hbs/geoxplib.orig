package com.geocoord.twitter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.util.Base64;
import org.apache.thrift.TException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.json.JSONException;
import org.json.JSONObject;

import com.geocoord.server.ServiceFactory;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.User;
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
import oauth.signpost.signature.SignatureMethod;

public class TwitterCallbackServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    Exception error = null;
    
    try {
      //
      // Prepare OAuth parties
      //
      
      OAuthConsumer consumer = new DefaultOAuthConsumer(Twitter.CONSUMER_KEY, Twitter.CONSUMER_SECRET, SignatureMethod.HMAC_SHA1);
      OAuthProvider provider = new DefaultOAuthProvider(consumer, Twitter.REQUEST_TOKEN_URL, Twitter.ACCESS_TOKEN_URL, Twitter.AUTHORIZE_URL);
      
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
      provider.retrieveAccessToken(req.getParameter("oauth_verifier"));
        
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
      User user = ServiceFactory.getInstance().getUserDAO().load("twitter:" + json.getString("id"));

      //
      // If user was not found, create one
      //

      if (null == user) {
        user = new User();
      }
      
      //
      // Update Twitter related fields
      //

      user.setTwitterAccountDetails(jsonstr);
      user.setTwitterId(json.getString("id"));
      user.setTwitterScreenName(json.getString("screen_name"));
      user.setTwitterPhotoURL(json.getString("profile_image_url"));
      user.setTwitterAccessToken(consumer.getToken());
      user.setTwitterAccessTokenSecret(consumer.getTokenSecret());
      
      //
      // Store/update user
      //
      
      user = ServiceFactory.getInstance().getUserDAO().store(user);
      
      //
      // Set Cookie
      //

      resp.addCookie(CookieUtil.getAuthCookie(user));
      
      //
      // Redirect to home page
      //
      
      resp.sendRedirect("http://www.google.com/");
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
        System.out.println(error);
        resp.sendRedirect(Twitter.ERROR_URL);
        return;
      }
    }    
  }
}

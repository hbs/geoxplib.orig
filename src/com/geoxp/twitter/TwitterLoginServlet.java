package com.geoxp.twitter;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthServiceProvider;
import net.oauth.client.OAuthClient;
import net.oauth.client.httpclient4.HttpClient4;

import org.bouncycastle.util.encoders.Base64;

import com.geoxp.util.CryptoUtil;
import com.google.inject.Singleton;


@Singleton
public class TwitterLoginServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    
    //
    // Prepare OAuth parties
    //
    
    OAuthServiceProvider sp = new OAuthServiceProvider(Twitter.REQUEST_TOKEN_URL, Twitter.AUTHORIZE_URL, Twitter.ACCESS_TOKEN_URL);
    OAuthConsumer consumer = new OAuthConsumer(Twitter.CALLBACK_URL, Twitter.CONSUMER_KEY, Twitter.CONSUMER_SECRET, sp);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    OAuthClient client = new OAuthClient(new HttpClient4());
    
    //
    // Retrieve URL
    //
    
    Exception error = null;
    
    String url = Twitter.ERROR_URL;
    
    try {
      client.getRequestToken(accessor);

      //
      // Set Cookie with (encrypted) token secret so we can get it back in the callback
      //
      
      resp.addCookie(new Cookie("cts", new String(Base64.encode(CryptoUtil.wrap(CryptoUtil.pad(8, accessor.tokenSecret.getBytes()))))));
      
      url = Twitter.AUTHORIZE_URL + "?oauth_token=" + accessor.requestToken;
    } catch (URISyntaxException use) {
      error = use;
    } catch (OAuthException oae) {
      error = oae;
    }
    
    if (null != error) {
      //
      // An error occurred, log the error
      //
      System.out.println(error);
    }
    
    //
    // Redirect to the login or error URL.
    //
    
    resp.sendRedirect(url);
  }
}

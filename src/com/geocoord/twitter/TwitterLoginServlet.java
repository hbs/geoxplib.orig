package com.geocoord.twitter;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bouncycastle.util.encoders.Base64;

import com.geocoord.util.CryptoUtil;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.signature.AuthorizationHeaderSigningStrategy;

public class TwitterLoginServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    
    //
    // Prepare OAuth parties
    //
    
    OAuthConsumer consumer = new DefaultOAuthConsumer(Twitter.CONSUMER_KEY, Twitter.CONSUMER_SECRET);
    consumer.setSigningStrategy(new AuthorizationHeaderSigningStrategy());
    OAuthProvider provider = new DefaultOAuthProvider(Twitter.REQUEST_TOKEN_URL, Twitter.ACCESS_TOKEN_URL, Twitter.AUTHORIZE_URL);

    //
    // Retrieve URL
    //
    
    Exception error = null;
    
    String url = Twitter.ERROR_URL;
    
    try {
      url = provider.retrieveRequestToken(consumer, Twitter.CALLBACK_URL);
      
      //
      // Set Cookie with (encrypted) token secret so we can get it back in the callback
      //
      
      resp.addCookie(new Cookie("cts", new String(Base64.encode(CryptoUtil.wrap(CryptoUtil.pad(8, consumer.getTokenSecret().getBytes()))))));
    } catch (OAuthExpectationFailedException oaefe) {
      error = oaefe;
    } catch (OAuthCommunicationException oace) {
      error = oace;
    } catch (OAuthMessageSignerException oamse) {
      error = oamse;
    } catch (OAuthNotAuthorizedException oanae) {
      error = oanae;
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

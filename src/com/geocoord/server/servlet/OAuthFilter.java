package com.geocoord.server.servlet;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geocoord.server.ServiceFactory;
import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.thrift.data.Layer;
import com.geocoord.thrift.data.LayerRetrieveRequest;
import com.geocoord.thrift.data.LayerRetrieveResponse;
import com.geocoord.thrift.data.User;
import com.geocoord.thrift.data.UserRetrieveRequest;
import com.geocoord.thrift.data.UserRetrieveResponse;
import com.geocoord.util.NamingUtil;
import com.google.inject.Singleton;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.SimpleOAuthValidator;
import net.oauth.server.OAuthServlet;

/**
 * Servlet filter used to validate OAuth signatures.
 * 
 */
@Singleton
public class OAuthFilter implements Filter {
  
  private static final Logger logger = LoggerFactory.getLogger(OAuthFilter.class);
  
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }
  
  @Override
  public void destroy() {
  }
  
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    OAuthMessage message = OAuthServlet.getMessage((HttpServletRequest) request, null);

    //
    // Extract consumer key
    //
    
    String consumerKey = message.getConsumerKey();
    
    //
    // Retrieve either User or Layer, depending on type of key
    //
    
    String consumerSecret = null;
    
           
    // FIXME(hbs): return clean error messages, not raw stack traces
    
    try {
      if (NamingUtil.isUUID(consumerKey)) {
        UserRetrieveRequest ureq = new UserRetrieveRequest();
        ureq.setUserId(consumerKey);

        UserRetrieveResponse uresp = ServiceFactory.getInstance().getUserService().retrieve(ureq);
        User user = uresp.getUser();
        consumerSecret = user.getSecret();
        request.setAttribute(Constants.SERVLET_REQUEST_ATTRIBUTE_CONSUMER, user);
      } else {
        LayerRetrieveRequest lreq = new LayerRetrieveRequest();
        lreq.setLayerId(consumerKey);
        LayerRetrieveResponse lresp = ServiceFactory.getInstance().getLayerService().retrieve(lreq);
        Layer layer = lresp.getLayers().get(0);
        consumerSecret = layer.getSecret();
        request.setAttribute(Constants.SERVLET_REQUEST_ATTRIBUTE_CONSUMER, layer);
      }      
    } catch (TException te) {
      // Throw a ServletException, don't expose te
      throw new ServletException();        
    } catch (GeoCoordException gce) {
      // Throw a ServletException, don't expose gce
      throw new ServletException();
    }
    
    SimpleOAuthValidator validator = new SimpleOAuthValidator();

    OAuthServiceProvider sp = new OAuthServiceProvider("","","");
    OAuthConsumer consumer = new OAuthConsumer(null, consumerKey, consumerSecret, sp);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    
    try {
      message.validateMessage(accessor, validator);
    } catch (URISyntaxException use) {
      logger.error("doFilter", use);
      throw new ServletException(use);
    } catch (OAuthException oae) {
      logger.error("doFilter", oae);
      throw new ServletException(oae.getMessage());
    }
    
    chain.doFilter(request, response);
  }
}

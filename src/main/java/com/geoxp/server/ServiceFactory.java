package com.geoxp.server;

import com.geocoord.thrift.services.ActivityService;
import com.geocoord.thrift.services.AtomService;
import com.geocoord.thrift.services.CentroidService;
import com.geocoord.thrift.services.SearchService;
import com.geocoord.thrift.services.UserService;
import com.geocoord.thrift.services.LayerService;
import com.geoxp.util.CassandraHelper;
import com.geoxp.util.CryptoHelper;
import com.geoxp.util.ThriftHelper;
import com.google.inject.Inject;

public class ServiceFactory {
  private static final ServiceFactory singleton = new ServiceFactory();
  
  private ServiceFactory() {}
  
  public static ServiceFactory getInstance() {    
    return singleton;
  }
  
  private SearchService.Iface searchService = null;
  
  private UserService.Iface userService = null;

  private LayerService.Iface layerService = null;

  private AtomService.Iface atomService = null;

  private ActivityService.Iface activityService = null;

  private CentroidService.Iface centroidService = null;
  
  private CassandraHelper cassandraHelper = null;
  
  private ThriftHelper thriftHelper = null;
  
  private CryptoHelper cryptoHelper = null;
  
  @Inject
  public void injectUserService(UserService.Iface userService) {
    this.userService = userService;
  }
  
  public UserService.Iface getUserService() {
    return this.userService;
  }

  @Inject
  public void injectLayerService(LayerService.Iface layerService) {
    this.layerService = layerService;
  }
  
  public LayerService.Iface getLayerService() {
    return this.layerService;
  }

  @Inject
  public void injectAtomService(AtomService.Iface atomService) {
    this.atomService = atomService;
  }
  
  public AtomService.Iface getAtomService() {
    return this.atomService;
  }

  @Inject
  public void injectActivityService(ActivityService.Iface activityService) {
    this.activityService = activityService;
  }
  
  public ActivityService.Iface getActivityService() {
    return this.activityService;
  }

  @Inject
  public void injectCentroidService(CentroidService.Iface centroidService) {
    this.centroidService = centroidService;
  }
  
  public CentroidService.Iface getCentroidService() {
    return this.centroidService;
  }

  @Inject
  public void injectSearchService(SearchService.Iface searchService) {
    this.searchService = searchService;
  }

  public SearchService.Iface getSearchService() {
    return this.searchService;
  }
  
  @Inject
  public void injectCassandraHelper(CassandraHelper cassandraHelper) {
    this.cassandraHelper = cassandraHelper;
  }
  
  public CassandraHelper getCassandraHelper() {
    return this.cassandraHelper;
  }
  
  @Inject
  public void injectThriftHelper(ThriftHelper thriftHelper) {
    this.thriftHelper = thriftHelper;
  }
  
  public ThriftHelper getThriftHelper() {
    return this.thriftHelper;    
  }
  
  @Inject
  public void injectCryptoHelper(CryptoHelper cryptoHelper) {
    this.cryptoHelper = cryptoHelper;
  }
  
  public CryptoHelper getCryptoHelper() {
    return this.cryptoHelper;
  }
}

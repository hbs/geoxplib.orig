package com.geoxp.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.geocoord.thrift.services.ActivityService;
import com.geocoord.thrift.services.AtomService;
import com.geocoord.thrift.services.CentroidService;
import com.geocoord.thrift.services.LayerService;
import com.geocoord.thrift.services.SearchService;
import com.geocoord.thrift.services.UserService;
import com.geoxp.geo.GeoNamesLuceneImpl;
import com.geoxp.lucene.IndexManager;
import com.geoxp.server.service.ActivityServiceLuceneIndexer;
import com.geoxp.server.service.ActivityServiceMock;
import com.geoxp.server.service.ActivityServiceSequenceFileImpl;
import com.geoxp.server.service.ActivityServiceWrapper;
import com.geoxp.server.service.AtomServiceCassandraImpl;
import com.geoxp.server.service.CassandraHelperDummyImpl;
import com.geoxp.server.service.CentroidServiceMock;
import com.geoxp.server.service.LayerServiceCassandraImpl;
import com.geoxp.server.service.SearchServiceLuceneImpl;
import com.geoxp.server.service.UserServiceCassandraImpl;
import com.geoxp.util.CassandraHelper;
import com.geoxp.util.CryptoHelper;
import com.geoxp.util.CryptoHelperImpl;
import com.geoxp.util.ThriftHelper;
import com.geoxp.util.ThriftHelperImpl;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Scopes;

public class GuiceModule extends AbstractModule {

  @BindingAnnotation @Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD }) @Retention(RetentionPolicy.RUNTIME)
  public @interface ActivityStorage {}

  @BindingAnnotation @Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD }) @Retention(RetentionPolicy.RUNTIME)
  public @interface ActivityIndexing {}

  //@Override
  protected void configure() {
    
    bind(IndexManager.class).in(Scopes.SINGLETON);
    
    bind(ActivityService.Iface.class).annotatedWith(ActivityStorage.class).to(ActivityServiceSequenceFileImpl.class).in(Scopes.SINGLETON);
    bind(ActivityService.Iface.class).annotatedWith(ActivityIndexing.class).to(ActivityServiceLuceneIndexer.class).in(Scopes.SINGLETON);
    bind(ActivityService.Iface.class).to(ActivityServiceWrapper.class).in(Scopes.SINGLETON);
    
    bind(UserService.Iface.class).to(UserServiceCassandraImpl.class).in(Scopes.SINGLETON);
    bind(LayerService.Iface.class).to(LayerServiceCassandraImpl.class).in(Scopes.SINGLETON);
    bind(AtomService.Iface.class).to(AtomServiceCassandraImpl.class).in(Scopes.SINGLETON);
    bind(SearchService.Iface.class).to(SearchServiceLuceneImpl.class).in(Scopes.SINGLETON);
        
    //bind(ActivityService.Iface.class).to(ActivityServiceMock.class).in(Scopes.SINGLETON);
    
    //bind(CentroidService.Iface.class).to(GeoNamesLuceneImpl.class).in(Scopes.SINGLETON);
    bind(CentroidService.Iface.class).to(CentroidServiceMock.class).in(Scopes.SINGLETON);
    
    bind(CassandraHelper.class).to(CassandraHelperDummyImpl.class).in(Scopes.SINGLETON);
    bind(ThriftHelper.class).to(ThriftHelperImpl.class).in(Scopes.SINGLETON);
    bind(CryptoHelper.class).to(CryptoHelperImpl.class).in(Scopes.SINGLETON);
  }
}

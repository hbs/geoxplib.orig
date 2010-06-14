package com.geocoord.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.geocoord.geo.GeoNamesLuceneImpl;
import com.geocoord.lucene.IndexManager;
import com.geocoord.server.service.ActivityServiceLuceneIndexer;
import com.geocoord.server.service.ActivityServiceMock;
import com.geocoord.server.service.ActivityServiceSequenceFileImpl;
import com.geocoord.server.service.ActivityServiceWrapper;
import com.geocoord.server.service.AtomServiceCassandraImpl;
import com.geocoord.server.service.CassandraHelperDummyImpl;
import com.geocoord.server.service.CentroidServiceMock;
import com.geocoord.server.service.LayerServiceCassandraImpl;
import com.geocoord.server.service.SearchServiceLuceneImpl;
import com.geocoord.server.service.UserServiceCassandraImpl;
import com.geocoord.thrift.services.ActivityService;
import com.geocoord.thrift.services.AtomService;
import com.geocoord.thrift.services.CentroidService;
import com.geocoord.thrift.services.LayerService;
import com.geocoord.thrift.services.SearchService;
import com.geocoord.thrift.services.UserService;
import com.geocoord.util.CassandraHelper;
import com.geocoord.util.CryptoHelper;
import com.geocoord.util.CryptoHelperImpl;
import com.geocoord.util.ThriftHelper;
import com.geocoord.util.ThriftHelperImpl;
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

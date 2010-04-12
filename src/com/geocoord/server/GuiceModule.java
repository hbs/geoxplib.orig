package com.geocoord.server;

import com.geocoord.geo.GeoNamesLuceneImpl;
import com.geocoord.server.service.ActivityServiceMock;
import com.geocoord.server.service.ActivityServiceSequenceFileImpl;
import com.geocoord.server.service.AtomServiceCassandraImpl;
import com.geocoord.server.service.CassandraHelperDummyImpl;
import com.geocoord.server.service.CentroidServiceMock;
import com.geocoord.server.service.LayerServiceCassandraImpl;
import com.geocoord.server.service.UserServiceCassandraImpl;
import com.geocoord.thrift.services.ActivityService;
import com.geocoord.thrift.services.AtomService;
import com.geocoord.thrift.services.CentroidService;
import com.geocoord.thrift.services.LayerService;
import com.geocoord.thrift.services.UserService;
import com.geocoord.util.CassandraHelper;
import com.geocoord.util.CryptoHelper;
import com.geocoord.util.CryptoHelperImpl;
import com.geocoord.util.ThriftHelper;
import com.geocoord.util.ThriftHelperImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class GuiceModule extends AbstractModule {

  //@Override
  protected void configure() {
    
    bind(UserService.Iface.class).to(UserServiceCassandraImpl.class).in(Scopes.SINGLETON);
    bind(LayerService.Iface.class).to(LayerServiceCassandraImpl.class).in(Scopes.SINGLETON);
    bind(AtomService.Iface.class).to(AtomServiceCassandraImpl.class).in(Scopes.SINGLETON);
    //bind(ActivityService.Iface.class).to(ActivityServiceMock.class).in(Scopes.SINGLETON);
    bind(ActivityService.Iface.class).to(ActivityServiceSequenceFileImpl.class).in(Scopes.SINGLETON);
    
    //bind(CentroidService.Iface.class).to(GeoNamesLuceneImpl.class).in(Scopes.SINGLETON);
    bind(CentroidService.Iface.class).to(CentroidServiceMock.class).in(Scopes.SINGLETON);
    
    bind(CassandraHelper.class).to(CassandraHelperDummyImpl.class).in(Scopes.SINGLETON);
    bind(ThriftHelper.class).to(ThriftHelperImpl.class).in(Scopes.SINGLETON);
    bind(CryptoHelper.class).to(CryptoHelperImpl.class).in(Scopes.SINGLETON);
  }
}

package com.geocoord.server;

import com.geocoord.server.dao.LayerServiceImpl;
import com.geocoord.server.dao.PointServiceImpl;
import com.geocoord.server.dao.UserDAOImpl;
import com.geocoord.thrift.services.PointService;
import com.geocoord.thrift.services.UserService;
import com.geocoord.thrift.services.LayerService;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class GuiceModule extends AbstractModule {
  //@Override
  protected void configure() {
    bind(UserService.Iface.class).to(UserDAOImpl.class).in(Scopes.SINGLETON);
    bind(LayerService.Iface.class).to(LayerServiceImpl.class).in(Scopes.SINGLETON);
    bind(PointService.Iface.class).to(PointServiceImpl.class).in(Scopes.SINGLETON);
  }
}

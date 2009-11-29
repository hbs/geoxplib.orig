package com.geocoord.server;

import com.geocoord.server.dao.UserDAOImpl;
import com.geocoord.thrift.services.UserService;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class GuiceModule extends AbstractModule {
  //@Override
  protected void configure() {
    //bind(ThriftService.Iface.class).to(ThriftServiceImpl.class).in(Scopes.SINGLETON);
    bind(UserService.Iface.class).to(UserDAOImpl.class).in(Scopes.SINGLETON);
  }
}

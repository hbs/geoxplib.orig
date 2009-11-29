CREATE DATABASE geocoord;
USE geocoord;

CREATE TABLE users (
  -- FNV of user uuid
  gcuid BIGINT(20) NOT NULL,
  thrift MEDIUMBLOB NOT NULL,
  PRIMARY KEY(gcuid)
) Engine=InnoDB;

CREATE TABLE userrefs (
  -- FNV of user ref (e.g. twitter:twittername)
  gcuref BIGINT(20) NOT NULL,
  -- gcuid from 'users' table
  gcuid BIGINT(20) NOT NULL,
  -- We could do without PRIMARY KEY but we'll have to deal with potential FNV collisions
  PRIMARY KEY(gcuref)
) Engine=InnoDB;

package com.geocoord.util;

import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TCompactProtocol;
import org.bouncycastle.util.encoders.Hex;

import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;

public class ThriftUtil {
  public static final byte[] serialize(TBase obj) throws TException {
    TSerializer serializer = new TSerializer(new TCompactProtocol.Factory());
    return serializer.serialize(obj);
  }
  
  public static final String serializeHex(TBase obj) throws TException {
    return new String(Hex.encode(serialize(obj)));
  }
  
  public static TBase deserialize(Class<? extends TBase> cls, byte[] data) throws GeoCoordException,TException {
    
    try {
      TBase base = cls.newInstance();
      
      TDeserializer deserializer = new TDeserializer(new TCompactProtocol.Factory());
      deserializer.deserialize(base, data);
      
      return base;      
    } catch (IllegalAccessException iae) {
      throw new GeoCoordException(GeoCoordExceptionCode.CLASS_ERROR);
    } catch (InstantiationException ie) {
      throw new GeoCoordException(GeoCoordExceptionCode.CLASS_ERROR);      
    }
  }
}

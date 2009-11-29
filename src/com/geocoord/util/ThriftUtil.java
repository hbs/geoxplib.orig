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
  public static final byte[] serialize(TBase obj) throws GeoCoordException {
    try {
      TSerializer serializer = new TSerializer(new TCompactProtocol.Factory());
      return serializer.serialize(obj);
    } catch (TException te) {
      throw new GeoCoordException(GeoCoordExceptionCode.THRIFT_ERROR);
    }
  }
  
  public static final String serializeHex(TBase obj) throws GeoCoordException {
    return new String(Hex.encode(serialize(obj)));
  }
  
  public static TBase deserialize(Class<? extends TBase> cls, byte[] data) throws GeoCoordException {
    
    try {
      TBase base = cls.newInstance();
      
      TDeserializer deserializer = new TDeserializer(new TCompactProtocol.Factory());
      deserializer.deserialize(base, data);
      
      return base;  
    } catch (TException te) {
      throw new GeoCoordException(GeoCoordExceptionCode.THRIFT_ERROR);
    } catch (IllegalAccessException iae) {
      throw new GeoCoordException(GeoCoordExceptionCode.CLASS_ERROR);
    } catch (InstantiationException ie) {
      throw new GeoCoordException(GeoCoordExceptionCode.CLASS_ERROR);      
    }
  }
}

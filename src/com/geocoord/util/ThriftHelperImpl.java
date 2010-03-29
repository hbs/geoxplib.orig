package com.geocoord.util;

import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TFieldIdEnum;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TCompactProtocol;

public class ThriftHelperImpl implements ThriftHelper {
  
  // FIXME(hbs): how could we reuse the protocol factories (are they MT safe?)
  
  @Override
  public TBase<? extends TFieldIdEnum> deserialize(TBase<? extends TFieldIdEnum> o, byte[] data) throws TException {
    TDeserializer deserializer = new TDeserializer(new TCompactProtocol.Factory());
    deserializer.deserialize(o, data);
    return o;
  }
  
  @Override
  public byte[] serialize(TBase<? extends TFieldIdEnum> o) throws TException {
    TSerializer serializer = new TSerializer(new TCompactProtocol.Factory());
    return serializer.serialize(o);
  }
}

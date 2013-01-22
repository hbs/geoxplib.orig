package com.geoxp.util;

import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.TFieldIdEnum;

public interface ThriftHelper {
  public byte[] serialize(TBase<? extends TFieldIdEnum> o) throws TException;
  public TBase<? extends TFieldIdEnum> deserialize(TBase<? extends TFieldIdEnum> o, byte[] data) throws TException;
}

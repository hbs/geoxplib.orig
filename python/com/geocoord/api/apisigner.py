# -*- coding: utf8 -*-

import hmac
import hashlib
import urllib

class APISigner:

  def __init__(self,key):
    """Create a new instance of APISigner with the provided key."""
    self.key = key
    
  def _percentEncodeRfc3986(self, str):
    unreserved = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._~'
    encoded = ''
    for c in str.encode('utf8'):
      if c in unreserved:
        encoded += c
      else:
        encoded += '%%%2.0X' % ord(c)
    return encoded
    
  def sign(self, uri, params, method='POST'):
    """Compute the signature for the given method, uri and params
    according to the GeoCoord API rules."""
    
    # Order the parameters
    parampairs = []
    
    for name in params.keys():
      if 'sig' == name:
        continue
      for value in params[name]:
        parampairs.append('%s=%s' % (self._percentEncodeRfc3986(unicode(name)), self._percentEncodeRfc3986(unicode(value))))
      
    parampairs.sort()
    
    data = method
    data += '\r\n'
    data += uri
    data += '\r\n'
    
    for param in parampairs:
      data += param
      data += '\r\n'
      
    mac = hmac.HMAC(msg=data,digestmod=hashlib.sha256,key=self.key)
        
    return mac.hexdigest()
    
    
if __name__ == '__main__':
  signer = APISigner('0000000000000000000000000000000000000000000000000000000000000000'.decode('hex'))
  print signer.sign(method='POST', uri='/foo/bar', params={ u'à':['val1','val2'], 'b':u'à', 'sig':'gisà' })
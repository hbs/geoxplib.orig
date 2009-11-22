import urllib
import httplib
import time
import com.geocoord.api.apisigner

class API:
  """Class designed to interact with the GeoCoord API."""
  
  _api_endpoint = 'localhost:8888'
  
  def __init__(self,apisigner,gcuid):
    self._apisigner = apisigner
    self._gcuid = gcuid
    
  def _issueRequest(self, method, uri, params):
    """Issue an HTTP request."""
    
    headers = {'X-GeoCoord-Client':'Python-API',
               'Content-type': 'application/x-www-form-urlencoded' }
    
    conn = httplib.HTTPConnection(self._api_endpoint)
    conn.request(method, uri, urllib.urlencode(params), headers)
    response = conn.getresponse()
    status = response.status
    reason = response.reason
    body = response.read()
    conn.close()
    return (status,reason,body)
    
  def createLayer(self,name,publicLayer=True):
    """Create a layer.
    
    @type name: string
    @param name: Name to give to the layer.
    @type publicLayer: boolean
    @param publicLayer: Flag indicating whether or not the layer is public
    @rtype: string
    @return The id of the layer"""

    method = 'POST'
    uri = '/api/layer/create'
    # Build params
    params = {'id': self._gcuid,
              'ts': long(1000*time.time()),
              'name': name}
    if not publicLayer:
      params['privacy'] = 'public'
    else:
      params['privacy'] = 'private'
              
    # Compute signature
    sig = self._apisigner.sign(uri, params, method)
    params['sig'] = sig
    
    (status,reason,body) = self._issueRequest(method,uri,params)
    
    print status,reason,body
    
    
if __name__ == '__main__':
  apisigner = com.geocoord.api.apisigner.APISigner('4218ff78c075f0690d5696dad4bcbd1e60350a5a76f150e63495399b3e35345f'.decode('hex'))
  api = API(apisigner,'6c135bb3-0d0d-4eee-bf50-53ed94bfc791')

  api.createLayer('test3',True)
  
import oauth2 as oauth
import json

import httplib2

class GeoXP(object):
  """GeoXP API Client. Initialize with an OAuth key and OAuth secret,
either user or layer based."""

  oauthKey = None
  oauthSecret = None
 
  GEOXP_LAYER_CREATE_ENDPOINT = 'http://api.geoxp.com/api/v0/layer/create'
  GEOXP_LAYER_RETRIEVE_ENDPOINT = 'http://api.geoxp.com/api/v0/layer/retrieve'
  GEOXP_LAYER_UPDATE_ENDPOINT = 'http://api.geoxp.com/api/v0/layer/update'

  GEOXP_ATOM_STORE_ENDPOINT = 'http://api.geoxp.com/api/v0/atom/store'
  GEOXP_ATOM_RETRIEVE_ENDPOINT = 'http://api.geoxp.com/api/v0/atom/retrieve'

  GEOXP_SEARCH_ATOMS_ENDPOINT = 'http://api.geoxp.com/api/v0/search/atoms'

  def __init__(self,oauthKey,oauthSecret):
    self.oauthKey = oauthKey
    self.oauthSecret = oauthSecret
    
  def layerCreate(self,layer):
    params = {}
    params['layer'] = json.dumps(layer)
    return self.oauthPost(self.GEOXP_LAYER_CREATE_ENDPOINT, params)

  def layerRetrieve(self,layerName):
    params = {}
    params['name'] = layerName
    return self.oauthPost(self.GEOXP_LAYER_RETRIEVE_ENDPOINT, params)

  def layerUpdate(self,layer):
    params = {}
    params['layer'] = json.dumps(layer)
    return self.oauthPost(self.GEOXP_LAYER_UPDATE_ENDPOINT, params)

  def atomStore(self,layerId,atomType,atom):
    params = {}
    params['type'] = atomType
    params['atom'] = json.dumps(atom)
    params['layer'] = layerId
    return self.oauthPost(self.GEOXP_ATOM_STORE_ENDPOINT, params)

  def atomRetrieve(self,layer,atom):
    params = {}
    params['atom'] = atom
    params['layer'] = layer
    return self.oauthPost(self.GEOXP_ATOM_RETRIEVE_ENDPOINT, params)

  def searchAtoms(self,query):
    params = {}
    params['q'] = json.dumps(query)
    return self.oauthPost(self.GEOXP_SEARCH_ATOMS_ENDPOINT, params)

  def oauthPost(self,url,params):
    params['oauth_version'] = '1.0' 
    params['oauth_signature_method'] = 'HMAC-SHA1' 
    params['oauth_nonce'] = oauth.Request.make_nonce()
    params['oauth_timestamp'] = oauth.Request.make_timestamp()

    consumer = oauth.Consumer(key=self.oauthKey, secret=self.oauthSecret)
    req = oauth.Request("POST", url, parameters=params)
    req.sign_request(signature_method=oauth.SignatureMethod_HMAC_SHA1(),consumer=consumer,token=None)
    http = httplib2.Http()
    headers = {'Content-Type':'application/x-www-form-urlencoded',
               'User-Agent':'GeoXP Python API' }
    (response,content) = http.request(uri=url, body=req.to_postdata(), method='POST', headers=headers)
    return (response, content)

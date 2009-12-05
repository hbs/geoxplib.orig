package com.geocoord.server.api;

import com.geocoord.server.ServiceFactory;
import com.geocoord.thrift.data.LayerAdminRequest;
import com.geocoord.thrift.data.LayerAdminRequestType;
import com.geocoord.thrift.data.User;
import com.geocoord.thrift.data.LayerAdminResponse;
import com.geocoord.thrift.services.UserService;
import com.geocoord.thrift.services.LayerService;

public class LayerApiTestCase extends ApiTestCase {

	public void testServices() throws Exception {
		final UserService.Iface userService = ServiceFactory.getInstance().getUserService();		
		final LayerService.Iface layerService = ServiceFactory.getInstance().getLayerService();
		
		//
		// create & reload user
		//
		
		User user = new User();
		user.setTwitterId("jeanbon");
		user.setTwitterScreenName("Jean Bon");
		userService.store(user);
		
		user = userService.load("twitter:jeanbon");
		assertEquals("twitter screen name", "Jean Bon", user.getTwitterScreenName());
		
		//
		// create a layer, and count layers in between
		//

		LayerAdminRequest request = new LayerAdminRequest();
		request.setGcuid(user.getGcuid());
		request.setType(LayerAdminRequestType.COUNT);
		LayerAdminResponse response = layerService.admin(request);
		assertEquals("user should have no layer", 0, response.getCount());
		
		request = new LayerAdminRequest();
		request.setType(LayerAdminRequestType.CREATE);
		request.setGcuid(user.getGcuid());
		request.setName("myprivatelayer");
		request.setPublicLayer(false);
		response = layerService.admin(request);
		assertNotNull(response.getGclid());
		assertFalse("layer", "".equals(response.getGclid()));
		
		request = new LayerAdminRequest();
		request.setGcuid(user.getGcuid());
		request.setType(LayerAdminRequestType.COUNT);
		response = layerService.admin(request);
		assertEquals("user should have a single layer", 1, response.getCount());
		

	}
}

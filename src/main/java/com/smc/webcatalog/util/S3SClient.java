package com.smc.webcatalog.util;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

public class S3SClient {

	private static Client client;

	private S3SClient() {}

	public static Client getInstance() {
		if(client==null) {
			client = ClientBuilder.newClient();
		}
		return  client;
	}

}

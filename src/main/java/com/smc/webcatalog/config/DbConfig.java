package com.smc.webcatalog.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;


@Configuration
public class DbConfig  {

	@Autowired
	Environment env;

	@Bean 
	public MongoClient mongoClient() {
		String host = env.getProperty("smc.webcatalog.mongodb.host");
		return MongoClients.create("mongodb://"+host+"/");
	}

	@Bean 
	public MongoTemplate mongoTemplate() {
	     return new MongoTemplate(mongoClient(), "smc-webcatalog");
	}
}

package com.smc;

import java.util.concurrent.Executor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication(scanBasePackages={"com.smc.webcatalog", "com.smc.discontinued",
		"com.smc.psitem", "com.smc.omlist", "com.smc.s3s", "com.smc.cad3d", "com.smc.languageSwitching"},
		exclude = MongoAutoConfiguration.class)
@Slf4j
@EnableAsync(proxyTargetClass = true)
public class SmcWebApplication extends SpringBootServletInitializer{

	public static void main(String[] args) {
// GSLBからロードバランサに変更。コメントアウト。
//		Security.setProperty("networkaddress.cache.ttl", "0");
//	    Security.setProperty("networkaddress.cache.negative.ttl", "0");
		System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
		SpringApplication.run(SmcWebApplication.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
	   return application.sources(SmcWebApplication.class);
	}

	@Bean("Thread2") // ここで設定した"Thread2"を＠Asyncに設定するとその設定が利用される
    public Executor taskExecutor2() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1); // デフォルトのThreadのサイズ。あふれるとQueueCapacityのサイズまでキューイングする
        executor.setQueueCapacity(1); // 待ちのキューのサイズ。あふれるとMaxPoolSizeまでThreadを増やす
        executor.setMaxPoolSize(1); // どこまでThreadを増やすかの設定。この値からあふれるとその処理はリジェクトされてExceptionが発生する
        executor.setThreadNamePrefix("Thread2--");
        executor.initialize();
        return executor;
    }

}

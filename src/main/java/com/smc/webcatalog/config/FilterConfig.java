package com.smc.webcatalog.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.smc.webcatalog.filter.LoggingFilter;

@Configuration
public class FilterConfig {

	@Bean
	public FilterRegistrationBean loggintFilter() {
		// FilterをnewしてFilterRegistrationBeanのコンストラクタに渡す
		FilterRegistrationBean bean = new FilterRegistrationBean(new LoggingFilter());
		// Filterのurl-patternを指定（可変長引数なので複数指定可能）
		bean.addUrlPatterns(new String[] { "/", "/login/*" });
		// Filterの実行順序。
		bean.setOrder(1);
		return bean;
	}

}

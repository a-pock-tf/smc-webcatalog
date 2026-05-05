package com.smc.webcatalog.config;

import java.util.List;
import java.util.Locale;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.util.UrlPathHelper;

import com.smc.util.ModelMapperFactory;
import com.smc.webcatalog.dao.NarrowDownColumnRepository;
import com.smc.webcatalog.dao.TemplateCategoryRepository;
import com.smc.webcatalog.dao.TemplateRepository;
import com.smc.webcatalog.model.NarrowDownColumn;
import com.smc.webcatalog.model.Template;
import com.smc.webcatalog.model.TemplateCategory;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Autowired
	TemplateRepository templateRepo;
	
	@Autowired
	TemplateCategoryRepository templateCategoryRepo;

	@Autowired
	NarrowDownColumnRepository narrowDownColumnRepo;

	/**
	 * ModelMapperの登録 Autowiredで使う
	 * @return
	 */
	@Bean
	public ModelMapper modelMapper() {
		return ModelMapperFactory.create();
	}

	/*
	 * Creating shared instance of singleton bean 'mongo'
	 * 127.0.0.1 に接続に行っていた。使って無かったので、コメントアウト 2025/1/18
	public @Bean MongoClientFactoryBean mongo() {
		MongoClientFactoryBean mongo = new MongoClientFactoryBean();
		return mongo;
	}*/

	/**
	 * VelidationMessagesのUTF-8化(ymlではだめだった)
	 */
	@Override
	public Validator getValidator() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		// メッセージファイルを読込むための設定を記載します
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		// 「setBasename」を使用することで任意のファイル名に変更することも可能です
		messageSource.setBasename("classpath:ValidationMessages");
		// 「setDefaultEncoding」を使用することで任意の文字コードに変更することも可能です
		messageSource.setDefaultEncoding("UTF-8");
		messageSource.setFallbackToSystemLocale(false);
		validator.setValidationMessageSource(messageSource);
		return validator;
	}

	@Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        UrlPathHelper urlPathHelper = new UrlPathHelper();
        urlPathHelper.setUrlDecode(false);
        configurer.setUrlPathHelper(urlPathHelper);
    }

	@Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/s3s/**")
                .allowedOrigins("http://192.168.0.*")
                .allowedOrigins("http://153.120.135.17")
                .allowedOrigins("https://153.120.135.17")

                .allowedOrigins("http://localhost:8080")
                .allowedOrigins("http://dev1.smcworld.com")
                .allowedOrigins("http://ap1.smcworld.com")
                .allowedOrigins("http://ap2.smcworld.com")
                .allowedOrigins("https://ap1admin.smcworld.com")
                .allowedOrigins("http://webcatalog.smcworld.com")
                .allowedOrigins("http://3sapi.smcworld.com")

                .allowedOrigins("https://cdn.smcworld.com")
                .allowedOrigins("https://test.smcworld.com")
                .allowedOrigins("https://www.smcworld.com")
                .allowedOrigins("https://3sapi.smcworld.com")
                .allowedOrigins("https://www.smc.com.cn")
                .allowedMethods("*").allowedHeaders("*");


    }


	@Bean
	public SessionLocaleResolver localeResolver() {
		SessionLocaleResolver r = new SessionLocaleResolver();
		r.setDefaultLocale(Locale.JAPAN);
		return r;
	}

	@Bean
	public LocaleChangeInterceptor localeChangeInterceptor() {
		LocaleChangeInterceptor i = new LocaleChangeInterceptor();
		//パラメーター locale = の値で、Locale変更
		i.setParamName("locale");
		return i;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(localeChangeInterceptor());
	}

    @Bean
    public ReloadableResourceBundleMessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
            messageSource.setBasename("classpath:messages");
            messageSource.setDefaultEncoding("UTF-8");
            messageSource.setFallbackToSystemLocale(false);
            return messageSource;
    }
    
    /**------myBeans-------**/
    @Bean(name = "templates")
	public List<Template> createTemplate() {
    	List<Template> templates = templateRepo.findAll();
		return templates;
	}

    @Bean(name = "templateCategories")
	public List<TemplateCategory> createTemplateCategories() {
    	List<TemplateCategory> templateCategories = templateCategoryRepo.findAll();
		return templateCategories;
	}
    
    @Bean(name = "narrowDownColumns")
	List<NarrowDownColumn> createNarrowDownColumns() {
    	List<NarrowDownColumn> narrowDownColumns = narrowDownColumnRepo.findAll();
		return narrowDownColumns;
    }

}

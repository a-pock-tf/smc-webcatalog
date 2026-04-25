package com.smc.webcatalog.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.smc.webcatalog.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	@Autowired
	CustomUserDetailsService userDetailsService;


	/**
     * 静的ファイルには認証をかけない
     * @param web
     * @throws Exception
     */
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/ja/**", "/en/**", "/zh/**", "/ja-jp/**","/en-**/**","/zh-**/**", "/api/**", "/2dcad/**", "/3dcad/**",
    	"/s3s/**","/THREES/**", "/discontinued/**", "/discon_closed8811/**",  "/discontinued/api/**",
    	"/favicon.ico", "/css/**","/**/**.css", "/js/**","/**/**.js", "/images/**", "/fonts/**", "/semantic2.4.1/themes/**" /* for Demo */);
   }
    @Bean
    public UserDetailsService mongoUserDetails() {
        return new CustomUserDetailsService();
    }
    @Bean
	public PasswordEncoder getPasswordEncoder() {
	    return new BCryptPasswordEncoder();
	}
    @Override
    protected void configure(HttpSecurity http) throws Exception {
    	http.authorizeRequests()
                .antMatchers("/login").permitAll()//ログイン処理は許可
                .antMatchers("/login/").permitAll()//ログイン処理は許可
                .antMatchers("/login/logout/**").permitAll()//ログアウト処理は許可
                .antMatchers("/logout**").permitAll()//ログアウト処理は許可
                .antMatchers("/error**").permitAll()//エラー処理は許可
                .antMatchers("/login/user/**").permitAll()
                .antMatchers("/login/index").permitAll()//ログインフォームは許可
            .and()
                .authorizeRequests().antMatchers("/").permitAll() //ログイン以外フォームは許可

            .and()
                .authorizeRequests().antMatchers("/login/**").authenticated() // /login/配下のすべては認証必須
            .and()
                //.anyRequest().authenticated()// それ以外は全て認証無しの場合アクセス不許可
//            	.authorizeRequests().anyRequest().anonymous()// それ以外は全て認証無し許可
                .authorizeRequests().anyRequest().permitAll()// それ以外は全て認証無し許可


            .and() // 不許可の場合、ログインフォームへ
                .formLogin()
                .loginProcessingUrl("/login/")//ログイン処理をするURL
                .loginPage("/login/")//ログイン画面のURL
                .failureHandler((req, res, exp) -> {
                	// Redirectなので/productsから書く必要あり。
                    res.sendRedirect("/webcatalog/login/index?error=true&loginid=" + req.getParameter("loginid") + "&loginpw=" + req.getParameter("loginpw"));
                })//認証失敗時のURL
                .defaultSuccessUrl("/login/loginSuccess", true)//認証成功時のURL

                .usernameParameter("loginid")//ユーザのパラメータ名
                .passwordParameter("loginpw")//パスワードのパラメータ名
            .and()
                .logout()
                .logoutUrl("/login/logout/")//ログアウト時のURL
                .logoutRequestMatcher(new AntPathRequestMatcher("/login/logout/**"))
                .logoutSuccessUrl("/login/") //ログアウト成功時のURL
                .invalidateHttpSession(false)
                .permitAll()
            .and()
                .sessionManagement()
                .invalidSessionUrl("/login/index?sessionerror=true") // セッションが無効な時の遷移先
            .and()
                // .headers().frameOptions()はどちらも変わらず。
                .headers().frameOptions().sameOrigin() // X-Frame-Options
            //	.headers().frameOptions().disable() // X-Frame-Options

                // .httpStrictTransportSecurity()はどちらも変わらず。
            //	.httpStrictTransportSecurity().disable();
                .httpStrictTransportSecurity().includeSubDomains(true);

    	  // 2025/8/21 cache-control: privateが出てしまうので、追加
    	  http.headers().cacheControl().disable();
    	  
          http.headers().xssProtection().block(false);
//          http.headers().disable();

        http.csrf().disable();


     // if Spring MVC is on classpath and no CorsConfigurationSource is provided,
        // Spring Security will use CORS configuration provided to Spring MVC
        http.cors().configurationSource(this.corsConfigurationSource());

        http.authorizeHttpRequests(auth -> auth.mvcMatchers("/").permitAll());

    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        UserDetailsService userDetailsService = mongoUserDetails();
        auth
            .userDetailsService(userDetailsService)
            .passwordEncoder(getPasswordEncoder());

    }

    // CORS設定を行うBean定義
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
    	CorsConfiguration configuration = new CorsConfiguration();

        // Access-Control-Allow-Origin
        configuration.setAllowedOrigins(
        		Arrays.asList(
        				"http://localhost:8080","http://localhost:8081","http://127.0.0.1:5173",
        				"http://dev1.smcworld.com", "http://ap1.smcworld.com", "http://ap2.smcworld.com",
        				"https://ap1admin.smcworld.com", "https://cdn.smcworld.com", "https://test.smcworld.com", "https://www.smcworld.com", 
        				"https://3sapi.smcworld.com", "http://3sapi.smcworld.com", "https://www.smc.com.cn", "http://192.168.0.*"));
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        // Access-Control-Allow-Methods
        configuration.setAllowedMethods(Arrays.asList("GET","POST"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // COSR設定を行う範囲のパスを指定する。この例では全てのパスに対して設定が有効になる
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }


} // end class

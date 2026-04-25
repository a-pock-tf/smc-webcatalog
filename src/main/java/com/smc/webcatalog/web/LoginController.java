package com.smc.webcatalog.web;

import java.io.BufferedReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.Lang;
import com.smc.webcatalog.model.LoginForm;
import com.smc.webcatalog.model.SeriesLinkMaster;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.service.LangService;
import com.smc.webcatalog.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/login")
@SessionAttributes("SessionUser")
public class LoginController  extends BaseController  {

	@Autowired
	UserService service;

	@Autowired
	LangService langService;

	@Autowired
	HttpSession session;

	@Autowired
    HttpServletRequest req;

	/**
	 * Init User(Session)
	 * @return
	 */
	@ModelAttribute("SessionUser")
	User getSessionUser() {
		return new User();
	}

    /**
     * ログイン画面表示
     * @return getメソッドの時はログイン画面を表示する
     */
    @GetMapping({"", "/", "/index"})
    public ModelAndView edit(
			ModelAndView mav,
			@ModelAttribute("LoginForm") LoginForm myform,
			BindingResult br) {

		// Set view
		mav.setViewName("/index");

		boolean isSubMachine = false;

		InetAddress ia;
		try {
			ia = InetAddress.getLocalHost();
			String hostname = ia.getHostName();
			if (hostname != null && hostname.indexOf("bk2") > 0) {
				isSubMachine = true;
			}
		} catch (UnknownHostException e) {
			log.error("Error! do not Login. this is subMachine. ");
			e.printStackTrace();
		}

		String uri = req.getRequestURI();

		if (br.hasErrors())
		{
			mav.addObject("loginError", true);
 			mav.addObject("message","Error!" );
		} else if (isSubMachine) {
			mav.addObject("loginError", true);
 			mav.addObject("message","Error! this machine is bk2. Do not Login!!" );
		}

		if (mav != null)
		{
			//Add Form to View
			if (myform != null) {
			mav.addObject(myform);
			}
		}
		return mav;
	}

    @RequestMapping(value = "/post")
 	public ModelAndView post(
 			@ModelAttribute("LoginForm") LoginForm user,
 			BindingResult result) {
 		// XXX BindingResultは@Validatedの直後の引数にする

 		log.debug(user.toString());

 		boolean isSubMachine = false;

		InetAddress ia;
		try {
			ia = InetAddress.getLocalHost();
			String hostname = ia.getHostName();
			if (hostname != null && hostname.indexOf("bk2") > 0) {
				isSubMachine = true;
			}
		} catch (UnknownHostException e) {
			log.error("Error! do not Login. this is subMachine. ");
			e.printStackTrace();
		}


 		ModelAndView mav = new ModelAndView();

 		ErrorObject obj = new ErrorObject();
 		// 3) フォームvalidate
// 		User user = loginValidator.validate(result, form);

 		log.debug(result.toString());

 		// 4) エラー判定
 		if (isSubMachine) {
 			mav.setViewName("/login/index");
 			mav.addObject("loginError", true);
 			mav.addObject("message","Error! this machine is bk2. Do not Login!!" );
 		} else 	if (!result.hasErrors()) {
 			// Set view
 			mav.setViewName("redirect:/login/admin/category"); // forwardだとpostのまま
 			String err = "url=" + req.getRequestURI() + " IP=" + req.getRemoteAddr() + " Error=" + result.getFieldErrors();
 			log.error(err);
 		} else {

 			// Set view
			mav.setViewName("/login/index");
 			mav.addObject("loginError", true);
 			mav.addObject("message","Error!" );
 		}
 		return mav;
     }

    @RequestMapping(value = "/loginSuccess")
	public ModelAndView success(
			@ModelAttribute("LoginForm") LoginForm form,
			@ModelAttribute("SessionUser") User s_user,
			BindingResult result) {
		// XXX BindingResultは@Validatedの直後の引数にする

		ModelAndView mav = new ModelAndView();

		ErrorObject obj = new ErrorObject();
		// 3) フォームvalidate
//		User user = loginValidator.validate(result, form);

		log.debug(result.toString());

		// 4) エラー判定
		if (!result.hasErrors()) {
			SecurityContext securityContext = SecurityContextHolder.getContext();
			org.springframework.security.core.userdetails.User u = (org.springframework.security.core.userdetails.User )securityContext.getAuthentication().getPrincipal();

			ErrorObject err = new ErrorObject();
			// User user = service.login(u.getUsername(), u.getPassword(), err); // u.getPassword()はnull
			User user = service.getFromLoginId(u.getUsername(), err);
			if (user == null)
			{
				mav.setViewName("redirect:/index?error=true");
			}
			else
			{
				s_user.setActive(user.isActive());
				s_user.setAdmin(user.isAdmin());
				if (user.isAdmin() == false) {
					s_user.setLangList(user.getLangList());
				}
				s_user.setCompany(user.getCompany());
				s_user.setCountry(user.getCountry());
				s_user.setEmail(user.getEmail());
				s_user.setId(user.getId());
				s_user.setLang(user.getLang());
				s_user.setLoginId(user.getLoginId());
				s_user.setName(user.getName());
				s_user.setPassword(user.getPassword());

				mav.addObject("SessionUser", s_user);

				// Set view
				String dispLocale="ja";
				if (StringUtils.isEmpty( s_user.getLang()) == false ) {
					String tmp = s_user.getLang();
					if (StringUtils.isEmpty(tmp) ==false) {
						dispLocale = tmp;
					}
				}
				String dispLang = "ja-jp";
				if (s_user.getLangList() != null && s_user.getLangList().length > 0) {
					dispLang = s_user.getLangList()[0];
				}
				mav.setViewName("redirect:/login/admin/welcome?locale="+dispLocale + "&lang="+dispLang); // forwardだとpostのまま

				// ログイン成功でLangのContextを確認
				Object object = req.getServletContext().getAttribute(Lang.APPLICATION_CONTEXT_PREFIX);
				if (object == null) {
					// Activeな言語を保持
					req.getServletContext().setAttribute(Lang.APPLICATION_CONTEXT_PREFIX,  langService.listAll(true, err));
				}
				object = req.getServletContext().getAttribute(Lang.APPLICATION_CONTEXT_ALL_PREFIX);
				if (object == null) {
					// Active以外も保持
					req.getServletContext().setAttribute(Lang.APPLICATION_CONTEXT_ALL_PREFIX,  langService.listAll(null, err));
				}
				object = req.getServletContext().getAttribute(Lang.APPLICATION_CONTEXT_VIEW_PREFIX);
				if (object == null) {
					// version==falseのみ
					req.getServletContext().setAttribute(Lang.APPLICATION_CONTEXT_VIEW_PREFIX,  langService.listAllWithoutVersion(err));
				}


				// SeriesLinkMaster
				Object objLinkMaster = req.getServletContext().getAttribute(SeriesLinkMaster.APPLICATION_CONTEXT_PREFIX);
				if (objLinkMaster == null) {
					req.getServletContext().setAttribute(SeriesLinkMaster.APPLICATION_CONTEXT_PREFIX,  service.listAll(true, err));
				}
				objLinkMaster = req.getServletContext().getAttribute(SeriesLinkMaster.APPLICATION_CONTEXT_ALL_PREFIX);
				if (objLinkMaster == null) {
					req.getServletContext().setAttribute(SeriesLinkMaster.APPLICATION_CONTEXT_ALL_PREFIX,  service.listAll(null, err));
				}
			}
		} else {

			// Set view
			mav.setViewName("redirect:/index?error=true");
		}
		return mav;
    }
    @RequestMapping("/admin/welcome")
    public ModelAndView welcome(
			ModelAndView mav,
			@RequestParam(name = "lang", required = false, defaultValue = "") String lang,
			@RequestParam(name = "locale", required = false, defaultValue = "") String locale,
			@ModelAttribute("SessionUser") User s_user,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {
		if (lang != null && lang.isEmpty() == false) {
			s_state.setLang(lang);
		}
		String _locale = "";
		if (!StringUtils.isEmpty(locale)) {
			_locale = locale;
		} else if (s_user.getLangList() != null && s_user.getLangList().length > 0) {
			_locale = s_user.getLangList()[0];
		}
		int cate = 0;
		if (s_user.isEditableCategory("webcatalog")) {
			cate+=1;
		}
		if (s_user.isEditableCategory("discontinued")) {
			cate+=2;
		}
		if (s_user.isEditableCategory("psitem")) {
			cate+=3;
		}

		if (cate >= 6) {
			s_state.setTopUrl("/login/admin/welcome");
			mav.setViewName("/login/admin/welcome");
		} else if ((cate & 3) == 3) {
			s_state.setTopUrl("/login/admin/psitem");
			mav.setViewName("/login/admin/psitem");
		} else if ((cate & 2) == 2) {
			s_state.setTopUrl("/login/admin/discontinued");
			mav.setViewName("/login/admin/discontinued");
		} else if ((cate & 1) == 1) {
			s_state.setTopUrl("/login/admin/category");
			mav.setViewName("/login/admin/category");
		}
		mav.addObject("path", "lang="+s_state.getLang() + "&locale="+locale);
 		return mav;
    }
    @RequestMapping(value = {"/logout/", "/logout/**"})
    public ModelAndView logout(
    		@RequestParam("logout") String logout,
    		@ModelAttribute("SessionUser") User s_user,
    		@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state,
    		SessionStatus status
    		) {
    	// セッションクリア
    	status.setComplete();
 		ModelAndView mav = new ModelAndView();
 		mav.setViewName("/login/index");
 		return mav;
    }
    @RequestMapping("/loginError")
    public ModelAndView loginError(
			ModelAndView mav,
			@ModelAttribute("UserDetails") UserDetails myform) {

		// Set view
		mav.setViewName("/index");


//		User user = new User();

		// Map(Copy) Category -> Form
//		modelMapper.map(user, myform);

		//Add Form to View
		mav.addObject(myform);
		mav.addObject("loginError", true);
		mav.addObject("message","Error!" );

		return mav;
	}

    @RequestMapping("/admin/deleteCache")
    public ModelAndView deleteCache(
			ModelAndView mav,
			@ModelAttribute("SessionUser") User s_user,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {
		mav.setViewName("/login/admin/deleteCache");
 		return mav;
    }

    @RequestMapping("/admin/deleteAllCache")
    public ModelAndView deleteAllCache(
			ModelAndView mav,
			@ModelAttribute("SessionUser") User s_user,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {
		mav.setViewName("/login/admin/deleteAllCache");
 		return mav;
    }

    /**
     * Cloudflare用画面
     */
    @RequestMapping("/admin/deleteCacheCloudflare")
    public ModelAndView deleteCacheCloudflare(
			ModelAndView mav,
			@ModelAttribute("SessionUser") User s_user,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {
		mav.setViewName("/login/admin/deleteCacheCloudflare");
 		return mav;
    }
    /**
     * Cloudflare用処理
     */
    @RequestMapping("/admin/postCloudflarePurgeCache")
    @ResponseBody
    public String postCloudflarePurgeCache(
    		HttpServletRequest request,
    		HttpServletResponse res
    		) {
    	String ret = "";
    	HttpClient client = new HttpClient();
        PostMethod post = null;
        try{
       		BufferedReader br = new BufferedReader( req.getReader() );
            String jsonString = br.readLine();
            jsonString = URLDecoder.decode( jsonString, "UTF-8" );
            log.info("postCloudflarePurgeCache() jsonString="+ jsonString);
            
            boolean isCN = false;
            if (jsonString.indexOf("smc.com.cn") > -1) isCN = true;

        	String authKey = "Bearer BuIG8UPKTvNxua1t6dLZxYlUaI3LYMX2mK-feB2o";
        	String ZONE_ID = "e47ffcb69a5a4c793b752e246e9b5b89";
        	String ZONE_ID_CN = "28df4b3df23b26a72b8bdda5049f33e6";
        	String url = "https://api.cloudflare.com/client/v4/zones/";
        	if (isCN) url += ZONE_ID_CN+"/purge_cache";
        	else url += ZONE_ID+"/purge_cache";
        	URL u = new URL(url);

        	String host = u.getHost();
       		client.getHostConfiguration().setHost(host);

       		//String jsonString = "{\"files\":[\"https://www.smcworld.com/assets/homenews/ja-jp/pf3ah.jpg\"]}";
            int timeout = 35000;
	        client.getParams().setParameter("http.socket.timeout", timeout);
	        StringRequestEntity requestEntity = new StringRequestEntity(
	        		jsonString,
	        	    "application/json",
	        	    "UTF-8");

	        post = new PostMethod( url );
            post.addRequestHeader("Content-Type", "application/json");
            post.addRequestHeader("Authorization", authKey);

            post.setRequestEntity(requestEntity);
	        int status = client.executeMethod(post);
	        String body = post.getResponseBodyAsString();
	        List<String> response = IOUtils.readLines(post.getResponseBodyAsStream(), "UTF-8");
	        if(status != 200 && response != null)
	        {
	        	for(String r : response) ret += r + "\r\n";
	        } else {
	        	ret = response.get(0);
	        	log.info("postCloudflarePurgeCache() status="+ status);
	        	log.info("postCloudflarePurgeCache() body="+ body);
	        }

        }catch(Exception ex){
	    	log.debug(ex.toString());
	    }finally{

	        if (post != null) post.releaseConnection();
	    }

 		return ret;
    }
    

}

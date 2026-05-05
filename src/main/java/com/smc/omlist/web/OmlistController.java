package com.smc.omlist.web;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.smc.omlist.model.OmlistForm;
import com.smc.omlist.service.OmlistFormValidator;
import com.smc.omlist.service.OmlistServiceImpl;
import com.smc.webcatalog.model.MyErrors;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.web.ScreenStatusHolder;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/login/admin/omlist")
@SessionAttributes(value= {"SessionScreenState", "SessionUser"})
public class OmlistController extends BaseController {


	@Autowired
	OmlistServiceImpl service;

	@Autowired
	OmlistFormValidator validator;

	@Autowired
    MessageSource messagesource;

	@Autowired
	HttpSession session;

	@Autowired
    HttpServletRequest req;

    @Autowired
	Environment env;

	private final String DIR="/tmp/omlist_data";

	private final int COL_SIZE = 18; // １行当たりのカラム数

	private final String[] LANGS = {"ja-jp", "en-jp", "zh-cn", "zh-tw"};

	/**
	 * Init ScreenState(Session)
	 * @return
	 */
	@ModelAttribute("SessionScreenState")
	ScreenStatusHolder getScreenState() {
		return new ScreenStatusHolder();
	}


	/**
	 * 管理系 > フォーム
	 * @param mav
	 * @return
	 */
	@GetMapping({ "", "/"})
	public ModelAndView list(
			ModelAndView mav,
			@ModelAttribute("omlistForm") OmlistForm myform,
			@RequestParam(name = "lang", required = false, defaultValue = "") String lang,
			@RequestParam(name = "locale", required = false, defaultValue = "") String locale,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		mav.setViewName("/login/admin/omlist/index");

		if (!StringUtils.isEmpty(lang)) {
			s_state.setLang(lang);
		}
		myform.setLang( s_state.getLang());

		mav.addObject("selectLang", getRadioLangs());

		mav.addObject(myform);
		return mav;
	}


	/**
	 * POSTされたデータからDB更新
	 * @param mav
	 * @param form
	 * @param result
	 * @return
	 */
	@RequestMapping(value = "/post", method = RequestMethod.POST)
	public ModelAndView post(
			ModelAndView mav,
			@RequestParam(name = "import_before", required = false, defaultValue = "") String beforeUrl,
			@Validated @ModelAttribute("omlistForm") OmlistForm form,
			BindingResult result) {
		// XXX BindingResultは@Validatedの直後の引数にする

		// Set view
		mav.setViewName("/login/admin/omlist/result");

		log.debug(form.toString());

		User s_user = (User)session.getAttribute("SessionUser");
		// 1) - 7)までが基本的な更新処理の流れ

			//新規の場合はそのまま
		validator.validateNew(result, form);

		log.debug(result.toString());

		// 4) エラー判定
		if (!result.hasErrors()) {

			File dir = new File(DIR);
            if(!dir.exists()) dir.mkdirs();

            String enc = "MS932";
            if (form.getLang().indexOf("zh") > -1) enc = "UTF-8";

            String fullpath = dir+"/"+"omlist.csv";
            if (form.getLang().indexOf("zh") > -1) fullpath = dir+"/"+"omlist_"+form.getLang()+".csv";

            String s1 = createFile(form.getFile(), fullpath , enc);

            MyErrors errors = service.checkFormat(fullpath, enc, COL_SIZE);

            if (errors.hasError()) {
            	mav.addObject("selectLang", getRadioLangs());
            	mav.addObject("is_error", "Error! "+errors.getAllMessages());
    			mav.setViewName("/login/admin/omlist/index");
            } else {
    			// 6) 保存
            	int cnt = service.importItem(fullpath, form.getLang(), enc);
            	mav.addObject("is_success", "登録:"+cnt+"件");
            }
		} else {
			mav.addObject("selectLang", getRadioLangs());
			mav.setViewName("/login/admin/omlist/index");
		}

		mav.addObject(form);

		return mav;
	}

	// ========== private ==========
	private String createFile(MultipartFile file,String filepath,String enc){

    	String s = "";
	    try{
	    	InputStream is = file.getInputStream();
			BufferedReader br = new BufferedReader( new InputStreamReader(is, enc) );
	        BufferedWriter out  = new BufferedWriter( new OutputStreamWriter(new FileOutputStream(filepath),enc) );
	        String line;
	        int i=0;
	        while ((line = br.readLine()) != null) {
	        	i++;
            	out.write(line);
            	out.write("\r\n");
	        }
	        is.close();
	        out.close();
	    }catch(Exception ex){
	    	log.error("createFile()"+ex.getMessage());
	    }

	    return s;

    }
	private Map<String,String> getRadioLangs(){
		Map<String, String> ret = new LinkedHashMap<String, String>();
		for(String lang : LANGS) {
			ret.put(lang, lang);
		}
	     return ret;
	}


	private Locale getLocale(String lang) {
		Locale loc = Locale.JAPANESE;
		if (lang.indexOf("en") > -1) loc = Locale.ENGLISH;
		else if (lang.indexOf("zh-tw") > -1) loc = Locale.TAIWAN;
		else if (lang.indexOf("zh") > -1)  loc = Locale.CHINESE;
		return loc;
	}

}

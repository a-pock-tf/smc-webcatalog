package com.smc.webcatalog.web;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.smc.webcatalog.dao.CategoryRepository;
import com.smc.webcatalog.dao.CategorySeriesRepository;
import com.smc.webcatalog.dao.CategoryTemplateImpl;
import com.smc.webcatalog.dao.LangRepository;
import com.smc.webcatalog.dao.SeriesLinkMasterRepository;
import com.smc.webcatalog.dao.SeriesLinkRepository;
import com.smc.webcatalog.dao.SeriesRepository;
import com.smc.webcatalog.dao.TemplateRepository;
import com.smc.webcatalog.dao.UserRepository;
import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.CategorySeries;
import com.smc.webcatalog.model.CategoryType;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.ImpCategory;
import com.smc.webcatalog.model.ImpCategorySlug;
import com.smc.webcatalog.model.ImpCategoryTemplate;
import com.smc.webcatalog.model.ImpItemType;
import com.smc.webcatalog.model.ImpSeries;
import com.smc.webcatalog.model.Lang;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Series;
import com.smc.webcatalog.model.SeriesLink;
import com.smc.webcatalog.model.SeriesLinkMaster;
import com.smc.webcatalog.model.TemplateCategory;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.model.ViewState;
import com.smc.webcatalog.service.ImpCategoryService;
import com.smc.webcatalog.service.ImpSeriesService;
import com.smc.webcatalog.service.TemplateCategoryServiceImpl;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@SessionAttributes(value= {"SessionScreenState", "SessionUser"})
public class ImpController {

	@Autowired
	ImpCategoryService impCategoryService;
	@Autowired
	ImpSeriesService impSeriesService;

	@Autowired
	private MongoTemplate db;
	@Autowired
	CategoryRepository catDao;

	@Autowired
	CategoryTemplateImpl catTemp;

	@Autowired
	UserRepository userDao;

	@Autowired
	SeriesRepository seriesDao;

	@Autowired
	CategorySeriesRepository csDao;

	@Autowired
	LangRepository langDao;

	@Autowired
	SeriesLinkRepository sLinkDao;

	@Autowired
	SeriesLinkMasterRepository sLinkMasterDao;

	@Autowired
	TemplateRepository templateDao;

	@Autowired
	TemplateCategoryServiceImpl templateCategoryService;

	// これでカテゴリ、シリーズすべてインポート。
//	@RequestMapping("/login/admin/importCategory")
	public ModelAndView importCategory(ModelAndView mav,
			@ModelAttribute("SessionUser") User s_user,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		db.dropCollection(CategorySeries.class);
		db.dropCollection(SeriesLink.class);
		db.dropCollection(Category.class);
		db.dropCollection(Series.class);

		csDao.deleteAll();
		sLinkDao.deleteAll();
		catDao.deleteAll();
		seriesDao.deleteAll();

		db.dropCollection("category_series");
		db.dropCollection("category");
		db.dropCollection("series_link");
		db.dropCollection("series");

		Optional<User> oUser = userDao.findByName("AdminUser");
		User user = oUser.get();

		String JpTestId = null;
		String JpProdId = null;
		String JpOtherTestId = null;
		String JpOtherProdId = null;
		CategoryType ct = CategoryType.CATALOG;
		ErrorObject err = new ErrorObject();

	    List<ImpCategory> categoryList = null;//impCategoryService.list();
	    List<Category> jaList = new LinkedList<Category>();  // 1階層 TEST
	    List<Category> jaProdList = new LinkedList<Category>();  // 1階層 PROD
	    HashMap<Integer, List<Category>> jaChild = new HashMap<Integer, List<Category>>();  // 2階層 Integerは1階層
	    HashMap<Integer, List<Category>> jaProdChild = new HashMap<Integer, List<Category>>();  // 2階層 PROD
	    HashMap<String, Series> jaSeriesMap = new HashMap<String, Series>(); // s_idをキーに。
	    HashMap<String, Series> jaSeriesProdMap = new HashMap<String, Series>(); // s_idをキーに。
	    List<SeriesLinkMaster> sLinkMaster = new LinkedList<SeriesLinkMaster>();
	    HashMap<Integer, String> categoryIdMap = new HashMap<Integer, String>(); // 新旧のCategoryID。パンくずの置き換えに利用
	    ImpCategorySlug slug = new ImpCategorySlug(); // 新サイト用スラッグ一覧。
	    ImpCategoryTemplate categoryTemplate = new ImpCategoryTemplate(); // HeartCoreのIDと新規CategoryIDを紐づけ

	    int cnt = 0; // 言語単位のトータル
	    int Cnt1 = 0; // 1階層
	    int Cnt2 = 0; // 2階層
	    String impLang = "";
	    String lang = "";
	    String testId = "";
	    String prodId = "";
	    String testOtherId = "";
	    String prodOtherId = "";
		String prefix = "-E";
		boolean isOther = false; // １度も出て来なければRootは作成
	    for (ImpCategory impC: categoryList)
	    {
	    	if (impC.getActive() == false) { // 非表示はインポートしない
	    		continue;
	    	}
	    	if (impC.getLang().equals("en") && impC.getName_html().equals("Gripper for Collaborative Robots")) // Gripper for Collaborative Robotsはインポートしない
	    	{
	    		continue;
	    	}
    		if (impC.getType() != ImpItemType.SERIES) {
    			ct = CategoryType.OTHER;
    			isOther = true;
    		}
    		else {
    			ct = CategoryType.CATALOG;
    		}

    		if (!impLang.equals(impC.getLang()))
	    	{
	    		impLang = impC.getLang();
	    		if (impLang.equals("ja")) lang = "ja-jp";
	    		else if (impLang.equals("en")) lang = "en-jp";
	    		else if (impLang.equals("zh-tw")) {lang = "zh-tw";impLang="zhtw";}
	    		else if (impLang.equals("zhtw")) lang = "zh-tw";
	    		else if (impLang.equals("zh")) lang = "zh-cn";
    			if (lang.equals("zh")) prefix = "-ZH";
    			else if (lang.equals("zh-tw")) prefix = "-ZHTW";
	    		cnt = 0;
	    		Cnt1 = 0;

	    		// make root(言語の切り替わり時にOTHERも作成しておく。)
	    		testId = createRootCategory(lang, JpTestId, CategoryType.CATALOG);
	    		if (lang.equals("ja-jp")) JpTestId = testId;
	    		prodId =  createProdRootCategory(lang, testId, JpProdId, CategoryType.CATALOG);
	    		if (lang.equals("ja-jp")) JpProdId = prodId;

	    		testOtherId = createRootCategory(lang, JpOtherTestId, CategoryType.OTHER);
	    		if (lang.equals("ja-jp")) JpOtherTestId = testOtherId;
	    		prodOtherId =  createProdRootCategory(lang, testOtherId, JpOtherProdId, CategoryType.OTHER);
	    		if (lang.equals("ja-jp")) JpOtherProdId = prodOtherId;

	    		sLinkMaster = sLinkMasterDao.findAllByLang(lang, null);
	    	}

    		// 日本語だけあるようなカテゴリ、その逆を飛ばす処理
    		if (jaList != null && jaList.size() > 0) {
	    		if (jaList.size() > Cnt1 && jaList.get(Cnt1).getName().equals("産業用通信機器／無線システム")) {
	    			Cnt1++;
	    			cnt++;
	    		}
    		}

	    	{
	    		// 1階層目のカテゴリ作成
	    		String langRefId = "";
	    		String langRefProdId = "";
	    		String langRefSlug = "";
	    		if (lang.equals("ja-jp") == false) {
	    			if (lang.equals("zh-cn")) {
	    				// zh-cnのみ25Aと20-が逆
	    				if (impC.getName_html().equals("二次电池 25A-系列")) {
			    			langRefId = jaList.get(Cnt1+1).getId();
			    			langRefProdId = jaProdList.get(Cnt1+1).getId();
			    			langRefSlug = jaList.get(Cnt1+1).getSlug();
	    				} else if (impC.getName_html().equals("禁铜、禁氟 20-系列")) {
			    			langRefId = jaList.get(Cnt1-1).getId();
			    			langRefProdId = jaProdList.get(Cnt1-1).getId();
			    			langRefSlug = jaList.get(Cnt1-1).getSlug();
	    				} else {
			    			langRefId = jaList.get(Cnt1).getId();
			    			langRefProdId = jaProdList.get(Cnt1).getId();
			    			langRefSlug = jaList.get(Cnt1).getSlug();
	    				}
	    			} else {
		    			langRefId = jaList.get(Cnt1).getId();
		    			langRefProdId = jaProdList.get(Cnt1).getId();
		    			langRefSlug = jaList.get(Cnt1).getSlug();
	    			}
	    		} else {
	    			langRefSlug = slug.getSlug(impC.getName_html());
	    		}

	    		Category c = impC.getCategory(testId, langRefId, null, lang, ct, ModelState.TEST, user, cnt);
	    		c.setSlug(langRefSlug);
	    		c = catDao.insert(c);
		    	Category cP = impC.getCategory(prodId, langRefProdId, c.getId(), lang, ct, ModelState.PROD, user, cnt);
	    		cP.setSlug(langRefSlug);
		    	cP = catDao.insert(cP);
	    		categoryIdMap.put(impC.getId(), c.getSlug());

		    	if (lang.equalsIgnoreCase("ja-jp"))
		    	{
		    		jaList.add(c);
		    		jaProdList.add(cP);
		    	}
	    		// templateCategory
		    	{
		    		String hcTemplateID = categoryTemplate.getIdFromName(c.getName().trim(), lang);
		    		if (hcTemplateID != null && hcTemplateID.isEmpty() == false) {
			    		TemplateCategory tc = templateCategoryService.getHeartCoreID(hcTemplateID, err);
			    		if (tc != null) {
			    			tc.setCategoryId(c.getId());
			    			templateCategoryService.save(tc);
			    		}
		    		}
		    	}

	    		Cnt1++;
	    		cnt++;

	    		// 1階層目のシリーズ作成
	    		List<ImpSeries> seList = null;//impSeriesService.list(impC.getId());
	    		if (seList != null && seList.size() > 0)
	    		{
		    		CategorySeries cs = new CategorySeries();
		    		cs.setCategoryId(c.getId());
		    		List<Series> listSeries = new LinkedList<Series>();
		    		CategorySeries csP = new CategorySeries();
		    		csP.setCategoryId(cP.getId());
		    		List<Series> listSeriesProd = new LinkedList<Series>();

	    			for(ImpSeries impSe: seList) {
	    				// シリーズはse_sid(SeriesID)で紐づけ。
	    	    		String langRefSeriesId = "";
	    	    		String langRefProdSeriesId = "";
	    	    		if (lang.equals("ja-jp") == false) {
	    	    			Series tmp = jaSeriesMap.get(impSe.getSidN(lang));
	    	    			if (tmp != null) {
		    	    			langRefSeriesId = jaSeriesMap.get(impSe.getSidN(lang)).getId();
		    	    			langRefProdSeriesId = jaSeriesProdMap.get(impSe.getSidN(lang)).getId();
	    	    			}
	    	    			else
	    	    			{
	    	    				log.debug("Series not found. name="+ impSe.getName() +" se_id=" + impSe.getSidN(lang) + " lang=" + lang );
	    	    			}
	    	    		}
	    	    		ImpSeries fullSeries = null;//impSeriesService.getWithSeriesMapItems(impSe.getId());
	    				Series se = fullSeries.getSeries(lang, langRefSeriesId, null, ModelState.TEST, sLinkMaster, user);
	    				se = seriesDao.insert(se);
	    				listSeries.add(se);

	    				List<SeriesLink> sLink = se.getLink();
	    				for(SeriesLink sl : sLink) {
	    					sl.setSeriesId(se.getId());
		    				sl = sLinkDao.insert(sl);
	    				}

	    				ImpSeries fullSeriesP = null;//impSeriesService.getWithSeriesMapItems(impSe.getId());
	    				Series seP = fullSeriesP.getSeries(lang, langRefProdSeriesId, se.getId(), ModelState.PROD, sLinkMaster, user);
	    				seP = seriesDao.insert(seP);
	    				listSeriesProd.add(seP);

	    				List<SeriesLink> sLinkP = seP.getLink();
	    				for(SeriesLink sl : sLinkP) {
	    					sl.setSeriesId(seP.getId());
		    				sl = sLinkDao.insert(sl);
	    				}

	    				if (lang.equalsIgnoreCase("ja-jp"))
	    		    	{
	    		    		jaSeriesMap.put(se.getModelNumber(), se);
	    		    		jaSeriesProdMap.put(seP.getModelNumber(), seP);
	    		    	}
	    			}

	    			cs.setSeriesList(listSeries);
	    			csDao.insert(cs);
	    			csP.setSeriesList(listSeriesProd);
	    			csDao.insert(csP);
	    		}

	    		List<ImpCategory> ch = impC.getChildren();
	    		if (ch != null && ch.size() > 0)
	    		{
	    			Cnt2 = 0;
	    			List<Category> childList = new LinkedList<Category>();
	    			List<Category> childProdList = new LinkedList<Category>();
	    			for(ImpCategory impCaCh:ch)
	    			{
	    				langRefId = "";
	    				langRefProdId = "";
	    				langRefSlug = "";
	    	    		if (lang.equals("ja-jp") == false) {
	    	    			List<Category> tmp = jaChild.get(Cnt1);
	    	    			List<Category> tmpP = jaProdChild.get(Cnt1);
	    	    			if (tmp != null && tmp.size() > Cnt2) {
	    	    				langRefId = tmp.get(Cnt2).getId();
	    	    				langRefSlug = tmp.get(Cnt2).getSlug();
	    	    			}
	    	    			if (tmpP != null  && tmpP.size() > Cnt2) {
	    	    				langRefProdId = tmpP.get(Cnt2).getId();
	    	    			}
	    	    		} else {
	    	    			langRefSlug = slug.getSlug(impCaCh.getName_html().trim());
	    	    		}
	    				Category c2 = impCaCh.getCategory(c.getId(), langRefId, null, lang, ct, ModelState.TEST, user, cnt);
	    				c2.setSlug(langRefSlug);
	    	    		c2 = catDao.insert(c2);
	    	    		childList.add(c2);
	    	    		Category c2P = impCaCh.getCategory(cP.getId(), langRefProdId, c2.getId(), lang, ct, ModelState.PROD, user, cnt);
	    				c2P.setSlug(langRefSlug);
	    	    		c2P = catDao.insert(c2P);
	    	    		childProdList.add(c2P);

	    	    		categoryIdMap.put(impCaCh.getId(), c2.getSlug());

	    	    		cnt++;
	    	    		Cnt2++;

	    	    		// 2階層目のシリーズ作成
	    	    		seList = null;//impSeriesService.list(impCaCh.getId());
	    	    		if (seList != null && seList.size() > 0)
	    	    		{
	    	    			CategorySeries cs = new CategorySeries();
	    		    		cs.setCategoryId(c2.getId());
	    		    		List<Series> listSeries = new LinkedList<Series>();
	    		    		CategorySeries csP = new CategorySeries();
	    		    		csP.setCategoryId(c2P.getId());
	    		    		List<Series> listSeriesProd = new LinkedList<Series>();

	    	    			for(ImpSeries impSe: seList) {
	    	    				if (impSe.getActive() == false) continue;
	    	    				// シリーズはse_sid(SeriesID)で紐づけ。
	    	    				// 重複もあるので、se_sid(SeriesID)で確認
	    	    	    		String langRefSeriesId = "";
	    	    	    		String langRefProdSeriesId = "";
	    	    	    		if (lang.equals("ja-jp") == false) {
	    	    	    			Series tmp = jaSeriesMap.get(impSe.getSidN(lang));
	    	    	    			if (tmp != null) {
		    	    	    			langRefSeriesId = tmp.getId();
		    	    	    			langRefProdSeriesId = jaSeriesProdMap.get(impSe.getSidN(lang)).getId();
	    	    	    			}
	    	    	    			else
	    	    	    			{
	    	    	    				log.debug("Series not found. name="+ impSe.getName() +" se_id=" + impSe.getSidN(lang) + " lang=" + lang );
	    	    	    			}
	    	    	    		}
	    	    	    		Optional<Series> chk = seriesDao.findByModelNumber(impSe.getSid(), ModelState.TEST, null);
	    	    	    		if (chk.isPresent() == false) {
	    	    	    			ImpSeries fullSeries = null;//impSeriesService.getWithSeriesMapItems(impSe.getId());
		    	    				Series se = fullSeries.getSeries(lang, langRefSeriesId, null, ModelState.TEST, sLinkMaster, user);
		    	    				seriesDao.insert(se);
		    	    				listSeries.add(se);

		    	    				List<SeriesLink> sLink = se.getLink();
		    	    				for(SeriesLink sl : sLink) {
		    	    					sl.setSeriesId(se.getId());
		    		    				sLinkDao.insert(sl);
		    	    				}

		    	    				ImpSeries fullSeriesP = null;//impSeriesService.getWithSeriesMapItems(impSe.getId());
		    	    				Series seP = fullSeriesP.getSeries(lang, langRefProdSeriesId, se.getId(), ModelState.PROD, sLinkMaster, user);
		    	    				seriesDao.insert(seP);
		    	    				listSeriesProd.add(seP);

		    	    				List<SeriesLink> sLinkP = seP.getLink();
		    	    				for(SeriesLink sl : sLinkP) {
		    	    					sl.setSeriesId(seP.getId());
		    		    				sl = sLinkDao.insert(sl);
		    	    				}

		    	    				if (lang.equalsIgnoreCase("ja-jp")) {
		    	    		    		jaSeriesMap.put(se.getModelNumber(), se);
		    	    		    		jaSeriesProdMap.put(seP.getModelNumber(), seP);
		    	    		    	}
	    	    	    		}
	    	    	    		else {
	    	    	    			Series se = chk.get();
		    	    				listSeries.add(se);
		    	    	    		Optional<Series> chkP = seriesDao.findByModelNumber(impSe.getName(), ModelState.PROD, null);
		    	    				if (chkP.isPresent()) listSeriesProd.add(chkP.get());
	    	    	    		}
	    	    			}

	    	    			cs.setSeriesList(listSeries);
	    	    			csDao.insert(cs);
	    	    			csP.setSeriesList(listSeriesProd);
	    	    			csDao.insert(csP);
	    	    		}
	    			}
	    			if (lang.equals("ja-jp")) {
	    				jaChild.put(Cnt1, childList);
	    				jaProdChild.put(Cnt1, childProdList);
	    			}

	    		}
	    	}
	    }
	    if (isOther == false) {
	    	// make rootlang = "ja-jp";
    		testId = createRootCategory("ja-jp", null, CategoryType.OTHER);
    		prodId =  createProdRootCategory("ja-jp", testId, null, CategoryType.OTHER);
	    }

	    // パンくずの置き換え
	    List<Lang> langList = langDao.listAll(null);
	    for(Lang la: langList) {
	    	List<Series> saveList = new LinkedList<Series>();
		    List<Series> listSeries = seriesDao.listAll(la.getName(), ModelState.TEST, null, null);
		    for(Series s : listSeries) {
		    	String tmp = s.getBreadcrumb();
		    	String ret = getCatpansToBreadCrumb(categoryIdMap, tmp);
		    	if (ret != null) {
		    		s.setBreadcrumb(ret);
//		    		saveList.add(s);
		    		seriesDao.save(s);
		    	}
		    }
//		    if (saveList.size() > 0) seriesDao.saveAll(saveList);

		    List<Series> saveListP = new LinkedList<Series>();
		    List<Series> listSeriesP = seriesDao.listAll(la.getName(), ModelState.PROD, null, null);
		    for(Series s : listSeriesP) {
		    	String tmp = s.getBreadcrumb();
		    	String ret = getCatpansToBreadCrumb(categoryIdMap, tmp);
		    	if (ret != null) {
		    		s.setBreadcrumb(ret);
//		    		saveListP.add(s);
		    		seriesDao.save(s);
		    	}
		    }
//		    if (saveListP.size() > 0) seriesDao.saveAll(saveListP);
	    }

	    AddCopperCategory("銅系・フッ素系", "ja-jp","シリーズ", "copper-fluorine-free-20");
	    AddCopperCategory("Copper", "en-jp","Series ", "copper-fluorine-free-20");
	    AddCopperCategory("禁铜、禁氟", "zh-cn","系列", "copper-fluorine-free-20");
	    // TODO zh-tw

	    s_state.setView(ViewState.CATEGORY.toString());
		s_state.setType(CategoryType.CATALOG.toString());
		s_state.setBackUrl("/login/admin/category/search");

		mav.setViewName("/login/admin/category/import_result");
		mav.addObject("list", jaList);
		mav.addObject("keyword", "ja-jp");

	    return mav;
	}

	Pattern p = Pattern.compile("^%(\\d+)%(.*)$");
	private String getCatpansToBreadCrumb(HashMap<Integer, String> map ,String s){
		String r = null;
		if (s != null) {
			r = "";
			StringTokenizer st = new StringTokenizer(s,"●");
			while(st.hasMoreTokens()){
				String ret = "";
				StringTokenizer st2 = new StringTokenizer((String)st.nextToken(),"#");
				while(st2.hasMoreTokens()){
					String cattxt = (String)st2.nextElement();
					//Log.log(cattxt);
					Matcher m = p.matcher(cattxt);
					if(m.find()){
						String id = m.group(1);
						String name = m.group(2);
						int panId = Integer.parseInt(id);
						String slug = map.get(panId);
						if (slug != null) {
							ret +="%" + slug + "%" + name + "#";
						}
					}
				}
				if (ret != null && ret.equals("") == false) r += "●" + ret;
			}
		}
		return r;
	}

	// pdfのパス変更
//	@RequestMapping("/login/admin/importChangePathZH")
	public ModelAndView importChangePathZH(ModelAndView mav,
			@ModelAttribute("SessionUser") User s_user,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {
		List<Series> listSeries = seriesDao.listAll("zh-cn", ModelState.TEST, null, null);
	    for(Series s : listSeries) {
	    	List<SeriesLink> sLink = sLinkDao.findBySeriesId(s.getId());
	    	if (sLink != null) {
		    	for(SeriesLink sl : sLink) {
		    		if (sl.getUrl().indexOf("https:/pdf//") > -1) {
		    			sl.setUrl(sl.getUrl().replace("https:/pdf//", "https://"));
		    			sLinkDao.save(sl);
		    		}
		    	}
	    	}
	    }
		listSeries = seriesDao.listAll("zh-cn", ModelState.PROD, null, null);
	    for(Series s : listSeries) {
	    	List<SeriesLink> sLink = sLinkDao.findBySeriesId(s.getId());
	    	if (sLink != null) {
		    	for(SeriesLink sl : sLink) {
		    		if (sl.getUrl().indexOf("https:/pdf//") > -1) {
		    			sl.setUrl(sl.getUrl().replace("https:/pdf//", "https://"));
		    			sLinkDao.save(sl);
		    		}
		    	}
	    	}
	    }
	    s_state.setView(ViewState.CATEGORY.toString());
		s_state.setType(CategoryType.CATALOG.toString());
		s_state.setBackUrl("/login/admin/category/search");
	    return mav;
	}

	//@RequestMapping("/login/admin/importAll")
	public String hello(Model model) {

		model.addAttribute("hello", "Hello World!"); // Hello World!の表示

	    List<ImpCategory> categoryList = null;//impCategoryService.list();
	    model.addAttribute("category", categoryList);

	    return "importAll";
	}

	@GetMapping("/login/admin/importSeries/{cid}")
	public String helloSeries(Model model,
			  @PathVariable(name = "cid", required = false) String cid) {

	    model.addAttribute("hello", "Hello Series!"); // Hello World!の表示

	    List<ImpCategory> categoryList = null;//impCategoryService.list();
	    model.addAttribute("category", categoryList);

	    int ca_id = Integer.valueOf(cid);
	    List<ImpSeries> sList = null;//impSeriesService.list(ca_id);
	    model.addAttribute("series", sList);

	    return "importAll";
	}

	/// ========== private ==========
	private void AddCopperCategory(String keyword, String lang, String replace, String slug) {
		// TODO zh-tw
		List<Category> list = catTemp.search(keyword, lang, ModelState.TEST, CategoryType.CATALOG, null);
		if (list != null) {
			Category c = list.get(0);
			Category c2 = c.Copy();
			c2.setId(null);
			c2.setParentId(c.getId());
			c2.setName(c.getName().replace(replace, ""));
			c2.setOldId("");
			c2.setOrder(1);
			c2.setSlug(slug);

			catDao.insert(c2);

			List<Category> ch = new LinkedList<Category>();
			ch.add(c2);
			c.setChildren(ch);
			catDao.save(c);

			Iterable<CategorySeries> itCS = csDao.findAllByCategoryId(c.getId());
			for (CategorySeries cs : itCS) {
				cs.setCategoryId(c2.getId());
				csDao.save(cs);
			}

			Optional<Category> oC = catTemp.findByStateRefId(c.getId(), ModelState.PROD, CategoryType.CATALOG);
			if (oC.isPresent()) {
				Category parent = oC.get();
				Category p2 = parent.Copy();

				p2.setId(null);
				p2.setParentId(parent.getId());
				p2.setName(parent.getName().replace(replace, ""));
				p2.setOldId("");
				p2.setOrder(1);
				p2.setSlug(slug);

				catDao.insert(p2);

				List<Category> pch = new LinkedList<Category>();
				pch.add(p2);
				parent.setChildren(pch);
				catDao.save(parent);

				Iterable<CategorySeries> itPCS = csDao.findAllByCategoryId(parent.getId());
				for (CategorySeries cs : itPCS) {
					cs.setCategoryId(p2.getId());
					csDao.save(cs);
				}
			}


		} else {
			log.error("Category is NULL.");
		}
	}

	/**
	 * Rootカテゴリを作成
	 * @return testId StatusがTESTのIDを返す
	 */
	private String createRootCategory(String lang, String langRefId, CategoryType ct) {

		String ret = null;
		// if not exists
		Category rootT = catDao.findRoot(lang, ModelState.TEST, ct);

		if (rootT == null) {
			Category c = new Category();
			c.setState(ModelState.TEST);
			c.setName("root");
			c.setSlug("root");
			c.setParentId("");
			c.setLang(lang);
			c.setType(ct);
			if (langRefId != null) c.setLangRefId(langRefId);
			catDao.save(c);
			ret = c.getId();
		}

		return ret;
	}
	/**
	 * Rootカテゴリを作成
	 * @return prodId StatusがPRODのIDを返す
	 */
	private String createProdRootCategory(String lang, String stateRefId, String langRefId, CategoryType ct) {

		String ret = null;
		// if not exists
		Category rootP = catDao.findRoot(lang, ModelState.PROD, ct);

		if (rootP == null) {
			Category c = new Category();
			c.setState(ModelState.PROD);
			c.setName("root");
			c.setSlug("root");
			c.setParentId("");
			c.setLang(lang);
			c.setType(ct);

			if (langRefId != null) c.setLangRefId(langRefId);
			c.setStateRefId(stateRefId);

			c = catDao.save(c);
			ret = c.getId();
		}
		return ret;
	}

}

package com.smc.webcatalog.config;

import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import com.smc.webcatalog.dao.CategoryRepository;
import com.smc.webcatalog.dao.CategorySeriesRepository;
import com.smc.webcatalog.dao.CategoryTemplateImpl;
import com.smc.webcatalog.dao.LangRepository;
import com.smc.webcatalog.dao.SeriesLinkMasterRepository;
import com.smc.webcatalog.dao.SeriesLinkRepository;
import com.smc.webcatalog.dao.SeriesRepository;
import com.smc.webcatalog.dao.TemplateCategoryRepository;
import com.smc.webcatalog.dao.TemplateRepository;
import com.smc.webcatalog.dao.UserRepository;
import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.CategorySeries;
import com.smc.webcatalog.model.CategoryType;
import com.smc.webcatalog.model.Lang;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Series;
import com.smc.webcatalog.model.SeriesLink;
import com.smc.webcatalog.model.SeriesLinkMaster;
import com.smc.webcatalog.model.SeriesLinkType;
import com.smc.webcatalog.model.Template;
import com.smc.webcatalog.model.TemplateCategory;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.service.NarrowDownServiceImpl;
import com.smc.webcatalog.service.TemplateCategoryServiceImpl;
import com.smc.webcatalog.service.TemplateServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * アプリケーション初期処理など
 * @author miyasit
 *
 */

@Component
@Slf4j
public class Init {

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
	TemplateRepository templateDao;

	@Autowired
	TemplateCategoryRepository templateCategoryDao;

	@Autowired
	SeriesLinkRepository sLinkDao;

	@Autowired
	SeriesLinkMasterRepository sLinkMasterDao;
	
	@Autowired
	TemplateServiceImpl templateService;
	
	@Autowired
	TemplateCategoryServiceImpl templateCategoryService;
	
	@Autowired
	NarrowDownServiceImpl narrowDownService;
	
	// Template, TemplateCategoryの初期化
	public void initService() {
		templateService.refreshTemplates();
		templateCategoryService.refreshTemplateCategories();
		narrowDownService.refreshNarrowDownColumns();
	}

	//Application 全体で使えるBean Thymeleafから ${@beanName} で呼び出す
	@Bean(name = "testCategory")
	@Scope(value = WebApplicationContext.SCOPE_APPLICATION, proxyMode = ScopedProxyMode.TARGET_CLASS)
	//@PostConstruct
	public Category setRootCategory() {
		Category c = new Category();
		c.setName("ApplicatonCategory");
		return c;
	}

	// for TEST
	// アプリケーション開始時の処理(テスト用DBの初期化)
	//@PostConstruct
	public void initMongoDb() {

		log.info("*** Initializing Mongo database");

		db.dropCollection(Category.class);
		db.dropCollection(Series.class);
		db.dropCollection(User.class);
		db.dropCollection(CategorySeries.class);
		db.dropCollection(Lang.class);
		db.dropCollection(Template.class);
		db.dropCollection(TemplateCategory.class);
		db.dropCollection(SeriesLink.class);
		db.dropCollection(SeriesLinkMaster.class);

		catDao.deleteAll();
		userDao.deleteAll();
		seriesDao.deleteAll();
		csDao.deleteAll();
		langDao.deleteAll();
		templateDao.deleteAll();
		templateCategoryDao.deleteAll();

		sLinkDao.deleteAll();
		sLinkMasterDao.deleteAll();

		// add Lang
		Lang lang = new Lang("ja-jp");
		lang.setActive(true);
		langDao.save(lang);
		lang = new Lang("en-jp");
		langDao.save(lang);
		lang = new Lang("zh-cn");
		langDao.save(lang);
		lang = new Lang("zh-tw");
		langDao.save(lang);

		// add User
		User user = new User();
		User adminUser = null;
		user.setLoginId("admin");
		BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
		user.setPassword(bCryptPasswordEncoder.encode("admin"));
		user.setName("AdminUser");
		user.setActive(true);
		user.setAdmin(true);
		userDao.save(user);
		adminUser = user;

		user = new User();
		user.setLoginId("test");
		user.setPassword(bCryptPasswordEncoder.encode("test"));
		user.setName("TestUser");
		user.setActive(true);
		user.setAdmin(false);
		userDao.save(user);

		user = new User();
		user.setLoginId("testEN");
		user.setPassword(bCryptPasswordEncoder.encode("testEN"));
		user.setName("TestEnUser");
		user.setLang("en-jp");
		user.setActive(true);
		user.setAdmin(false);
		userDao.save(user);

		user = new User();
		user.setLoginId("testCN");
		user.setPassword(bCryptPasswordEncoder.encode("testCN"));
		user.setName("TestCnUser");
		user.setLang("zh-cn");
		user.setActive(true);
		user.setAdmin(false);
		userDao.save(user);

		user = new User();
		user.setLoginId("testTW");
		user.setPassword(bCryptPasswordEncoder.encode("testTW"));
		user.setName("TestTwUser");
		user.setLang("zh-tw");
		user.setActive(true);
		user.setAdmin(false);
		userDao.save(user);


		user = new User();
		user.setLoginId("testFalse");
		user.setPassword(bCryptPasswordEncoder.encode("testFalse"));
		user.setName("TestActiveFalseUser");
		user.setActive(false);
		user.setAdmin(false);
		userDao.save(user);

		// Series Link ダウンロードは用意。他はImport時に生成。
		// カタログ閲覧URLからカタログPDFURLを自動生成
		// /catalog/New-products/mpv/s100-83-LEY/index.html -> /catalog/New-products/mpv/s100-83-LEY/data/s100-83-LEY.pdf
		String[] slmJpIconTitleArr = {"カタログ閲覧", "貸出サービス", "モーターレス仕様","選定ソフト","アプリケーション例",
				"設定ソフトウェア","パッキンセット","エレメント交換案内","動画","IODD",
				"ロック解除ユニット", "オーダーメイド仕様"};
		// TODO IODD のcat_iodd以外のcat_ioddはダミー 3/4に作成依頼済み
		String[] slmJpIconClassArr = {"cat_pdf", "cat_service", "cat_motor","cat_soft","cat_appli",
				"cat_soft","cat_packing","cat_elem","cat_movie","cat_iodd",
				"cat_unlock","cat_info"};
		String[] slmJpDLArr = {"2DCAD", "取扱説明書", "マニホールド仕様書", "自己宣言書" }; // "カタログPDF"はカタログ閲覧で自動生成

		List<SeriesLinkMaster> slmJpList = new LinkedList<SeriesLinkMaster>();
		SeriesLinkMaster slm = new SeriesLinkMaster();
		int cnt = 0;
		for(; cnt < slmJpIconTitleArr.length; cnt++)
		{
			slm.setId(null);
			slm.setTitle(slmJpIconTitleArr[cnt]);
			slm.setName(slm.getTitle());
			slm.setType(SeriesLinkType.ICON);
			slm.setLang("ja-jp");
			slm.setIconClass(slmJpIconClassArr[cnt]);
			slm.setUser(adminUser);
			slm.setOrder(cnt);
			slm.setBlank(true);

			slm = sLinkMasterDao.save(slm);

			slmJpList.add(slm);
		}

		for(int j = 0; j < slmJpDLArr.length; j++) // cntは再利用 ImpSeriesで利用
		{
			slm.setId(null);
			String tmp = slmJpDLArr[j];
			if (j == 0) tmp +=" 以下、自己宣言書まではシステムで自動生成するので、文字が入っていれば大丈夫です。";
			slm.setTitle(tmp);
			slm.setName(slm.getTitle());
			slm.setType(SeriesLinkType.DOWNLOAD);
			slm.setLang("ja-jp");
			slm.setUser(adminUser);
			slm.setOrder(cnt);
			slm.setBlank(true);

			slm = sLinkMasterDao.save(slm);

			slmJpList.add(slm);
			cnt++;
		}

		// TODO とりあえず他言語も日本語 + 後ろにlang
		List<SeriesLinkMaster> slmList = sLinkMasterDao.findAllByLang("ja-jp", null);
		List<Lang> langList = langDao.listAll(null);
		cnt = 0;
		for(SeriesLinkMaster s : slmList) {
			String id = s.getId();
			String title = s.getTitle();
			for (int c = 1; c < langList.size(); c++)
			{
				String lng = langList.get(c).getName();
				s.setId(null);
				s.setTitle(title);
				s.setName(title+lng);
				s.setLangRefId(id);
				s.setLang(lng);
				sLinkMasterDao.save(s);
			}
		}

		// インポートが出来たので、ダミーは不要のためコメントアウト。Rootは必要。
		for(int c = 0; c < 2; c++) { // CategoryType

			CategoryType ct = CategoryType.CATALOG;
			if (c == 1) ct = CategoryType.OTHER;

			List<Lang> langList2 = langDao.findAll();
			String JpTestId = null;
			String JpProdId = null;
			String[][] langRefList = new String[11][16];
			String[][][] seriesLangRefList = new String[11][16][11];

			for (Lang lng : langList2) {

				String testId = createRootCategory(lng.getName(), JpTestId, ct);
				if (lng.getName().equals("ja-jp")) JpTestId = testId;
				String prodId =  createProdRootCategory(lng.getName(), testId, JpProdId, ct);
				if (lng.getName().equals("ja-jp")) JpProdId = prodId;

				/*
				Category root = catTemp.findRoot(lng.getName(), ModelState.TEST, ct);

				for (int i = 1; i <= 10; i++) {
					Category c1 = new Category();
					String prefix ="カテゴリ";
					if (lng.getName().equals("en-jp")) prefix = "Category";
					else if(lng.getName().equals("zh-cn")) prefix = "类别";
					else if(lng.getName().equals("zh-tw")) prefix = "類別";
					else if (lng.getName().indexOf("en-") > -1) prefix = "Category";
					c1.setName(prefix + i);
					c1.setSlug("c" + i);
					c1.setParentId(root.getId());
					c1.setType(ct);
					c1.setLang(lng.getName());
					if (lng.getName().equals("ja-jp") == false) c1.setLangRefId(langRefList[i][0]);

					c1.setOrder(i);
					if (i % 2 == 0) {
						c1.setActive(false);
					}
					c1.setUser(user);
					c1 = catDao.save(c1);

					if (lng.getName().equals("ja-jp")) langRefList[i][0] = c1.getId();

					//2層目
					for (int i2 = 1; i2 < 15; i2++) {
						Category c2 = new Category();
						c2.setName(prefix + i + "-" + i2);
						c2.setSlug("c" + i + "-" + i2);
						c2.setParentId(c1.getId());
						c2.setType(ct);
						c2.setLang(lng.getName());
						if (lng.getName().equals("ja-jp") == false) c2.setLangRefId(langRefList[i][i2]);

						c2.setOrder(i2);
						c2.setActive(true);
						c2.setUser(user);
						c2 = catDao.save(c2);
						if (lng.getName().equals("ja-jp")) langRefList[i][i2] = c2.getId();

						List<Series> seriesList = new ArrayList<Series>();
						CategorySeries cs = new CategorySeries();
						cs.setCategoryId(c2.getId());

						// create series
						String sPrefix ="シリーズ";
						if (lng.getName().equals("en-jp")) sPrefix = "Series";
						else if(lng.getName().equals("zh-cn")) sPrefix = "系列";
						else if(lng.getName().equals("zh-tw")) sPrefix = "系列";
						else if (lng.getName().indexOf("en-") > -1) prefix = "Series";
						for (int s = 1; s <= 10; s++) {
							Series se = new Series();
							se.setName(sPrefix + i + "-" + i2 + "--" + s);
							se.setLang(lng.getName());
							if (lng.getName().equals("ja-jp") == false) se.setLangRefId(seriesLangRefList[i][i2][s]);
							se.setActive(true);
							se = seriesDao.save(se);
							if (lng.getName().equals("ja-jp")) seriesLangRefList[i][i2][s] = se.getId();
							seriesList.add(se);
						}
						cs.setSeriesList(seriesList);
						csDao.save(cs);
					}
				}
*/
			}
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

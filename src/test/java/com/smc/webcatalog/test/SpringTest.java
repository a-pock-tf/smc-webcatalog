package com.smc.webcatalog.test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.smc.psitem.dao.PsItemTemplateImpl;
import com.smc.psitem.model.PsItem;
import com.smc.webcatalog.api.InternalApiRestController;
import com.smc.webcatalog.dao.CategoryRepository;
import com.smc.webcatalog.dao.CategorySeriesTemplateImpl;
import com.smc.webcatalog.dao.CategoryTemplateImpl;
import com.smc.webcatalog.dao.LangRepository;
import com.smc.webcatalog.dao.SeriesLinkMasterRepository;
import com.smc.webcatalog.dao.UserTemplateImpl;
import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.CategorySeries;
import com.smc.webcatalog.model.CategoryType;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.ImpCategoryTemplate;
import com.smc.webcatalog.model.Lang;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Series;
import com.smc.webcatalog.model.SeriesLinkMaster;
import com.smc.webcatalog.model.SeriesLinkType;
import com.smc.webcatalog.model.Template;
import com.smc.webcatalog.model.TemplateCategory;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.service.CategoryServiceImpl;
import com.smc.webcatalog.service.SeriesServiceImpl;
import com.smc.webcatalog.service.TemplateCategoryServiceImpl;
import com.smc.webcatalog.service.TemplateServiceImpl;
import com.smc.webcatalog.service.UserServiceImpl;
import com.smc.webcatalog.util.LibSynonyms;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@EnableAutoConfiguration(exclude = MongoAutoConfiguration.class)
@AutoConfigureMockMvc
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class
  })
@EnableAsync(proxyTargetClass = true)
//@Ignore
public class SpringTest {

	@Autowired
	private CategoryServiceImpl service;

	@Autowired
	private CategoryRepository cRepo;

	@Autowired
	private CategoryTemplateImpl cTemp;

	@Autowired
	private CategorySeriesTemplateImpl csTemp;

	@Autowired
	private SeriesServiceImpl sService;

	@Autowired
	private UserTemplateImpl uTemp;

	@Autowired
	private UserServiceImpl uService;

	@Autowired
	private TemplateServiceImpl templateService;

	@Autowired
	private TemplateCategoryServiceImpl templateCategoryService;

	@Autowired
	private LangRepository langDao;
	
	@Autowired
	private PsItemTemplateImpl psItemTemp;

	@Autowired
	private SeriesLinkMasterRepository sLinkMasterDao;

	@Before
    public void setup() {
    }


	//@Test
	public void getParentsTest() {
		log.info("getParentsTest() ---------------------");
		ErrorObject ret = new ErrorObject();
		List<Category> list = service.getParents("5d8dca074a5a8292c8783d66",  null, ret);
		for(Category _c:list) {
			 log.info(_c.getId()+":"+_c.getName()+":"+_c.getOrder());
		 }
		log.info("getParentsTest() ret=" + ret);
	}

	//@Test
	public void saveTest() {
		log.info("saveTest() ---------------------");
		Category c = new Category();
		c.setParentId("5d8dca074a5a8292c8783d66");
		c.setState(ModelState.TEST);
		c.setName("fuji");
		c.setSlug("fuji");
		ErrorObject res = service.save(c);

		log.info(c.getId()+":"+c.getName()+":"+c.getOrder() + " res="+res);
	}

	//@Test
	public void getSlugTest() {
		log.info("getSlugTest() ---------------------");
		List<Category> _c = cTemp.findBySlug("fuji", "ja-jp", ModelState.TEST, CategoryType.CATALOG, null);
		for(Category c : _c) {
			 log.info(c.getId()+":"+c.getName()+":"+c.getOrder());
		}
	}

	//@Test
	public void checkNameTest() {
		log.info("checkNameTest() ---------------------");
		ErrorObject res = new ErrorObject();
		Category c = service.get("5d8dca084a5a8292c8783d97", null);
		boolean ret = service.isNameExists(c, res);
		log.info("bool = " + ret + " res=" + res);
	}

	//@Test
	public void changeStateAllTest() {
		log.info("changeStateAllTest() ---------------------");
		Optional<Category> c = cTemp.findByName("カテゴリ2", "ja-jp", ModelState.TEST, CategoryType.CATALOG);
		Category ca = c.get();
		ErrorObject err = service.changeStateToProdAll(ca.getId(), ca.getUser());
		log.info(err.toString());
		// カテゴリツリーを取得し確認
		Category root = cTemp.findRoot( "ja-jp", ModelState.PROD, CategoryType.CATALOG);
		err = new ErrorObject();
		List<Category> ret = service.listAll(root.getId(), null, err);
		log.info(ret.toString());
		log.info(err.toString());
		for(Category _c:ret) {
			 log.info(_c.getId()+":"+_c.getName()+":"+_c.getOrder());
		}
	}

	//@Test
	public void moveTest() {
		log.info("moveTest() ---------------------");
		Optional<Category> c = cTemp.findByName("カテゴリ2-9", "ja-jp", ModelState.PROD, CategoryType.CATALOG);
		Optional<Category> c2 = cTemp.findByName("カテゴリ2-5", "ja-jp", ModelState.PROD, CategoryType.CATALOG);
		ErrorObject err = service.move(c.get().getId(), c2.get().getId(), true);
		log.info("moveTest() ret=" + err);
		// 以下、確認
		Category root = cTemp.findRoot( "ja-jp", ModelState.PROD, CategoryType.CATALOG);
		List<Category> list = service.listAll(root.getId(), null, err);
		for(Category _c:list) {
			 log.info(_c.getId()+":"+_c.getName()+":"+_c.getOrder());
		}
	}

	//@Test
	public void changeStateTest() {
		log.info("changeStateTest() ---------------------");
		Optional<Category> c = cTemp.findByName("カテゴリ2-9", "ja-jp", ModelState.TEST, CategoryType.CATALOG);
		ErrorObject err = service.changeStateToProd(c.get().getId());
		log.info(err.toString());
		// 以下、確認
		List<Category> ret = service.listAll(c.get().getId(), null, err);
		log.info(ret.toString());
		for(Category _c:ret) {
			 log.info(_c.getId()+":"+_c.getName()+":"+_c.getOrder());
		}
	}

	//@Test
	public void getFindChildTest() {

//		List<Category> ret = cTemp.findChild("5d2d3088060a8a61f8cef263", ModelState.TEST, null, list);
		List<Category> ret = cTemp.findChild("", ModelState.TEST, CategoryType.CATALOG, null);
		for(Category _c:ret) {
			 log.info(_c.getId()+":"+_c.getName()+":"+_c.getOrder());
		 }
	}

	//@Test
	public void getListAllTest() {
		log.info("getListAllTest() ---------------------");
		ErrorObject err = new ErrorObject();
		List<Category> ret = service.listAll("ja-jp", ModelState.TEST, CategoryType.CATALOG, err);
		log.info(ret.toString());
		log.info(err.toString());
		for(Category _c:ret) {
			 log.info(_c.getId()+":"+_c.getName()+":"+_c.getOrder());
		}
	}
	//@Test
	public void getIdListAllTest() {
		log.info("getIdListAllTest() ---------------------");
		ErrorObject err = new ErrorObject();
//		List<Category> ret = service.listAll("5d2d3088060a8a61f8cef260", ModelState.TEST, null);
		List<Category> ret = service.listAll("5d2d3088060a8a61f8cef261", ModelState.PROD, null, err);
		log.info(ret.toString());
		for(Category _c:ret) {
			 log.info(_c.getId()+":"+_c.getName()+":"+_c.getOrder());
		}
	}

	//@Test
	public void setTemplateCategory() {
		ErrorObject err = new ErrorObject();
		ImpCategoryTemplate categoryTemplate = new ImpCategoryTemplate();
/*		{
			String id = "6156a676d2491a3ab871f356";
			List<Category> ret = service.listAll(id, true, err);
			for (Category c : ret) {
				if (c.getParentId().equals(id)) {
		    		String hcTemplateID = categoryTemplate.getIdFromName(c.getName(), "ja-jp");
		    		if (hcTemplateID != null && hcTemplateID.isEmpty() == false) {
			    		TemplateCategory tc = templateCategoryService.getHeartCoreID(hcTemplateID, err);
			    		if (tc != null) {
			    			tc.setCategoryId(c.getId());
			    			templateCategoryService.save(tc);
			    		}
		    		}
		    		else {
		    			log.info("cate="+c.getName() + " id="+c.getId());
		    		}
				}
			}
		}*/
		{
			String id = "6156a6dcd2491a3ab8722378";
			List<Category> ret = service.listAll(id, true, err);
			for (Category c : ret) {
				if (c.getParentId().equals(id)) {
		    		String hcTemplateID = categoryTemplate.getIdFromName(c.getName(), "en-jp");
		    		if (hcTemplateID != null && hcTemplateID.isEmpty() == false) {
			    		TemplateCategory tc = templateCategoryService.getHeartCoreID(hcTemplateID, err);
			    		if (tc != null) {
			    			tc.setCategoryId(c.getId());
			    			templateCategoryService.save(tc);
			    		}
		    		}
		    		else {
		    			log.info("cate="+c.getName() + " id="+c.getId());
		    		}
				}
			}
    	}
	}

	//@Test
	public void deleteTest() {
		log.info("--------------------- deleteTest()");
		ErrorObject ret = service.delete("5d2d3088060a8a61f8cef33c");

		if (ret.isError())log.info("NG:" + ret.toString());
		else log.info("OK");
	}

	//@Test
	public void getSeriesListAllTest() {
		log.info("getSeriesListAllTest() ---------------------");
		ErrorObject err = new ErrorObject();
		List<Category> ret = service.listAll("5d8dca074a5a8292c8783d64", ModelState.TEST, null, err);
		log.info(ret.toString());
		for(Category _c:ret) {
			 log.info(_c.getId()+":"+_c.getName()+":"+_c.getOrder() + ":"+_c.getParentId());
			 Category c = service.getWithSeries(_c.getId(), null, err);
			 List<Series> list = c.getSeriesList();
			 for(Series _s:list) {
				 log.info(_s.getId()+":"+_s.getName()+":"+_s.getOrder());
			 }
		}
	}

	// -------------- Series --------------------
	@Autowired
    private MockMvc mockMvc;
	@Before
    public void before() throws Exception {
		mockMvc = MockMvcBuilders.standaloneSetup(new InternalApiRestController()).build();
    }
	//@Test
	public void seriesLinkMaster() // 間違って消してしまった。作成用。2021/9/23
	{
        try {
        	ErrorObject err = new ErrorObject();
        	User adminUser = uService.getFromLoginId("admin", err);
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
        } catch(Exception e) {

        }
	}
	//@Test
	public void moveImage()
	{
		String path = "C:/workspace/smc-webcatalog/src/main/resources/static/html/series/";
		ErrorObject err = new ErrorObject();
		for(int i = 2; i < 3; i++) {
			String lang = "ja-jp";
			if (i == 0) {}
			else if (i == 1) lang = "en-jp";
			else if (i == 2) lang = "zh-cn";
//			List<Series> list = sService.listAll(lang, ModelState.TEST, null, err);
			List<Series> list = sService.listAll(lang, ModelState.TEST, null, 0, err);
			for(Series s : list) {
				if (s.getImage() != null && s.getOldId() != null) {
					try {
						URL url = new URL("https://www.smcworld.com/upfiles/series/"+s.getOldId()+"/"+s.getImage());
						HttpURLConnection conn =
						          (HttpURLConnection) url.openConnection();
						      conn.setAllowUserInteraction(false);
						      conn.setInstanceFollowRedirects(true);
						      conn.setRequestMethod("GET");
						      conn.connect();
						      int httpStatusCode = conn.getResponseCode();
						      if (httpStatusCode != HttpURLConnection.HTTP_OK) {
						    	  log.info(err.getCode() + ":" + err.getMessage()+ ":" + s.getImage());
						      }
						      String contentType = conn.getContentType();
						      System.out.println("Content-Type: " + contentType);

						      // Input Stream
						      DataInputStream dataInStream
						          = new DataInputStream(
						          conn.getInputStream());

						      // Output Stream
						      DataOutputStream dataOutStream
						          = new DataOutputStream(
						          new BufferedOutputStream(
						              new FileOutputStream(path+lang+"/"+s.getImage())));

						      // Read Data
						      byte[] b = new byte[4096];
						      int readByte = 0;

						      while (-1 != (readByte = dataInStream.read(b))) {
						        dataOutStream.write(b, 0, readByte);
						      }

						      // Close Stream
						      dataInStream.close();
						      dataOutStream.close();

					} catch (IOException e) {
						log.info( "Exception:" + e.getMessage()+ ":" + s.getImage());
						e.printStackTrace();
					}
				} else {
					log.info( "Null:" + s.getImage()+ ":" + s.getOldId());
				}
			}
		}
	}
	// ========== cad3d ==========
	//@Test
	public void cad3dApi()
	{
        try {
        mockMvc.perform(
                post("/api/ja-jp/cad3d")
                .content(("id=JSY-P"))  // リクエストボディを指定
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE) // Content Typeを指定
        ).andExpect(status().isCreated());
        } catch(Exception e) {

        }
	}
	//@Test
	public void cad3dApiService()
	{
		List<String> list = new LinkedList<String>();
		list.add("JSY-P");
		list.add("CJ1");
		list.add("KFG2-E-C-F");
		ErrorObject err = new ErrorObject();
 		Template t = templateService.getLangAndModelState("ja-jp", ModelState.PROD, true, err);
 		List<Category> prod = service.listAll("ja-jp", ModelState.PROD, CategoryType.CATALOG, err);
		err = sService.cad3DUpdate(t, list, "ja-jp", prod);
	}

	//@Test
	public void cad3dApiServiceCSV()
	{
		try {
			BufferedReader br = null;
	        String score_csv = "src/main/resources/csv/cad3d_ja.csv";
	        File file = new File(score_csv);
	        FileInputStream input = new FileInputStream(file);
	        InputStreamReader stream = new InputStreamReader(input,"SJIS");
	        br = new BufferedReader(stream);
	        String line;
	        String[] data;
	        br.readLine();
	        List<String> list = new ArrayList<String>();
	        while ((line = br.readLine()) != null) {
	        	byte[] b = line.getBytes();
                line = new String(b, "UTF-8");
	            data = line.split(",");
	            String sid = data[9];
	            if (sid != null && sid.isEmpty() == false && list.contains(sid) == false) {
	            	list.add(sid);
	            }
	        }
	        br.close();

	        String ids = "";
	        for(String sid : list) {
	        	ids+="id="+sid+"&";
	        }
	        ids = ids.substring(0, ids.length()-1);
	        log.error(ids);
	        ErrorObject err = new ErrorObject();
	 		Template t = templateService.getLangAndModelState("ja-jp", ModelState.PROD, true, err);
	 		List<Category> prod = service.listAll("ja-jp", ModelState.PROD, CategoryType.CATALOG, err);
	 		String[] cond = {"TEST"};
			err = sService.cad3DUpdate(t, list, "ja-jp", prod);
		}catch(Exception e) {
			log.error(e.getMessage());
			log.error(e.getLocalizedMessage());
        }
	}


	//@Test
	public void cad3dApiCsv() {
		try {
			BufferedReader br = null;
	        String score_csv = "src/main/resources/csv/cad3d_ja.csv";
	        File file = new File(score_csv);
	        FileInputStream input = new FileInputStream(file);
	        InputStreamReader stream = new InputStreamReader(input,"SJIS");
	        br = new BufferedReader(stream);
	        String line;
	        String[] data;
	        br.readLine();
	        List<String> list = new ArrayList<String>();
	        while ((line = br.readLine()) != null) {
	        	byte[] b = line.getBytes();
                line = new String(b, "UTF-8");
	            data = line.split(",");
	            String sid = data[9];
	            if (sid != null && sid.isEmpty() == false && list.contains(sid) == false) {
	            	list.add(sid);
	            }
	        }
	        br.close();

	        String ids = "";
	        for(String sid : list) {
	        	ids+="id="+sid+"&";
	        }
	        ids = ids.substring(0, ids.length()-1);
	        log.error(ids);
	        mockMvc.perform(
	                post("/api/ja-jp/cad3d")
	                .content((ids))  // リクエストボディを指定
	                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE) // Content Typeを指定
	        ).andExpect(status().isCreated());
		} catch(Exception e) {
			log.error(e.getMessage());
			log.error(e.getLocalizedMessage());
        }
	}
	//@Test
	public void findCategorySeriesTest() {
		log.info("--------------------- findCategorySeriesTest()");
		List<CategorySeries> list = csTemp.findBySeriesId("5d92d6554a5a82ca94de3d32");

		for(CategorySeries _c:list) {
			 log.info(_c.getId()+":"+_c.getCategoryId()+":"+_c.getSeriesList());
		}
	}

	//@Test
	public void changeToProdSeriesTest() {
		log.info("--------------------- changeToProdSeriesTest()");
		ErrorObject err = new ErrorObject();
		User u = new User();

		Series s = sService.get("5d92d6554a5a82ca94de3d36", err);
		log.info(err.toString());

		err = sService.changeStateToProd(s.getId(), u);
		log.info(err.toString());

		List<Series> list = sService.listAll(s.getLang(), ModelState.PROD, null, 0, err);
		for(Series _c:list) {
			 log.info(_c.getId()+":"+_c.getLang()+":"+_c.getName());
		}
	}

	// -------------- User --------------------
	//@Test
	public void findAllUserTest() {
		log.info("--------------------- findAllUserTest()");
		ErrorObject err = new ErrorObject();
//		List<User> list = uService.listAll(null,err );
		List<User> list = uService.listAll(true,err );
		log.info(err.toString());
		for(User _c:list) {
			 log.info(_c.getId()+":"+_c.getLoginId()+":"+_c.getPassword());
		}
	}

	//  -------------- remote --------------------

	//@Test
	public void findHostTest() {
		log.info("--------------------- findHostTest()");
		ErrorObject err = new ErrorObject();

		for(int i = 0; i < 10; i++) {
			InetAddress addr;
			try {
				addr = InetAddress.getByName("www.smc3s.com");
		    	log.info("cnt"+i+":"+addr.getHostAddress());
			} catch (UnknownHostException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
	}
	
	// -------------- Search --------------------
	//@Test
	public void findPsItemTest() {
		log.error("--------------------- findPsItemTest()");
		ErrorObject err = new ErrorObject();
		
		for(int i = 0; i < 10; i++) {
			String[] arr = {"CUJ CU CUK", "アジャスタ 調整用部品 調節器", ""};
			try {
				List<PsItem> list = psItemTemp.searchAndOr(arr, "ja-jp", "1", 10,  true);
				int j = 0;
				for(PsItem item : list) {
					log.error("cnt"+i+":"+j+":"+item.getSid()+item.getName()+item.getSeries());
					j++;
				}
			} catch (Exception e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
	}
	
	//@Test
	public void findSynonymsTest() {
		log.error("--------------------- findSynonymsTest()");
		LibSynonyms lib = new LibSynonyms();
		String[] kwArr = {"バルブ", "高温"};
//		String[] kwArr = {"バルブ"};
		lib.getSynonyms(kwArr, "ja-jp");
	}
}

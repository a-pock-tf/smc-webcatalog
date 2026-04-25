package com.smc.webcatalog.test;

import static org.junit.Assert.*;
import static org.springframework.data.mongodb.core.query.Criteria.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.client.MongoClients;
import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.ImpCategorySlug;
import com.smc.webcatalog.model.ImpCategorySlug.CategorySlug;
import com.smc.webcatalog.model.Series;
import com.smc.webcatalog.util.LibSynonyms;

import lombok.extern.slf4j.Slf4j;

/**
 * Spring外で実行できるテスト
 * MongoTemplateの練習用
 * XXX Repositoryが使えると良いが....
 * @author miyasit
 *
 */
@Slf4j
public class UnitTest{

	private MongoOperations db;

	@Before
	public void setup() {
		com.mongodb.client.MongoClient mongoClient = MongoClients.create("mongodb://localhost/");
		db = new MongoTemplate(mongoClient, "smc-webcatalog");
	}

	//@Test
	//@Ignore
	 public void クエリーあれこれ() {

		 //動的にwhereを追加
		 Query q = new Query(where("name").is("root"));
		 q.addCriteria(where("lang").is("ja-jp"));
		 q.addCriteria(where("active").is(true));

		 List<Category> list = db.find(q, Category.class);
		 print(list,"ダイナミッククエリ");
		 assertTrue(list.size()==1);

		 //コレクションで検索(or 条件)
		 List<String> ids = new ArrayList<String>();
		 ids.add("5d1349554f17c51e10145af0");
		 ids.add("5d1349554f17c51e10145aee");
		 ids.add("5d1349554f17c51e10145aef");

		 q = new Query(where("id").in(ids));
		 list = db.find(q, Category.class);

		 log.info("----------result:"+ids.toString());

		 for(Category _c:list) {
			 log.info(_c.getId()+":"+_c.getName()+":"+_c.getOrder());
		 }


	}

	//@Test
	public void シリーズがカテゴリ内のArrayの順で取得できるか() {



		log.info("シリーズがカテゴリ内のArrayの順で取得できるか------------------------------------------");
		Query q = new Query(where("name").is("カテゴリ1-1")) ;
		List<Category> list = db.find(q, Category.class);

		//配列の順番を変える
		Category c = list.get(0);
		Collections.swap(c.getSeriesList(), 0, 1);
		int count =0;
		for(Series _s:c.getSeriesList()) {
			log.info(_s.getName());
			count ++;
			//シリーズ名をいじる
			/*
			_s.setName(_s.getName()+" *");
			if(count<5) {
				db.save(_s);
			}else {
				db.remove(_s);
			}
			*/
		}
		c.setCtime(new Date());
		db.save(c);

		q = new Query(where("name").is("カテゴリ1-1")) ;
		list = db.find(q, Category.class);

		for(Category _c:list) {
			for(Series _s:_c.getSeriesList()) {
				log.info(_s.getName());
			}
		}
	}

	//@Test
	public void 集約テスト() {

		LookupOperation lookupOperation = LookupOperation.newLookup().
	            from("series").
	            localField("seriesIds").
	            foreignField("_id").
	            as("series");

		Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(Criteria.where("_id").is("5d13fdf24f17c518c0723871")) , lookupOperation);
	    List<Category> results = db.aggregate(aggregation, "category", Category.class).getMappedResults();
	    log.info("Obj Size " +results.size());

	    for(Category _c:results) {
	    	log.info(_c.getName());
	    	for(Series _s:_c.getSeriesList()) {
	    		log.info(_s.getName());
	    	}
	    }


	}


	// -------------- Offline --------------------
	//@Test
	public void replaceOfflineTest2() {
		log.info("--------------------- replaceOfflineTest()");
		String str = "<ul class=\"pro_details_text\">\r\n" +
				"    入力・出力対応<br><br>※シリーズにより適用プロトコルは異なります。<br> 　  詳細は各シリーズのカタログをご参照ください。<br><br><a href=\"/products/ja/s.do?ca_id=1341\" target=\"_blank\" class=\"tx_link_b\">　▶IO-Linkデバイス機器</a> \r\n" +
				"<a href=\"/products/ja/s.do?ca_id=1341\" target=\"_blank\" class=\"tx_link_b\">　▶IO-Linkデバイス機器</a>"+
				"</ul>";

		String ret = "";
		if (str.indexOf("<a") > -1) {
			str = str.replaceAll("</a>", "");
			String[] arr = str.split("<a");
			if (arr != null && arr.length > 1) {
				int cnt = 0;
				for(String s : arr) {
					if (cnt == 0) ret += s;
					else {
						int end = s.indexOf(">");
						if (end > -1) {
							ret+=s.substring(end+1);
						}
					}
					cnt++;
				}
			}
		} else {
			ret = str;
		}
		log.info(ret);
	}
	//@Test
	public void replaceOfflineTest() {
		log.info("--------------------- replaceOfflineTest()");
		String link = "<img src=\"/assets/newproducts/ja-jp/jsy5000-h/images/10.jpg\" usemap=\"#ImageMap\" alt=\"\" height=\"1687\" width=\"745\" border=\"0\"/>\r\n" ;
		String key = "src=\"";
		String quote = "\"";
		int start = link.indexOf(key);
		int end = link.indexOf(quote, start+key.length());

		String url = link.substring(start+key.length(), end); // /webcatalog/en-jp/ -> webcatalog/en-jp/
		log.info(url);
	}

	//@Test
	public void スラッグテスト() {
		ImpCategorySlug slug = new ImpCategorySlug();
		 List<CategorySlug> list = slug.get();
		 for(CategorySlug c : list) {
			 log.info(c.getName() + ":" + c.getSlug() + ":" + c.isLarge());
		 }
	}
	//@Test
	public void findSynonymsTest() {
		log.error("--------------------- findSynonymsTest()");
		LibSynonyms lib = new LibSynonyms();
		String[] kwArr = {"バルブ", "高温"};
		lib.getSynonyms(kwArr, "ja-jp");
	}
	private void print(List<Category> list,String name) {
		 log.info("----------result:"+name);
		 for(Category _c:list) {
			 log.info(_c.toString());
		 }
	}


}

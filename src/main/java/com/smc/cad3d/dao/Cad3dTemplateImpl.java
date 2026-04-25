package com.smc.cad3d.dao;

import static org.springframework.data.mongodb.core.query.Criteria.*;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.smc.cad3d.model.Cad3d;

/***
 * MongoRepositoryで足りないものはこちらで
 * @author miyasit
 *
 */
public class Cad3dTemplateImpl implements Cad3dTemplate {

	@Autowired
	private MongoTemplate db;

	@Override
	public List<Cad3d> listAll(String lang, Boolean active) {
		Query query = new Query();
		if (active != null) {
			addActiveQuery(query, active);
		}
		List<Cad3d> list = db.find(query, Cad3d.class);
		return list;
	}

	@Override
	public List<Cad3d> search(List<String> keyList, String c1c2, String series, String lang, Boolean active) {
		Query query = new Query();
		String keyword = "";
		for(String key : keyList) {
				keyword += key.trim() + "|";
		}
		if (keyword.indexOf("□") > -1) {
			keyword = keyword.replaceAll("□", ".*");
		}
		keyword = keyword.substring(0, keyword.length()-1);
		query.addCriteria(where("query").ne("") // ID無し（query="")は除外
				.orOperator(where("series").regex(keyword, "i"),
						where("name").regex(keyword, "i"),
						where("item").regex(keyword, "i"))
				);
		addQuery(query, c1c2, series, lang, active);
		List<Cad3d> list = db.find(query, Cad3d.class);

		if (keyList.size() == 1) {
			// あいまい検索
			String k = keyList.get(0);
			char c = k.charAt(0);
			query = new Query();
			query.addCriteria(where("query").ne("") // ID無し（query="")は除外
					.orOperator(where("item").regex("^"+c+".*□.*", "i"))
							);
			addQuery(query, c1c2, series, lang, active);
			List<Cad3d> temp = db.find(query, Cad3d.class);
			if (temp != null && temp.size() > 0) {
				for(Cad3d item:temp) {
					if (list != null) {
						boolean isSame = false;
						for ( Cad3d it : list) {
							if(it.getId().equals(item.getId())) {
								isSame = true;
							}
						}
						if (isSame) continue;
					}
					// item に LEFS□F が格納 -> LEFS16F というキーワードでヒットさせたい
					// □の前後に分解し、前後が同じかどうか。
					// □は3文字分有効
					String str = item.getItem();
					String[] arr = str.split("□");
					if (arr.length == 1) { // １番後ろが□の場合
						if (k.indexOf(arr[0]) == 0 && k.length() <= arr[0].length()+3) {
							list.add(item);
						}
					} else if (arr.length == 2) {
						if (arr[1].isEmpty() == false) {
							if (str.lastIndexOf("□") == str.length()-1) { // □が２個で、１番後ろが□の場合
								if (k.indexOf(arr[0]) == 0 && k.indexOf(arr[1]) > arr[0].length() && k.length() < (arr[0].length()+3+arr[1].length()+2)) {
									list.add(item);
								}
							} else if (k.indexOf(arr[0]) == 0 && k.lastIndexOf(arr[1]) == k.length()-arr[1].length() && k.length() < (arr[0].length()+2+arr[1].length()+3)) {
								list.add(item);
							}
						} else if (k.indexOf(arr[0]) == 0 && k.length() < arr[0].length()+3) {
							list.add(item);
						}
					} else if (arr.length == 3) {
						if (arr[1] == null || arr[1].isEmpty()) {
							if (k.indexOf(arr[0]) == 0 && k.lastIndexOf(arr[2]) == k.length()-arr[2].length() && k.length() <= str.length()+6) {
								list.add(item);
							}
						} else if (arr[1] != null && arr[1].equals("-")) { // SY30M-□-□-□のように-□が連続している場合
							String[] kList = k.split("-");
							if (kList[0].equals(arr[0].replace("-", "")) && kList.length > 1 && kList[1].length() <= 3) {
								list.add(item);
							}
						} else if (arr[1] != null && arr[1].equals("-") && arr[2] != null && arr[2].equals("-")) {
							String[] kList = k.split("-");
							if (kList.length > 2 && kList[0].equals(arr[0].replace("-", "")) && kList[1].length() <= 3 && kList[2].length() <= 3) {
								list.add(item);
							} else if (kList.length > 3 && kList[0].equals(arr[0].replace("-", "")) && kList[1].length() <= 3 && kList[2].length() <= 3 && kList[3].length() <= 3) {
								list.add(item);
							}
						} else if (k.indexOf(arr[0]) == 0 && k.indexOf(arr[1]) > 1 && k.lastIndexOf(arr[2]) == k.length()-arr[2].length() && k.length() < str.length()+6) {
							list.add(item);
						}
					}
				}
			}
		}
		return list;
	}

	@Override
	public List<Cad3d> searchIndex(String index, String c1c2, String series, String lang, Boolean active) {
		Query query = new Query();
		query.addCriteria(where("query").ne(""));
		if (series == null || series.isEmpty()) {
			query.addCriteria(where("series").regex(index, "i"));
		}
		addQuery(query, c1c2, series, lang, active);
		List<Cad3d> list = db.find(query, Cad3d.class);
		return list;
	}

	@Override
	public List<Cad3d> searchUrl(String url, String lang, Boolean active) {
		Query query = new Query();

		String backslash = url.replace("/", "\\/");
		query.addCriteria(where("url1").regex(".*"+backslash+".*"));
		if (lang != null && lang.isEmpty() == false) {
			query.addCriteria(where("lang").is(lang));
		}
		if (active != null) {
			query.addCriteria(where("active").is(active));
		}

		List<Cad3d> list = db.find(query, Cad3d.class);
		return list;
	}

	@Override
	public List<Cad3d> search(String sid, String series, String lang, String cat) {
		Query query = new Query();
		query.addCriteria(where("ids").regex(sid));
		if (series != null && series.isEmpty() == false) {
			query.addCriteria(where("series").is(series));
		}
		if (lang != null && lang.isEmpty() == false) {
			query.addCriteria(where("lang").is(lang));
		}
		if (cat != null && cat.isEmpty() == false) {
			query.addCriteria(where("cat").is(cat));
		}
		query.with(Sort.by(Sort.Direction.ASC, "num"));
		List<Cad3d> list = db.find(query, Cad3d.class);
		return list;
	}


	// =================== private ===================
	private void addQuery(Query query, String c1c2, String series, String lang, Boolean active) {
		if (c1c2 != null && c1c2.isEmpty() == false) {
			query.addCriteria(where("c1c2").is(c1c2));
		}
		if (series != null && series.isEmpty() == false) {
			query.addCriteria(where("series").is(series));
		}
		if (lang != null && lang.isEmpty() == false) {
			query.addCriteria(where("lang").is(lang));
		}
		if (active != null) {
			query.addCriteria(where("active").is(active));
		}

	}
	// active の検索を付与
	private void addActiveQuery(Query q, Boolean active) {
		if (active != null) {
			q.addCriteria(where("active").is(active));
		}
	}


}

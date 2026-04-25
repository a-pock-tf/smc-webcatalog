package com.smc.psitem.dao;

import static org.springframework.data.mongodb.core.query.Criteria.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.smc.psitem.model.PsItem;

/***
 * MongoRepositoryで足りないものはこちらで
 * @author miyasit
 *
 */
public class PsItemTemplateImpl implements PsItemTemplate {

	@Autowired
	private MongoTemplate db;

	@Override
	public List<PsItem> listAll(String lang, Boolean active) {
		Query query = new Query();
		if (active != null) {
			addActiveQuery(query, active);
		}
		List<PsItem> list = db.find(query, PsItem.class);
		return list;
	}

	@Override
	public List<PsItem> searchAndOr(String[] kwArr, String lang, String cd, int max, Boolean active) {
		List<PsItem> ret = null;
		List<Criteria> cliList = getKwArrCriteriaList(kwArr, cd);
		
		if (cliList.size() > 0) {
			if (lang != null && lang.isEmpty() == false) {
				cliList.add(where("lang").is(lang));
			}
			if (active != null) {
				cliList.add(where("active").is(active));
			}
			cliList.add(where("query").ne("")); // ID無し（query="")は除外

			Criteria cri = new Criteria(); // criは毎回newしないとaddされる。
			Query query = new Query(cri.andOperator(cliList));
			if (max > 0) {
				query.limit(max);
			}
			ret = db.find(query, PsItem.class);
			if ((ret == null || ret.size() == 0) && kwArr.length == 1 ) {
				// 検索結果が無くて、kwが１つならあいまい検索。
				String[] arr = kwArr[0].split(" ");
				if (arr.length == 1) {
					ret = new ArrayList<>();
					addAboutList(ret, arr[0], null, null, lang, active);
				}
			}

		}
		return ret;
	}
	// 同義語の場合、kwArr１つの中で半角スペース区切りでor
	// cd=1の場合、kwArr[0]は^ kwArr[1]は.*
	private List<Criteria> getKwArrCriteriaList(String[] kwArr, String cd) {
		List<Criteria> cliList = new ArrayList<>();
		
		int cnt = 0;
		for(String key : kwArr) {
			String[] arr = key.split(" ");
			List<Criteria> orList = new ArrayList<>();
			for(String k : arr) {
				if (k != null && k.trim().isEmpty() == false) {
					if (k.indexOf("□") > -1) {
						k = k.replaceAll("□", ".*");
					}
					String kwQuery = ".*"+k+".*";
					if (cd != null && cd.equals("1") && cnt == 0) kwQuery = "^" + k + ".*";
					orList.add(where("series").regex(kwQuery, "i"));
					orList.add(where("name").regex(kwQuery, "i"));
					orList.add(where("item").regex(kwQuery, "i"));
				}
			}
			if (orList.size() > 0) {
				Criteria cri = new Criteria();
				cliList.add(cri.orOperator(orList));
			}
			cnt++;
		}
		
		if (cliList.size() > 0) {
			cliList.add(where("query").ne("")); // ID無し（query="")は除外
		}
		return cliList;
	}
	@Override
	public long searchAndOrCount(String[] kwArr, String lang, String cd, Boolean active) {
		long ret = -1;
		List<Criteria> cliList = getKwArrCriteriaList(kwArr, cd);
		if (cliList.size() > 0) {
			if (lang != null && lang.isEmpty() == false) {
				cliList.add(where("lang").is(lang));
			}
			if (active != null) {
				cliList.add(where("active").is(active));
			}
			cliList.add(where("query").ne("")); // ID無し（query="")は除外

			Criteria cri = new Criteria(); // criは毎回newしないとaddされる。
			Query query = new Query(cri.andOperator(cliList));

			ret = db.count(query, PsItem.class);
			if ((ret == 0) && kwArr.length == 1 ) {
				// 検索結果が無くて、kwが１つならあいまい検索。
				String[] arr = kwArr[0].split(" ");
				if (arr.length == 1) {
					List<PsItem> list = new ArrayList<>();
					addAboutList(list, arr[0], null, null, lang, active);
					ret = list.size();
				}
			}
		}
		return ret;
	}
	static Pattern regex_AlphaNum = Pattern.compile("^[A-Za-z0-9-_]+$") ; // 半角英数字のみ 
	@Override
	public List<PsItem> search(List<String> keyList, String condition, String c1c2, String series, String lang, Boolean active) 
	{
		List<PsItem> ret = null;
		List<Criteria> cliList = new ArrayList<>();
		
		for(String key : keyList) {
			String[] arr = key.split(" ");
			int cnt = 0;
			List<Criteria> orList = new ArrayList<>();
			for(String k : arr) {
				if (k != null && k.trim().isEmpty() == false) {
					if (k.indexOf("□") > -1) {
						k = k.replaceAll("□", ".*");
					}
					String kwQuery = ".*"+k+".*";
					if (condition != null && condition.equals("1") && cnt == 0) kwQuery = "^" + k + ".*";
					orList.add(where("series").regex(kwQuery, "i"));
					orList.add(where("name").regex(kwQuery, "i"));
					orList.add(where("item").regex(kwQuery, "i"));
					cnt++;
				}
			}
			if (orList.size() > 0) {
				Criteria cri = new Criteria();
				cliList.add(cri.orOperator(orList));
			}
		}
		if (cliList.size() > 0) {
			cliList.add(where("query").ne("")); // ID無し（query="")は除外

			Criteria cri = new Criteria(); // criは毎回newしないとaddされる。
			Query query = new Query(cri.andOperator(cliList));
			addQuery(query, c1c2, series, lang, active);

			ret = db.find(query, PsItem.class);
			if ((ret == null || ret.size() == 0) && keyList.size() == 1 ) {
				// 検索結果が無くて、kwが１つならあいまい検索。
				String[] arr = keyList.get(0).split(" ");
				if (arr.length == 1) {
					ret = new ArrayList<>();
					addAboutList(ret, arr[0], null, null, lang, active);
				}
			}

		}
		// キーワード１つ、英数字のみ
		if ((ret == null || ret.size() == 0) && keyList.size() == 1 ) {
			java.util.regex.Matcher m = regex_AlphaNum.matcher(keyList.get(0));
			if (m.matches()) {
				// あいまい検索
				ret = new ArrayList<>();
				addAboutList(ret, keyList.get(0), c1c2, series, lang, active);
			}
		}

		return ret;
	}

	@Override
	public List<PsItem> searchIndex(String index, String c1c2, String series, String lang, Boolean active) {
		Query query = new Query();
		query.addCriteria(where("query").ne(""));
		if (series == null || series.isEmpty()) {
			query.addCriteria(where("series").regex(index, "i"));
		}
		addQuery(query, c1c2, series, lang, active);
		List<PsItem> list = db.find(query, PsItem.class);
		return list;
	}

	// =================== private ===================
	// あいまい検索□追加
	private void addAboutList(List<PsItem> list, String k, String c1c2, String series, String lang, Boolean active) {
		
		// キーワードの先頭１文字と□を含むものを検索
		char c = k.charAt(0);
		Query query = new Query();
		query.addCriteria(where("query").ne("") // ID無し（query="")は除外
				.orOperator(where("item").regex("^"+c+".*□.*", "i"))
				);
		addQuery(query, c1c2, series, lang, active);

		List<PsItem> temp = db.find(query, PsItem.class);
		if (temp != null && temp.size() > 0) {
			for(PsItem item:temp) {
				if (list != null) {
					boolean isSame = false;
					for ( PsItem it : list) {
						if(it.getId().equals(item.getId())) {
							isSame = true;
						}
					}
					if (isSame) continue;
				}
				///// ===== 新処理。□は何個でも [0-9a-zA-Z]+ に置き換え 2023/11/27 =====
				String strItem = item.getItem();
				String regexItem = strItem.replaceAll("□", "[0-9a-zA-Z]+");
				if (k.matches(regexItem)) {
					list.add(item);
				}
				
				///// ===== 以下、旧処理。コメントアウト 2023/11/27 =====
				// item に LEFS□F が格納 -> LEFS16F というキーワードでヒットさせたい
				// □の前後に分解し、前後が同じかどうか。
				// □は3文字分有効
/*				String str = item.getItem();
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
*/
			}
		}
	}
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

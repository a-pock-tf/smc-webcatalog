package com.smc.webcatalog.service;

import java.util.List;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Series;
import com.smc.webcatalog.model.SeriesLink;
import com.smc.webcatalog.model.SeriesLinkMaster;
import com.smc.webcatalog.model.Template;
import com.smc.webcatalog.model.User;

@Service
@Scope("session")
@EnableCaching
public interface SeriesService {

	/**
	 *  save
	 * @param series OUT 新規追加後、IDを戻す。
	 * @memo 保存前にnameの重複チェック
	 * @return ErrorObject
	 */
	ErrorObject save(Series series);

	/**
	 * nameの重複チェック
	 * @param series
	 * @param err OUT falseでもエラーの場合もあるので、isError()で先に確認
	 * @return 同名が引数と同じ階層にあればtrue
	 */
	boolean isNameExists(Series series, ErrorObject err);

	/**
	 * modelNumberの重複チェック
	 * @param seid
	 * @param state
	 * @param err OUT falseでもエラーの場合もあるので、isError()で先に確認
	 * @return Series全体で同名があればtrue
	 */
	boolean isModelNumberExists(String seid, ModelState state, @Nullable Boolean active, ErrorObject err);

	/**
	 * modelNumberから取得
	 * @param seid
	 * @param state
	 * @param err OUT falseでもエラーの場合もあるので、isError()で先に確認
	 * @return
	 */
	Series getFromModelNumber(String seid, ModelState state, ErrorObject err);
	/**
	 *  get
	 * @param id
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	Series get(String id, ErrorObject err);


	/**
	 * CategorySeriesとCategory、SeriesLinkも取得
	 * @param id
	 * @param active
	 * @param err OUT
	 * @memo lang, state, typeはidのものを参照
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	Series getWithCategory(String id, @Nullable Boolean active, ErrorObject err);
	
	/**
	 * CategorySeriesを１つだけ検索して、そのcategoryIdを取得
	 * @return
	 */
	String getCategoryId(String seriesId, ErrorObject err);
	
	/**
	 * SeriesLinkも取得
	 * @param id
	 * @param active
	 * @param err OUT
	 * @memo LinkMasterIdにタイトルを設定。比較表示用
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	Series getWithLink(String id, @Nullable Boolean active, ErrorObject err);

	/**
	 * SeriesLinkのみ取得
	 * @param id
	 * @param err OUT
	 * @memo シリーズの一覧はあるけど、Linkが必要になった時用
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	List<SeriesLink> getLink(String id, ErrorObject err);

	/**
	 *  getStateRefId
	 * @param c
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	List<Series> getStateRefId(Series s, ModelState state, ErrorObject err);

	/**
	 *  getLangRefId
	 * @param c
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	Series getLangRefId(Series s, String lang, ErrorObject err);

	/**
	 * スラッグのシリーズ一覧
	 * @param lang
	 * @param slug
	 * @param active
	 * @param err
	 * @note PRODのみ。ユーザー表示用
	 * @return
	 */
	List<Series> listSlug(String lang, String slug, int level, Boolean active, ErrorObject err);

	/**
	 * スラッグのシリーズ一覧
	 * @param category ２階層なら２階層目のカテゴリ
	 * @param slug
	 * @param active
	 * @param err
	 * @note 管理画面用
	 * @return
	 */
	List<Series> listSlug(Category c, Boolean active, ErrorObject err);

	/**
	 * 各国の同一シリーズ一覧
	 * @param id
	 * @param err
	 * @return
	 */
	List<Series> listLangRef(String id, ErrorObject err);

	/**
	 * listAll
	 * @param lang
	 * @param state
	 * @param active
	 * @param err OUT
	 * @return
	 */
	List<Series> listAll200(String lang, ModelState state, Boolean active, ErrorObject err);

	List<Series> listAll(String lang, ModelState state, Boolean active, Integer limit, ErrorObject err);

	/**
	 * getPage WEBカタログサイト内検索
	 * @note only ModelState.PROD
	 *        only active = true
	 */
	List<Series> getPage(String[] kwArr, String lang, ModelState state, int page, int max, ErrorObject err);

	/**
	 * searchCount WEBカタログサイト内検索の件数取得
	 * @note only ModelState.PROD
	 *        only active = true
	 */
	int searchCount(String[] kwArr, String lang, ModelState state, ErrorObject err);

	/**
	 * Admin >> search
	 * @param lang
	 * @param state
	 * @param active
	 * @param err OUT
	 * @return
	 */
	List<Series> search(String keyword, String lang, ModelState state, @Nullable Boolean active, ErrorObject err);

	/**
	 * search
	 * @param lang
	 * @param state
	 * @param active
	 * @param err OUT
	 * @return
	 */
	List<Series> indexSearch(String h, String lang, ModelState state, @Nullable Boolean active, ErrorObject err);

	/**
	 * 同一階層内のソート
	 * @param idsのカテゴリid
	 * @param ids このidの順(List)にソート
	 * @memo idsに含まれないものがあれば最後尾に回す。
	 * @return ErrorObject
	 */
	ErrorObject sort(String id, List<String> ids);
	
	/**
	 *  該当idのstateをTESTからPRODへ変更
	 * @param id (ModelState.TEST)
	 * @return ErrorObject
	 */
	ErrorObject changeStateToProd(String id, User u);

	/**
	 *  該当idのstateをTESTからARCHIVEへ変更
	 * @param id (ModelState.TEST)
	 * @memo CategorySeriesは保持しない。
	 * @return ErrorObject
	 */
	ErrorObject changeStateToArchive(String id, User u);

	/**
	 *  該当idのstateをARCHIVEからTESTへ変更
	 * @param id (ModelState.ARCHIVE)
	 * @memo TESTのIDが有る場合はID以外の項目をコピー。
	 * TESTのIDが無い場合、ARCHIVEの情報を元にTESTを新規登録。ただし、同一nameチェックはする。
	 * CategorySeriesはARCHIVEに対し保持していないのでそのまま。
	 * @return ErrorObject
	 */
	ErrorObject changeStateToTest(String id);

	/**
	 *  該当idの削除後、CategorySeriesも削除。
	 * @param id (ModelState.TEST)
	 * @memo TESTの場合、PRODがすべてactive=falseになっていないと削除出来ない。ARCHIVEも削除
	 * @return ErrorObject
	 */
	ErrorObject checkDelete(String id);

	/**
	 *  該当idの削除後、CategorySeriesも削除。
	 * @param id (ModelState.TEST)
	 * @memo PRODがすべてactive=falseになっていないと削除出来ない。ARCHIVEも削除
	 * @return ErrorObject
	 */
	ErrorObject delete(String id);

	/**
	 * PROD削除時に表示用HTMLも削除
	 * @param s
	 * @return
	 */
	ErrorObject deleteHtml(Series s);

	/**
	 * Breadcrumbに入っているSlugを更新
	 * 大カテゴリが違えば同じSlugがあるので、ID必須
	 * @param id
	 * @param before
	 * @param after
	 * @return
	 */
	ErrorObject updateSlug(String id, String before, String after);

	/**
	 * SeriesLinkを削除してからInsert
	 * @param id
	 * @param master
	 * @param link 入力のあるもののみInsert
	 * @return
	 */
	ErrorObject linkUpsert(String id, List<SeriesLinkMaster> master, String[] link, ModelState state, User u);

	/**
	 * listで渡されたSIDのcad3dフラグをON、他はすべてOFF
	 * @param list
	 * @param lang
	 * @param prodList
	 * @return
	 */
	ErrorObject cad3DUpdate(Template t, List<String> list, String lang, List<Category> prodList);

	/**
	 * 該当言語のSeriesの重みをリセット
	 * @param lang
	 * @param state
	 * @param u
	 * @return
	 */
	ErrorObject resetOrder(String lang, ModelState state, User u);
	
	/**
	 * 配下のカテゴリに紐づくシリーズに重み付け
	 * @param list
	 * @param lang
	 * @param state
	 * @param u
	 * @return
	 */
	ErrorObject updateOrder(List<Category> list, String lang, ModelState state, User u);
	
	/**
	 * 全体検索
	 * @param lang
	 * @param state
	 * @param active
	 * @param err OUT
	 * @return
	 */
	List<Series> searchKeywordAndOr(String[] keyword, String lang, int limit, Boolean isProd, @Nullable Boolean active);
	long searchKeywordAndOrCount(String[] keyword, String lang, Boolean isProd, @Nullable Boolean active);

}

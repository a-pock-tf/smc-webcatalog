package com.smc.discontinued.service;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.smc.discontinued.model.DiscontinuedModelState;
import com.smc.discontinued.model.DiscontinuedSeries;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.User;

@Service
@Scope("session")
public interface DiscontinuedSeriesService {

	/**
	 *  save
	 * @param category OUT 新規追加後、IDを戻す。
	 * @memo save直前にもslugの重複チェックをかける。
	 * @return ErrorObject
	 */
	ErrorObject save(DiscontinuedSeries series);

	/**
	 *
	 * @param state
	 * @param type
	 * @param lang
	 * @param err OUT
	 * @memo active = trueのみ
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	List<DiscontinuedSeries> listAll(String lang, DiscontinuedModelState state, ErrorObject err);

	/**
	 *
	 * @param state
	 * @param type
	 * @param lang
	 * @param err OUT
	 * @memo active = trueのみ
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	List<DiscontinuedSeries> listAllSortByEndDate(String lang, DiscontinuedModelState state, boolean asc, ErrorObject err);

	/**
	 * カテゴリ配下のシリーズ取得
	 * @param categoryId
	 * @param err
	 * @return
	 */
	List<DiscontinuedSeries> listCategory(String categoryId, DiscontinuedModelState state, Boolean active, ErrorObject err);

	/**
	 * 言語のシリーズ取得
	 * @param lang
	 * @param err
	 * @return
	 */
	List<DiscontinuedSeries> listLang(String lang,Boolean active, ErrorObject err);

	/**
	 *  get
	 * @param id
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	DiscontinuedSeries get(String id, ErrorObject err);

	/**
	 *  get
	 * @param id
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	DiscontinuedSeries getSeriesId(String seriesId, DiscontinuedModelState state, ErrorObject err);

	/**
	 *  getStateRefId
	 * @param c
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	List<DiscontinuedSeries> getStateRefId(DiscontinuedSeries s, DiscontinuedModelState state, ErrorObject err);

	/**
	 *  getLangRefId
	 * @param c
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	DiscontinuedSeries getLangRefId(DiscontinuedSeries s, String lang, ErrorObject err);

	/**
	 * スラッグのシリーズ一覧
	 * @param lang
	 * @param slug
	 * @param active
	 * @param err
	 * @note PRODのみ。ユーザー表示用
	 * @return
	 */
	List<DiscontinuedSeries> listSlug(String lang, String slug, int level, Boolean active, ErrorObject err);
	/**
	 * 各国の同一シリーズ一覧
	 * @param id
	 * @param err
	 * @return
	 */
	List<DiscontinuedSeries> listLangRef(String id, ErrorObject err);

	/**
	 * search
	 * @param lang
	 * @param state
	 * @param active
	 * @param err OUT
	 * @return
	 */
	List<DiscontinuedSeries> search(String keyword, String lang, DiscontinuedModelState state, @Nullable Boolean active, ErrorObject err);
	
	/**
	 * searchHit
	 * @param lang
	 * @param state
	 * @param active
	 * @param err OUT
	 * @return
	 * @apiNote searchだと全件なので、１件のみ取得しHitの確認。countが不要ならlimit 1の方が高速!
	 */
	boolean hitSearch(String keyword, String lang, DiscontinuedModelState state, @Nullable Boolean active, ErrorObject err);

	/**
	 * indexSearch
	 * @param lang
	 * @param state
	 * @param active
	 * @param err OUT
	 * @return
	 */
	List<DiscontinuedSeries> indexSearch(String h, String lang, DiscontinuedModelState state, @Nullable Boolean active, ErrorObject err);
	
	/**
	 * indexSearchHit
	 * @param lang
	 * @param state
	 * @param active
	 * @param err OUT
	 * @return
	 * @apiNote searchだと全件なので、１件のみ取得しHitの確認。countが不要ならlimit 1の方が高速!
	 */
	boolean hitIndexSearch(String h, String lang, DiscontinuedModelState state, @Nullable Boolean active, ErrorObject err);

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
	ErrorObject changeStateToProd(String testid, String prodCategoryId,  User u);

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
}

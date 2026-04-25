package com.smc.discontinued.service;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.smc.discontinued.model.DiscontinuedCategory;
import com.smc.discontinued.model.DiscontinuedModelState;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.User;

@Service
@Scope("session")
public interface DiscontinuedCategoryService {

	/**
	 *  save
	 * @param category OUT 新規追加後、IDを戻す。
	 * @memo save直前にもslugの重複チェックをかける。
	 * @return ErrorObject
	 */
	ErrorObject save(DiscontinuedCategory category);

	/**
	 * slugの重複チェック
	 * @param slug
	 * @param lang
	 * @param state
	 * @param err OUT falseでもエラーの場合もあるので、isError()で先に確認
	 * @return 同じslugがあればtrue
	 * @see CategoryTypeを超えても同一SlugはNGにするためType不要
	 */
	boolean isSlugExists(String slug, String lang, DiscontinuedModelState state, @Nullable Boolean active,   ErrorObject err);

	/**
	 *  getStateRefId
	 * @param c
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	DiscontinuedCategory getStateRefId(DiscontinuedCategory c, DiscontinuedModelState state, ErrorObject err);

	/**
	 *  getLangRefId
	 * @param c
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	DiscontinuedCategory getLangRefId(DiscontinuedCategory c, String lang, ErrorObject err);

	/**
	 * ユーザー表示用。active=trueのみ
	 * @param state
	 * @param type
	 * @param lang
	 * @param err OUT
	 * @memo active = trueのみ
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	List<DiscontinuedCategory> listAllActive(String lang, DiscontinuedModelState state, ErrorObject err);

	/**
	 *  全カテゴリを階層構造で取得(親カテゴリ選択など、Contextに入れる)
	 * @param state
	 * @param type
	 * @param lang
	 * @param err OUT
	 * @memo active = trueのみ
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	List<DiscontinuedCategory> listAll(String lang, DiscontinuedModelState state, ErrorObject err);

	/**
	 * 各国の同一カテゴリ一覧
	 * @param id
	 * @param err
	 * @return
	 */
	List<DiscontinuedCategory> listLangRef(String id, ErrorObject err);

	/**
	 *  get
	 * @param id
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	DiscontinuedCategory get(String id, ErrorObject err);

	/**
	 *  get
	 * @param id
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	DiscontinuedCategory getSlug(String slug, String lang, DiscontinuedModelState state, ErrorObject err);

	/**
	 *  get
	 * @param id
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	DiscontinuedCategory getOldId(String oldId, String lang, DiscontinuedModelState state, ErrorObject err);

	/**
	 *  該当idのstateをTESTからPRODへ変更
	 * @param id (ModelState.TEST)
	 * @return ErrorObject
	 */
	ErrorObject changeStateToProd(String id, User u);

	/**
	 * 同一階層内のソート
	 * @param lang
	 * @param state
	 * @param ids このidの順(List)にソート
	 * @memo idsに含まれないものがあれば最後尾に回す。
	 * @return ErrorObject
	 */
	ErrorObject sort(String lang, DiscontinuedModelState state, List<String> ids);

	/**
	 * 同一階層内のシリーズをソート
	 * @param カテゴリid
	 * @param ids CategorySeriesのID。このidの順(List)にソート
	 * @memo idsに含まれないものがあれば最後尾に回す。
	 * @return ErrorObject
	 */
	ErrorObject sortSeries(String categoryid, List<String> ids);
	/**
	 * 該当id配下のCategoryをすべて削除
	 * @param id
	 * @memo TESTならPRODがすべてactive=falseになっていないと削除出来ない。PRODならactive=falseのみチェック。
	 * @return ErrorObject
	 */
	ErrorObject delete(String id);

	/**
	 * 削除前に削除可能かどうかのチェック。
	 * @param id
	 * @memo TESTならPRODがすべてactive=falseになっていないと削除出来ない。PRODならactive=falseのみチェック。
	 * @return ErrorObject
	 */
	ErrorObject checkDelete(String id);
}

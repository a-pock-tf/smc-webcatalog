package com.smc.webcatalog.service;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.CategoryType;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.User;

@Service
@Scope("session")
public interface CategoryService {

	/**
	 *  save
	 * @param category OUT 新規追加後、IDを戻す。
	 * @memo save直前にもslugの重複チェックをかける。
	 * @return ErrorObject
	 */
	ErrorObject save(Category category);

	/**
	 * slugの重複チェック
	 * @param category idの有無で新規・更新を判断
	 * @param err OUT falseでもエラーの場合もあるので、isError()で先に確認
	 * @return 同じslugがあればtrue
	 * @see 同じ親の子供同士での同一SlugはNGにする
	 */
	boolean isSlugExists(Category c, ErrorObject err);

	/**
	 * nameの重複チェック
	 * @param category idの有無で新規・更新を判断
	 * @param err OUT falseでもエラーの場合もあるので、isError()で先に確認
	 * @return 同名が引数と同じ階層にあればtrue
	 */
	boolean isNameExists(Category category, ErrorObject err);

	/**
	 *  get
	 * @param id
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	Category get(String id, ErrorObject err);

	/**
	 *  getRoot
	 * @param id
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	Category getRoot(String lang, ModelState state, CategoryType type, ErrorObject err);


	/**
	 *  getLang 言語の先頭のカテゴリを返す。
	 * @param id
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	Category getLang(String lang, ModelState state, CategoryType type, Boolean active, ErrorObject err);

	/**
	 *  getStateRefId
	 * @param c
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	Category getStateRefId(Category c, ModelState state, ErrorObject err);

	/**
	 *  getLangRefId
	 * @param c
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	Category getLangRefId(Category c, String lang, ErrorObject err);

	/**
	 * Categoryとシリーズも取得
	 * @param id
	 * @param active
	 * @param err OUT
	 * @memo lang, state, typeはidのものを参照。idがrootの場合、CategorySeriesの無いSeriesを戻す。
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	Category getWithSeries(String id, @Nullable Boolean active, ErrorObject err);

	// ユーザー系からみたら、stateとactiveのフィルタが必要
	Category getFromSlug(String slug, String lang, ModelState state, CategoryType type, int level, @Nullable Boolean active, ErrorObject err);

	// ２階層目
	Category getFromSlugSecond(String slug, String parentId, String lang, ModelState state, CategoryType type, @Nullable Boolean active, ErrorObject err);

	Category getFromOldId(String id, ModelState state, CategoryType type,@Nullable Boolean active, ErrorObject err);

	/**
	 *
	 * FIXME state,active 以外はフィルターする必要がない(idで呼ぶので)。state,activeをパラメータにしたのはセキュリティ上の理由
	 *
	 * 自分と配下のカテゴリ((1階層)を取得
	 * @param id
	 * @param active
	 * @param err OUT
	 * @memo lang, state, typeはidのものを参照。rootから必要な場合はlistAll()
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 *
	 */
	Category getWithChildren(String id, @Nullable Boolean active, ErrorObject err);

	/**
	 *
	 * FIXME idがnull or 空の場合、ルートカテゴリを取得
	 *
	 * ルートから該当idまでのリストを取得( 階層表示用 root -> カテゴリA -> カテゴリA-1 )
	 * @param id
	 * @param active
	 * @param err OUT
	 * @memo lang, state, typeはidのものを参照
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 * @return
	 */
	 List<Category> getParents(String id, @Nullable Boolean active, ErrorObject err);

	/**
	 *  該当idのstateをTESTからPRODへ変更
	 * @param id (ModelState.TEST)
	 * @memo active = true のみ変更。前後のIDが同じでなければエラー。子供が居る場合もエラー。
	 * @return ErrorObject
	 */
	ErrorObject changeStateToProd(String id);

	/**
	 *  該当id配下のstateをすべてTESTからPRODへ変更
	 *  PRODのSeriesが無ければ、PRODのSeries、CategorySeriesを作成
	 *  PRODのSeriesがあれば、CategorySeriesも作成
	 *
	 *  静的Htmlを作成。以下、フォルダ構成
	 *  /products/ja-jp/index.html
	 *  /products/ja-jp/slug0/index.html
	 *  /products/ja-jp/slug0/slug1/index.html(全部)
	 *  /products/ja-jp/series/se_id/index.html(全部) s.html(一部のみ）
	 * @param id (ModelState.TEST)
	 * @memo active = true のみ変更。TESTのPRODが有る場合はすべて削除してからUP
	 * @return ErrorObject
	 */
	ErrorObject changeStateToProdAll(String id, User u);

	/**
	 *  該当id配下のactiveをすべて変更
	 * @param id
	 * @param active
	 * @memo ARCHIVEは受け付けない
	 * @return ErrorObject
	 */
	ErrorObject changeActive(String id, Boolean active);

	/**
	 * カテゴリを移動(srcIdをdstIdの直後|直前に移動、階層も超えられる)
	 * @param srcId
	 * @param dstId
	 * @param true = 直後に移動, false = 直前に移動
	 * @memo activeは見ない。
	 * @return ErrorObject
	 */
	ErrorObject move(String srcId, String dstId, boolean append);

	/**
	 *  search
	 * @param state
	 * @param type
	 * @param lang
	 * @param active
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	List<Category> search(String keyword, String lang, ModelState state, CategoryType type, @Nullable Boolean active, ErrorObject err);

	/**
	 * 各国の同一カテゴリ一覧
	 * @param id
	 * @param err
	 * @return
	 */
	List<Category> listLangRef(String id, ErrorObject err);

	/**
	 *  全カテゴリを階層構造で取得(親カテゴリ選択など、Contextに入れる)
	 * @param state
	 * @param type
	 * @param lang
	 * @param err OUT
	 * @memo active = trueのみ
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	List<Category> listAll(String lang, ModelState state, CategoryType type, ErrorObject err);

	/**
	 *  該当id配下のすべての階層を取得
	 *  state,typeはidから取得
	 * @param id
	 * @param active
	 * @param err
	 * @return
	 */
	List<Category> listAll(String id, @Nullable Boolean active, ErrorObject err);

	/**
	 *  該当indexのすべての階層を取得
	 *  typeはOtherのみ
	 * @param index
	 * @param active
	 * @param err
	 * @return
	 */
	List<Category> listOtherAll(String index, String lang, ModelState state, @Nullable Boolean active, ErrorObject err);

	/**
	 *  シリーズIDからCategorySeriesを参照し、Category一覧を取得
	 * @param id
	 * @param active
	 * @param err
	 * @return
	 */
	List<Category> listCategoryFromSeries(String seriesId, @Nullable Boolean active, ErrorObject err);
	/**
	 * 管理画面用。浮いたカテゴリIDを削除
	 */
	List<Category> listCategoryFromSeriesWithCheck(String seriesId, ErrorObject err);

	/**
	 * 同一階層内のソート
	 * @param idsの親のid
	 * @param ids このidの順(List)にソート
	 * @memo idsに含まれないものがあれば最後尾に回す。
	 * @return ErrorObject
	 */
	ErrorObject sort(String id, List<String> ids);

	/**
	 * 同一階層内のシリーズをソート
	 * @param カテゴリid
	 * @param ids CategorySeriesのID。このidの順(List)にソート
	 * @memo idsに含まれないものがあれば最後尾に回す。
	 * @return ErrorObject
	 */
	ErrorObject sortSeries(String categoryid, List<String> ids);

	/**
	 * rootカテゴリを作成。新規Lang作成時
	 * @param lang
	 * @memo ModelState TEST PROD の両方を作成
	 * @return ErrorObject
	 */
	ErrorObject createRootCategory(String lang);

	/**
	 *  該当id配下のCategoryをすべて削除
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

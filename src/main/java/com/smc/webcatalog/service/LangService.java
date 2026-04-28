package com.smc.webcatalog.service;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.Lang;

@Service
@Scope("session")
public interface LangService {

	/**
	 *  save
	 * @param lang OUT 新規追加後、IDを戻す。
	 * @memo 保存前にnameの重複チェック
	 * @return ErrorObject
	 */
	ErrorObject save(Lang lang);

	/**
	 * nameの重複チェック
	 * @param lang
	 * @param err OUT falseでもエラーの場合もあるので、isError()で先に確認
	 * @return idがあれば自分は除外。
	 */
	boolean isNameExists(String lang, ErrorObject err);

	/**
	 * listAll
	 * @param err OUT
	 * @return
	 */
	List<Lang> listAll(Boolean active, ErrorObject err);

	/**
	 * listAllWithoutVersion
	 * @param err OUT
	 * @return
	 */
	List<Lang> listAllWithoutVersion(ErrorObject err);

	/**
	 *  get
	 * @param id
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	Lang get(String id, ErrorObject err);

	/**
	 *  getLang
	 * @param lang
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	Lang getLang(String lang, ErrorObject err);
	
	/**
	 * コンテキストから取得（メモリ上から読むので表示はこちらを使用すること）
	 * @param lang
	 * @return
	 */
	Lang getFromContext(String lang);
	
}

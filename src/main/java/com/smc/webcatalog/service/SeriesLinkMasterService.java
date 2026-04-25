package com.smc.webcatalog.service;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.SeriesLinkMaster;

@Service
@Scope("session")
public interface SeriesLinkMasterService {

	/**
	 *  save
	 * @param lang OUT 新規追加後、IDを戻す。
	 * @memo 保存前にnameの重複チェック
	 * @return ErrorObject
	 */
	ErrorObject save(SeriesLinkMaster master);

	/**
	 * nameの重複チェック
	 * @param lang
	 * @param err OUT falseでもエラーの場合もあるので、isError()で先に確認
	 * @return idがあれば自分は除外。
	 */
	boolean isNameExists(String name, String lang, ErrorObject err);

	/**
	 * listAll
	 * @param err OUT
	 * @return
	 */
	List<SeriesLinkMaster> listAll(Boolean active, ErrorObject err);

	/**
	 *
	 * @param lang
	 * @param active
	 * @param err
	 * @return
	 */
	List<SeriesLinkMaster> findByLangAll(String lang, Boolean active, ErrorObject err);

	/**
	 *  get
	 * @param id
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	SeriesLinkMaster get(String id, ErrorObject err);

}

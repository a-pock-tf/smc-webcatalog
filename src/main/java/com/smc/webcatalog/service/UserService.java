package com.smc.webcatalog.service;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.User;

@Service
@Scope("session")
public interface UserService {

	/**
	 *  save
	 * @param category OUT 新規追加後、IDを戻す。
	 * @memo save直前にもslugの重複チェックをかける。
	 * @return ErrorObject
	 */
	ErrorObject save(User user);

	/**
	 * emailの重複チェック
	 * @param email
	 * @param err OUT falseでもエラーの場合もあるので、isError()で先に確認
	 * @return 同じemailがあればtrue
	 */
	boolean isEmailExists(String email, ErrorObject err);

	/**
	 * nameの重複チェック
	 * @param name
	 * @param err OUT falseでもエラーの場合もあるので、isError()で先に確認
	 * @return 同じnameがあればtrue
	 */
	boolean isNameExists(String name, ErrorObject err);

	/**
	 * loginidの重複チェック
	 * @param loginid
	 * @param err OUT falseでもエラーの場合もあるので、isError()で先に確認
	 * @return 同じloginIdがあればtrue
	 */
	boolean isLoginIdExists(String loginid, ErrorObject err);

	/**
	 *  login
	 * @param loginid
	 * @param loginpw
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	User login(String loginid, String loginpw, ErrorObject err);

	/**
	 *  get
	 * @param id
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	User get(String id, ErrorObject err);

	/**
	 *  getFromLoginId LoginControllerのsuccess()loginSuccess時はIDのみ。
	 * @param id
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	User getFromLoginId(String loginId, ErrorObject err);

	/**
	 * listAll
	 * @param active
	 * @param err OUT
	 * @return
	 */
	List<User> listAll(Boolean active, ErrorObject err);

	/**
	 * update
	 * @param user
	 * @return ErrorObject
	 */
	ErrorObject update(User user);

	/**
	 * delete
	 * @param id
	 * @return ErrorObject
	 */
	ErrorObject delete(String id);
}

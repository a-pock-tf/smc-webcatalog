package com.smc.webcatalog.dao;

import java.util.List;
import java.util.Optional;

import com.smc.webcatalog.model.User;

/***
 * MongoRepositoryで足りないものはこちらで
 * @author miyasit
 *
 */
public interface UserTemplate {

	// Nameから検索
	Optional<User> findByLoginIdAndPassword(String id, String pw);

	List<User> listAll(Boolean active);
}

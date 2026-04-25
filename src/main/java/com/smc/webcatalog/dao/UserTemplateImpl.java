package com.smc.webcatalog.dao;

import static org.springframework.data.mongodb.core.query.Criteria.*;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.smc.webcatalog.model.User;

public class UserTemplateImpl implements UserTemplate{

	@Autowired
	private MongoTemplate db;

	@Override
	public Optional<User> findByLoginIdAndPassword(String id, String pw) {
		Query query = new Query();
		addLoginIdQuery(query, id);
		// パスワードをDecodeする必要があるのでIDのみで取得
		//addPasswordQuery(query, pw);
		addActiveQuery(query, true);

		List<User> list = db.find(query, User.class);
		User c = null;
		if (list != null && list.isEmpty() == false)
		{
			BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
			c = list.get(0);
			if (encoder.matches(pw, c.getPassword()) == false) {
				c = null;
			}
		}
		return Optional.ofNullable(c);
	}

	@Override
	public List<User> listAll(Boolean active) {
		Query query = new Query();
		if (active != null) {
			addActiveQuery(query, active);
		}
		List<User> list = db.find(query, User.class);
		return list;
	}

	// =================== private ===================
		// active の検索を付与
		private void addActiveQuery(Query q, Boolean active) {
			if (active != null) {
				q.addCriteria(where("active").is(active));
			}
		}

		private void addLoginIdQuery(Query q, String id) {
			if (id != null) {
				q.addCriteria(where("loginId").is(id));
			}
		}
		private void addPasswordQuery(Query q, String password) {
			if (password != null) {
				q.addCriteria(where("password").is(password));
			}
		}
}

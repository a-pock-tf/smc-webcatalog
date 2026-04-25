package com.smc.webcatalog.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.mongodb.MongoException;
import com.smc.exception.ModelExistsException;
import com.smc.exception.ModelNotFoundException;
import com.smc.webcatalog.config.ErrorCode;
import com.smc.webcatalog.dao.LangRepository;
import com.smc.webcatalog.dao.LangTemplateImpl;
import com.smc.webcatalog.dao.UserRepository;
import com.smc.webcatalog.dao.UserTemplateImpl;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.Lang;
import com.smc.webcatalog.model.User;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

	@Autowired
	UserRepository repo;

	@Autowired
	UserTemplateImpl temp;

	@Autowired
	LangRepository langRepo;

	@Autowired
	LangTemplateImpl langTemp;

	@Autowired
    HttpServletRequest req;

	@Override
	public ErrorObject save(User user) {
		ErrorObject ret = new ErrorObject();
		try {
			// 新規の場合
			if (StringUtils.isEmpty(user.getId())) {
				// 同email
				if (isEmailExists(user.getEmail(), ret)) throw new ModelExistsException("User email is exists.");
				else if (isLoginIdExists(user.getLoginId(), ret)) throw new ModelExistsException("User loginid is exists.");
				else if (ret.isError()) return ret;
				user.setId(null);
			} else {
				User u = get(user.getId(), ret);
				if (ret.isError()) return ret;

				// DBと違う場合、同じかどうか確認
				if (StringUtils.isEmpty(user.getEmail()) != StringUtils.isEmpty(u.getEmail()) && user.getEmail().equals(u.getEmail()) == false) {
					if (isEmailExists(user.getEmail(), ret)) throw new ModelExistsException("User email is exists.");
				}
				if (user.getLoginId().equals(u.getLoginId()) == false) {
					if (isLoginIdExists(user.getLoginId(), ret)) throw new ModelExistsException("User loginid is exists.");
				}
			}

			user.setMtime(new Date()); // Always update mtime

			user = repo.save(user);
			ret.setCount(1);

		} catch (ModelExistsException e) {
			ret.setCode(ErrorCode.E10001);
			ret.setMessage(e.getMessage());
		} catch (MongoException e) {
			ret.setCode(ErrorCode.E50001);
			ret.setMessage(e.getMessage());
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public boolean isEmailExists(String email, ErrorObject err) {
		boolean ret = false;
		try {
			if (StringUtils.isEmpty( email)) { // カラならチェックしない。
				ret = true;
			}
			else {
				Optional<User> os = repo.findByEmail(email);
				if (os.isPresent()) {
					ret = true;
				}
			}
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public boolean isNameExists(String name, ErrorObject err) {
		boolean ret = false;
		try {
			if (StringUtils.isEmpty( name)) { // カラならチェックしない。
				ret = true;
			}
			else {
				Optional<User> os = repo.findByName(name);
				if (os.isPresent()) {
					ret = true;
				}
			}
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public boolean isLoginIdExists(String loginid, ErrorObject err) {
		boolean ret = false;
		try {
			if (StringUtils.isEmpty(loginid)) { // カラならチェックしない。
				ret = true;
			}
			else {
				Optional<User> os = repo.findByLoginId(loginid);
				if (os.isPresent()) {
					ret = true;
				}
			}
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public User login(String loginid, String loginpw, ErrorObject err) {
		User ret = null;
		try {
			Optional<User> os = temp.findByLoginIdAndPassword(loginid, loginpw);
			if (os.isPresent()) {
				ret = os.get();

				// ログイン成功でLangのContextを確認
				Object obj = req.getServletContext().getAttribute(Lang.APPLICATION_CONTEXT_PREFIX);
				if (obj == null) {
					// Activeな言語を保持
					req.getServletContext().setAttribute(Lang.APPLICATION_CONTEXT_PREFIX,  langTemp.listAll(true));
				}
				obj = req.getServletContext().getAttribute(Lang.APPLICATION_CONTEXT_ALL_PREFIX);
				if (obj == null) {
					// Activeな言語を保持
					req.getServletContext().setAttribute(Lang.APPLICATION_CONTEXT_ALL_PREFIX,  langRepo.findAll());
				}

			}
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public User get(String id, ErrorObject err) {
		User ret = null;
		try {
			ret = repo.findById(id).orElseThrow(() -> new ModelNotFoundException("User.id=" + id));
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public User getFromLoginId(String loginId, ErrorObject err) {
		User ret = null;
		try {
			ret = repo.findByLoginId(loginId).orElseThrow(() -> new ModelNotFoundException("User.id=" + loginId));
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public List<User> listAll(Boolean active, ErrorObject err) {
		List<User> ret = null;
		try {
			ret = temp.listAll(active);
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public ErrorObject update(User user) {
		ErrorObject ret = new ErrorObject();
		try {
			// 新規の場合
			if (StringUtils.isEmpty(user.getId())) {
				throw new ModelNotFoundException("update() user.id is empty.");
			} else {
				User u = get(user.getId(), ret);
				if (ret.isError()) return ret;

				// DBと違う場合、同じかどうか確認
				if (u.getEmail().equals(user.getEmail()) == false) {
					if (isEmailExists(user.getEmail(), ret)) throw new ModelExistsException("User email is exists.");
				}
				if (u.getLoginId().equals(user.getLoginId()) == false) {
					if (isLoginIdExists(user.getLoginId(), ret)) throw new ModelExistsException("User loginid is exists.");
				}
			}
			user.setMtime(new Date()); // Always update mtime

			User res = repo.save(user);
			if (res == null) throw new MongoException("User save() return null.");
			ret.setCount(1);

		} catch (ModelNotFoundException e) {
			ret.setCode(ErrorCode.E10001);
			ret.setMessage(e.getMessage());
		} catch (MongoException e) {
			ret.setCode(ErrorCode.E50001);
			ret.setMessage(e.getMessage());
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public ErrorObject delete(String id) {
		ErrorObject ret = new ErrorObject();
		try {
			if (StringUtils.isEmpty(id)) {
				throw new ModelNotFoundException("delete() user.id is empty.");
			}

			repo.deleteById(id);
			ret.setCount(1);

		} catch (ModelNotFoundException e) {
			ret.setCode(ErrorCode.E10001);
			ret.setMessage(e.getMessage());
		} catch (MongoException e) {
			ret.setCode(ErrorCode.E50001);
			ret.setMessage(e.getMessage());
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
		}
		return ret;
	}

}

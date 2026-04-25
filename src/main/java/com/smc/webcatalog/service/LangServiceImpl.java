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
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.Lang;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LangServiceImpl implements LangService {

	@Autowired
	LangRepository repo;

	@Autowired
	LangTemplateImpl temp;

	@Autowired
    HttpServletRequest req;

	@Override
	public ErrorObject save(Lang lang) {
		ErrorObject ret = new ErrorObject();
		try {
			// 新規の場合
			if (StringUtils.isEmpty(lang.getId())) {
				// 同名チェック
				if (isNameExists(lang.getName(), ret)) throw new ModelExistsException("Lang name is exists.");
				else if (ret.isError()) return ret;
			} else {
				Lang s = get(lang.getId(), ret);
				if (ret.isError()) return ret;

				if (s.getName().equals(lang.getName()) == false) {
					if (isNameExists(lang.getName(), ret)) throw new ModelExistsException("Lang name is exists.");
				}
			}

			lang.setMtime(new Date()); // Always update mtime

			lang = repo.save(lang);

			// context update
			req.getServletContext().setAttribute(Lang.APPLICATION_CONTEXT_PREFIX, temp.listAll(true));
			req.getServletContext().setAttribute(Lang.APPLICATION_CONTEXT_ALL_PREFIX, repo.findAll());

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
	public boolean isNameExists(String lang, ErrorObject err) {
		boolean ret = false;
		try {
			Optional<Lang> os = repo.findByName(lang);
			if (os.isPresent()) {
				ret = true;
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

	@SuppressWarnings("unchecked")
	@Override
	public List<Lang> listAll(Boolean active, ErrorObject err) {
		List<Lang> ret = null;
		try {
			// 全部
			if (active == null) {
				Object obj = req.getServletContext().getAttribute(Lang.APPLICATION_CONTEXT_ALL_PREFIX);
				if (obj == null) {
					ret = repo.findAll();
					req.getServletContext().setAttribute(Lang.APPLICATION_CONTEXT_ALL_PREFIX, ret);
				}
				else {
					ret = (List<Lang>)obj;
				}
			// 表示のみ
			} else if (active) {
				Object obj = req.getServletContext().getAttribute(Lang.APPLICATION_CONTEXT_PREFIX);
				if (obj == null) {
					ret = temp.listAll(true);
					req.getServletContext().setAttribute(Lang.APPLICATION_CONTEXT_PREFIX, ret);
				}
				else {
					ret = (List<Lang>)obj;
				}
			// 非表示のみ
			} else {
				ret = temp.listAll(false);
			}
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

	@SuppressWarnings("unchecked")
	@Override
	public List<Lang> listAllWithoutVersion(ErrorObject err) {
		List<Lang> ret = null;
		try {
			Object obj = req.getServletContext().getAttribute(Lang.APPLICATION_CONTEXT_VIEW_PREFIX);
			if (obj == null) {
				ret = temp.listWithoutVersion();
				req.getServletContext().setAttribute(Lang.APPLICATION_CONTEXT_VIEW_PREFIX, ret);
			}
			else {
				ret = (List<Lang>)obj;
			}
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
	public Lang get(String id, ErrorObject err) {
		Lang ret = null;
		try {
			ret = repo.findById(id).orElseThrow(() -> new ModelNotFoundException("Lang.id=" + id));
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
	public Lang getLang(String lang, ErrorObject err) {
		Lang ret = null;
		try {
			ret = repo.findByName(lang).orElseThrow(() -> new ModelNotFoundException("getLang. lang=" + lang));
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

}

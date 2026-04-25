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
import com.smc.webcatalog.dao.SeriesLinkMasterRepository;
import com.smc.webcatalog.dao.SeriesLinkMasterTemplateImpl;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.SeriesLinkMaster;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SeriesLinkMasterServiceImpl implements SeriesLinkMasterService {

	@Autowired
	SeriesLinkMasterRepository repo;

	@Autowired
	SeriesLinkMasterTemplateImpl temp;

	@Autowired
    HttpServletRequest req;

	@Override
	public ErrorObject save(SeriesLinkMaster master) {
		ErrorObject ret = new ErrorObject();
		try {
			// 新規の場合
			if (StringUtils.isEmpty(master.getId())) {
				// 同名チェック
				if (isNameExists(master.getName(), master.getLang(),ret)) throw new ModelExistsException("SeriesLinkMaster name is exists.");
				else if (ret.isError()) return ret;
			} else {
				SeriesLinkMaster s = get(master.getId(), ret);
				if (ret.isError()) return ret;

				if (s.getName().equals(master.getName()) == false) {
					if (isNameExists(master.getName(), master.getLang(), ret)) throw new ModelExistsException("SeriesLinkMaster name is exists.");
				}
			}

			master.setMtime(new Date()); // Always update mtime

			master = repo.save(master);

			// context update
			req.getServletContext().setAttribute(SeriesLinkMaster.APPLICATION_CONTEXT_PREFIX, temp.listAll(true));
			req.getServletContext().setAttribute(SeriesLinkMaster.APPLICATION_CONTEXT_ALL_PREFIX, repo.findAll());

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
	public boolean isNameExists(String name, String lang, ErrorObject err) {
		boolean ret = false;
		try {
			Optional<SeriesLinkMaster> os = repo.findByNameAndLang(name, lang);
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

	@Override
	public List<SeriesLinkMaster> listAll(Boolean active, ErrorObject err) {
		List<SeriesLinkMaster> ret = null;
		try {
			// 全部
			if (active == null) {
				Object obj = req.getServletContext().getAttribute(SeriesLinkMaster.APPLICATION_CONTEXT_ALL_PREFIX);
				if (obj == null) {
					ret = repo.findAll();
					req.getServletContext().setAttribute(SeriesLinkMaster.APPLICATION_CONTEXT_ALL_PREFIX, ret);
				}
				else {
					ret = (List<SeriesLinkMaster>)obj;
				}
			// 表示のみ
			} else if (active) {
				Object obj = req.getServletContext().getAttribute(SeriesLinkMaster.APPLICATION_CONTEXT_PREFIX);
				if (obj == null) {
					ret = temp.listAll(true);
					req.getServletContext().setAttribute(SeriesLinkMaster.APPLICATION_CONTEXT_PREFIX, ret);
				}
				else {
					ret = (List<SeriesLinkMaster>)obj;
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

	@Override
	public List<SeriesLinkMaster> findByLangAll(String lang, Boolean active, ErrorObject err) {
		List<SeriesLinkMaster> ret = null;
		try {
			ret = temp.findAllByLang(lang, active);
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
	public SeriesLinkMaster get(String id, ErrorObject err) {
		SeriesLinkMaster ret = null;
		try {
			ret = repo.findById(id).orElseThrow(() -> new ModelNotFoundException("SeriesLinkMaster.id=" + id));
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

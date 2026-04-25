package com.smc.discontinued.service;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mongodb.MongoException;
import com.smc.discontinued.dao.DiscontinuedTemplateRepository;
import com.smc.discontinued.model.DiscontinuedTemplate;
import com.smc.exception.ModelExistsException;
import com.smc.exception.ModelNotFoundException;
import com.smc.webcatalog.config.ErrorCode;
import com.smc.webcatalog.dao.CategoryRepository;
import com.smc.webcatalog.model.ErrorObject;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DiscontinuedTemplateServiceImpl implements DiscontinuedTemplateService {

	@Autowired
	DiscontinuedTemplateRepository repo;

	@Autowired
	CategoryRepository categoryRepo;

	@Autowired
    HttpServletRequest req;

	@Override
	public ErrorObject save(DiscontinuedTemplate temp) {
		ErrorObject ret = new ErrorObject();
		try {

			temp = repo.save(temp);

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
	public DiscontinuedTemplate get(String id, ErrorObject err) {
		DiscontinuedTemplate ret = null;
		try {
			ret = repo.findById(id).orElseThrow(() -> new ModelNotFoundException("Template.id=" + id));
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
	public DiscontinuedTemplate getLang(String lang, ErrorObject err) {
		DiscontinuedTemplate ret = null;
		try {
			ret = repo.findByLang(lang).orElseThrow(() -> new ModelNotFoundException("Template. lang=" + lang));
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
	// 複数あったら１つにする
	public DiscontinuedTemplate getHeartCoreID(String id, ErrorObject err) {
		DiscontinuedTemplate ret = null;
		try {
			List<DiscontinuedTemplate> list = repo.findAllByHeartCoreID(id);
			if (list == null || list.size() == 0) {
				throw new ModelNotFoundException("Not found. heartCoreId = "+id);
			} else if (list.size() > 1) {
				ret = list.get(0);
				int cnt = 0;
				List<DiscontinuedTemplate> temp = new LinkedList<DiscontinuedTemplate>();
				for(DiscontinuedTemplate t : list)
				{
					if (cnt > 0) {
						temp.add(t);
					}
					cnt++;
				}
				repo.deleteAll(temp);
			} else {
				ret = list.get(0);
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

}

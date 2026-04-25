package com.smc.webcatalog.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mongodb.MongoException;
import com.smc.exception.ModelExistsException;
import com.smc.webcatalog.config.ErrorCode;
import com.smc.webcatalog.dao.CategoryRepository;
import com.smc.webcatalog.dao.SeriesFaqRepository;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.SeriesFaq;
import com.smc.webcatalog.model.User;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FaqCategoryServiceImpl implements FaqCategoryService {

	@Autowired
	SeriesFaqRepository repo;

	@Autowired
	CategoryRepository categoryRepo;

	@Override
	public ErrorObject saveAll(List<SeriesFaq> list, User u) {
		ErrorObject ret = new ErrorObject();
		try {
			Date date = new Date();
			for(SeriesFaq f : list) {
				if (f.getId() != null && f.getId().isEmpty()) {
					f.setId(null);
					f.setCtime(date);
					f.setMtime(date);
					f.setUser(u);
					repo.save(f);
				} else {
					ErrorObject obj = new ErrorObject();
					SeriesFaq faq = get(f.getId(), obj);
					if (faq != null) {
						faq.setMtime(date);
						faq.setUser(u);
						faq.setFaq(f.getFaq());
						repo.save(faq);
					}
				}
			}

			ret.setCount(1);

		} catch (ModelExistsException e) {
			ret.setCode(ErrorCode.E10001);
			ret.setMessage(e.getMessage());
		} catch (MongoException e) {
			log.error("MongoException", e);
			ret.setCode(ErrorCode.E50001);
			ret.setMessage(e.getMessage());
		} catch (Exception e) {
			log.error("Exception", e);
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public ErrorObject delete(String seriesId) {
		ErrorObject ret = new ErrorObject();
		try {
			repo.deleteBySeriesId(seriesId);
		} catch (ModelExistsException e) {
			ret.setCode(ErrorCode.E10001);
			ret.setMessage(e.getMessage());
		} catch (MongoException e) {
			log.error("MongoException", e);
			ret.setCode(ErrorCode.E50001);
			ret.setMessage(e.getMessage());
		} catch (Exception e) {
			log.error("Exception", e);
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public SeriesFaq getModelNumber(String modelNumber, ErrorObject err) {
		SeriesFaq ret = null;
		try {
			Optional<SeriesFaq> op = repo.findByModelNumber(modelNumber);
			if (op.isPresent()) ret = op.get();
		} catch (ModelExistsException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			log.error("MongoException", e);
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			log.error("Exception", e);
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
			
		return ret;
	}


	@Override
	public SeriesFaq get(String id, ErrorObject err) {
		SeriesFaq ret = null;
		try {
			Optional<SeriesFaq> op = repo.findById(id);
			if (op.isPresent()) ret = op.get();
		} catch (ModelExistsException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			log.error("MongoException", e);
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			log.error("Exception", e);
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
			
		return ret;
	}


	@Override
	public SeriesFaq getSeriesId(String seriesId, ErrorObject err) {
		SeriesFaq ret = null;
		try {
			Optional<SeriesFaq> op = repo.findBySeriesId(seriesId);
			if (op.isPresent()) ret = op.get();
		} catch (ModelExistsException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			log.error("MongoException", e);
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			log.error("Exception", e);
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
			
		return ret;
	}


}

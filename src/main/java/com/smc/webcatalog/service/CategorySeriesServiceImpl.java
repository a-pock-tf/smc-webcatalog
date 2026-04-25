package com.smc.webcatalog.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mongodb.MongoException;
import com.smc.exception.ModelExistsException;
import com.smc.webcatalog.config.ErrorCode;
import com.smc.webcatalog.dao.CategorySeriesRepository;
import com.smc.webcatalog.dao.CategorySeriesTemplateImpl;
import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.CategorySeries;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.Series;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CategorySeriesServiceImpl implements CategorySeriesService {

	@Autowired
	CategorySeriesRepository repo;

	@Autowired
	CategorySeriesTemplateImpl temp;

	@Override
	public CategorySeries save(Category category, Series series, ErrorObject err) {
		CategorySeries ret = null;
		try {
			CategorySeries cs = null;
			Optional<CategorySeries> oCs = repo.findByCategoryId(category.getId());

			if (oCs.isPresent() == false) {
				// 新規の場合
				cs = new CategorySeries();
				cs.setCategoryId(category.getId());
				List<Series> sList = new ArrayList<Series>();
				sList.add(series);
				cs.setSeriesList(sList);
				repo.save(cs);
				err.setCount(1);
			} else {
				cs = oCs.get();
				List<Series> sList = cs.getSeriesList();
				if (sList != null)
				{
					boolean isFind = false;
					for(Series s : sList) {
						if (s.getId().equals(series.getId())) {
							isFind = true;
							break;
						}
					}
					if (isFind == false) {
						sList.add(series);
					}
				}
				else {
					sList = new ArrayList<Series>();
					sList.add(series);
				}
				repo.save(cs);
				err.setCount(1);
			}

		} catch (ModelExistsException e) {
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
	public ErrorObject delete(String categoryId, String seriesId) {
		ErrorObject ret = new ErrorObject();
		try {
			List<CategorySeries> list = temp.findAllByCategoryAndSeriesId(categoryId, seriesId);
			if (list != null && list.size() > 0) {
				for(CategorySeries cs : list) {
					List<Series> sList = cs.getSeriesList();
					for(Series s : sList) {
						if (seriesId.equals(s.getId())) {
							sList.remove(s);
							break;
						}
					}
					cs.setSeriesList(sList);
					repo.save(cs);
				}
			}
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

}

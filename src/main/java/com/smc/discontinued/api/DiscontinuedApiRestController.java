package com.smc.discontinued.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.smc.discontinued.model.DiscontinuedCategory;
import com.smc.discontinued.model.DiscontinuedModelState;
import com.smc.discontinued.model.DiscontinuedSeries;
import com.smc.discontinued.service.DiscontinuedCategoryServiceImpl;
import com.smc.discontinued.service.DiscontinuedSeriesServiceImpl;
import com.smc.webcatalog.model.ErrorObject;

import lombok.extern.slf4j.Slf4j;

@RestController
@ResponseBody
@RequestMapping("/discontinued/api/v1")
@Slf4j
public class DiscontinuedApiRestController {
	
	@Autowired
	DiscontinuedSeriesServiceImpl seriesService;

	@Autowired
	DiscontinuedCategoryServiceImpl categoryService;
	
	@GetMapping({"/category/{lang}"})
	// カテゴリの一覧
	public List<DiscontinuedCategory> getCategory(@PathVariable(name = "lang", required = true) String lang,
			HttpServletRequest request) {
	
		List<DiscontinuedCategory> ret = null;
		ErrorObject err = new ErrorObject();
		DiscontinuedModelState s = DiscontinuedModelState.PROD;
		String referer = request.getHeader("REFERER");
		if (referer != null && referer.isEmpty() == false && isTest(referer) ) {
			s = DiscontinuedModelState.TEST;
		}
	
		{
			ret = categoryService.listAllActive(lang, s, err);
			if (ret != null) {
				
			}
		}
		if (err.isError() || ret == null || ret.size() == 0) {
		    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return ret;
	}
	@GetMapping({"/categoryItem/{lang}/slug/", "/categoryItem/{lang}/slug/{slug}", "/categoryItem/{lang}/slug/{slug}/"})
	// Slugからカテゴリとカテゴリ内のManualの数量を取得
	// testModeは止めてRefererを見るようにした。6/27
	public List<DiscontinuedSeries> getCategoryItem(@PathVariable(name = "lang", required = true) String lang,
			@PathVariable(name = "slug", required = false) String slug,
			@PathVariable(name = "slug2", required = false) String slug2,
			HttpServletRequest request) {
	
		List<DiscontinuedSeries> ret = null;
		ErrorObject err = new ErrorObject();
		DiscontinuedModelState s = DiscontinuedModelState.PROD;
		String referer = request.getHeader("REFERER");
		if (referer != null && referer.isEmpty() == false && isTest(referer) ) {
			s = DiscontinuedModelState.TEST;
		}
	
		if (slug2 != null && slug2.isEmpty() == false) {
			DiscontinuedCategory c = categoryService.getSlug(slug2, lang, s, err);
			if (c != null) {
				ret = seriesService.listCategory(c.getId(), s, true, err);
			}
		} else if (slug != null && slug.isEmpty() == false) {
			DiscontinuedCategory c = categoryService.getSlug(slug, lang, s, err);
			if (c != null) {
				ret = seriesService.listCategory(c.getId(), s, true, err);
			}
		}
		if (err.isError() || ret == null || ret.size() == 0) {
		    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
	
		return ret;
	}

	@GetMapping({"/categoryItemAll/{lang}", "/categoryItemAll/{lang}/"})
	// Slugからカテゴリとカテゴリ内のManualの数量を取得
	// testModeは止めてRefererを見るようにした。6/27
	public List<DiscontinuedCategory> getAllCategoryItem(@PathVariable(name = "lang", required = true) String lang,
			HttpServletRequest request) {
	
		List<DiscontinuedCategory> ret = null;
		ErrorObject err = new ErrorObject();
		DiscontinuedModelState s = DiscontinuedModelState.PROD;
		String referer = request.getHeader("REFERER");
		if (referer != null && referer.isEmpty() == false && isTest(referer) ) {
			s = DiscontinuedModelState.TEST;
		}
	
		{
			ret = categoryService.listAllActive(lang, s, err);
			if (ret != null) {
				for(DiscontinuedCategory c : ret) {
					if (c != null) {
						List<DiscontinuedSeries> list = seriesService.listCategory(c.getId(), s, true, err);
						if (list != null) {
							c.setSeriesList(list);
						}
					}
				}
			}
		}
		if (err.isError() || ret == null || ret.size() == 0) {
		    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return ret;
	
	}

	
	// ===== private =====
	private boolean isTest(String referer) {
		boolean ret = false;
		if (referer.indexOf("https://test.smcworld.com") > -1 || referer.indexOf("http://ap1.smcworld.com") > -1
		|| referer.indexOf("http://ap2.smcworld.com") > -1 || referer.indexOf("http://dev1.smcworld.com") > -1
		|| referer.indexOf("http://localhost:8080") > -1 || referer.indexOf("http://localhost:8081") > -1)  {
			ret = true;
		}
		return ret;
	}
}

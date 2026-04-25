package com.smc.discontinued.api;

import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smc.discontinued.model.DiscontinuedCategory;
import com.smc.discontinued.model.DiscontinuedModelState;
import com.smc.discontinued.model.DiscontinuedSeries;
import com.smc.discontinued.service.DiscontinuedCategoryServiceImpl;
import com.smc.discontinued.service.DiscontinuedSeriesServiceImpl;
import com.smc.webcatalog.model.ErrorObject;

import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin
@RequestMapping("/login/admin/discontinued/api/series")
@Slf4j
public class DiscontinuedSeriesRestController {

	@Autowired
	DiscontinuedSeriesServiceImpl service;

	@Autowired
	DiscontinuedCategoryServiceImpl caService;

	@GetMapping("/{id}")
	public DiscontinuedSeries get(@PathVariable(name = "id") String id) {

		ErrorObject obj = new ErrorObject();
		DiscontinuedSeries c = service.get(id, obj);

		return c;
	}
	/**
	 * IDとlangが同じならそのまま。違えば、langRefIdのlangのものを返す。
	 * @param id
	 * @param lang
	 * @return
	 */
	@GetMapping("/{id}/{lang}")
	public DiscontinuedSeries getCompare(@PathVariable(name = "id") String id, @PathVariable(name = "lang") String lang) {

		ErrorObject obj = new ErrorObject();
		DiscontinuedSeries c = service.get(id, obj);
		if (c != null) {
			if (c.getLang().equals(lang) == false) {
				c = service.getLangRefId(c, lang, obj);
			}
		}

		return c;
	}
	/**
	 * IDとlangが同じならそのまま。
	 * 違えば、langRefIdのlangのSeriesを元にCategorySeriesからCategoryのリストを返す。
	 * ARCHIVEならTESTのCategorySeriesを返す。
	 * @param id
	 * @param lang
	 * @return
	 */
	@GetMapping("/category/{id}/{lang}")
	public List<DiscontinuedCategory> getCompareCategory(@PathVariable(name = "id") String id, @PathVariable(name = "lang") String lang) {

		List<DiscontinuedCategory> ret = null;

		ErrorObject obj = new ErrorObject();
		DiscontinuedSeries s = service.get(id, obj);
		if (s != null) {
			if (s.getLang().equals(lang) == false) {
				s = service.getLangRefId(s, lang, obj);
			}
		}
		if (s.getState().equals(DiscontinuedModelState.ARCHIVE) && StringUtils.isEmpty(s.getStateRefId()) == false) {
			// TESTのSeriesを取得
			s = service.get(s.getStateRefId(), obj);
		}
		if (s != null) {
			DiscontinuedCategory c =caService.get(s.getCategoryId(), obj);
			ret = new LinkedList<DiscontinuedCategory>();
			ret.add(c);
		}

		return ret;
	}

}

package com.smc.discontinued.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smc.discontinued.model.DiscontinuedCategory;
import com.smc.discontinued.model.DiscontinuedModelState;
import com.smc.discontinued.service.DiscontinuedCategoryServiceImpl;
import com.smc.webcatalog.model.ErrorObject;

import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin
@RequestMapping("/login/admin/discontinued/api/category")
@Slf4j
public class DiscontinuedCategoryRestController {

	@Autowired
	DiscontinuedCategoryServiceImpl service;

	@GetMapping("/{id}")
	public DiscontinuedCategory get(@PathVariable(name = "id") String id) {

		ErrorObject obj = new ErrorObject();
		DiscontinuedCategory c = service.get(id, obj);

		return c;
	}

	/**
	 * IDとlangが同じならそのまま。違えば、langRefIdのものを返す。
	 * @param id
	 * @param lang
	 * @return
	 */
	@GetMapping("/{id}/{lang}")
	public DiscontinuedCategory getCompare(@PathVariable(name = "id") String id, @PathVariable(name = "lang") String lang) {

		ErrorObject obj = new ErrorObject();
		DiscontinuedCategory c = service.get(id, obj);
		if (c != null) {
			if (c.getLang().equals(lang) == false) {
				c = service.getLangRefId(c, lang, obj);
			}
		}

		return c;
	}

	@GetMapping("/lang/{lang}/{state}")
	public List<DiscontinuedCategory> getLang(@PathVariable(name = "lang") String lang, @PathVariable(name = "state") String state) {

		ErrorObject obj = new ErrorObject();
		DiscontinuedModelState st = DiscontinuedModelState.PROD;
		if (state.equals("TEST")) st = DiscontinuedModelState.TEST;
		else if (state.equals("ARCHIVE")) st = DiscontinuedModelState.ARCHIVE;

		List<DiscontinuedCategory> c = service.listAll(lang, st, obj);

		return c;
	}

}

package com.smc.webcatalog.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.service.CategoryService;

import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin
@RequestMapping("/login/admin/api/category")
@Slf4j
public class CategoryRestController {

	@Autowired
	CategoryService service;

	@GetMapping("/{id}")
	public Category get(@PathVariable(name = "id") String id) {

		ErrorObject obj = new ErrorObject();
		Category c = service.get(id, obj);

		return c;
	}

	/**
	 * IDとlangが同じならそのまま。違えば、langRefIdのものを返す。
	 * @param id
	 * @param lang
	 * @return
	 */
	@GetMapping("/{id}/{lang}")
	public Category getCompare(@PathVariable(name = "id") String id, @PathVariable(name = "lang") String lang) {

		ErrorObject obj = new ErrorObject();
		Category c = service.get(id, obj);
		if (c != null) {
			if (c.getLang().equals(lang) == false) {
				c = service.getLangRefId(c, lang, obj);
			}
		}

		return c;
	}

}

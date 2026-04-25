package com.smc.webcatalog.api;

import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Series;
import com.smc.webcatalog.model.SeriesLink;
import com.smc.webcatalog.model.SeriesLinkMaster;
import com.smc.webcatalog.service.CategoryService;
import com.smc.webcatalog.service.SeriesLinkMasterService;
import com.smc.webcatalog.service.SeriesService;

import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin
@RequestMapping("/login/admin/api/series")
@Slf4j
public class SeriesRestController {

	@Autowired
	SeriesService service;

	@Autowired
	SeriesLinkMasterService linkMasterService;

	@Autowired
	CategoryService caService;

	@GetMapping("/{id}")
	public Series get(@PathVariable(name = "id") String id) {

		ErrorObject obj = new ErrorObject();
		Series c = service.get(id, obj);

		return c;
	}
	/**
	 * IDとlangが同じならそのまま。違えば、langRefIdのlangのものを返す。
	 * @param id
	 * @param lang
	 * @return
	 */
	@GetMapping("/{id}/{lang}")
	public Series getCompare(@PathVariable(name = "id") String id, @PathVariable(name = "lang") String lang) {

		ErrorObject obj = new ErrorObject();
		Series c = service.get(id, obj);
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
	public List<Category> getCompareCategory(@PathVariable(name = "id") String id, @PathVariable(name = "lang") String lang) {

		List<Category> ret = null;

		ErrorObject obj = new ErrorObject();
		Series s = service.get(id, obj);
		if (s != null) {
			if (s.getLang().equals(lang) == false) {
				s = service.getLangRefId(s, lang, obj);
			}
		}
		if (s.getState().equals(ModelState.ARCHIVE) && StringUtils.isEmpty(s.getStateRefId()) == false) {
			// TESTのSeriesを取得
			s = service.get(s.getStateRefId(), obj);
		}
		if (s != null) {
			 ret = caService.listCategoryFromSeries(s.getId(), null, obj);
		}

		return ret;
	}

	/**
	 * IDとlangが同じならそのまま。
	 * 違えば、langRefIdのlangのSeriesを元にCategorySeriesからCategoryのリストを返す。
	 * ARCHIVEならTESTのCategorySeriesを返す。
	 * @note 登録されているSeriesLinkMasterはすべて設定した状態で戻す。表示で使う場合はactiveを見ること。
	 * @param id
	 * @param lang
	 * @return
	 */
	@GetMapping("/link/{id}/{lang}")
	public List<SeriesLink> getCompareSeriesLink(@PathVariable(name = "id") String id, @PathVariable(name = "lang") String lang) {

		List<SeriesLink> ret = null;
		List<SeriesLinkMaster> master = null;

		ErrorObject obj = new ErrorObject();
		Series s = service.get(id, obj);
		if (s != null) {
			if (s.getLang().equals(lang) == false) {
				s = service.getLangRefId(s, lang, obj);
			}
			if (lang != null) {
				master = linkMasterService.findByLangAll(lang, null, obj);
			}
		}
		if (s.getState().equals(ModelState.ARCHIVE) && StringUtils.isEmpty(s.getStateRefId()) == false) {
			// TESTのSeriesを取得
			// ARCHIVEのSeriesLinkは保持していないため。
			s = service.get(s.getStateRefId(),  obj);
		}
		if (s != null) {
			s = service.getWithLink(s.getId(), null, obj);

			// ret = s.getLink();
			// リンクマスター全部をセットする。javascriptで入れるのは大変。
			if (master != null) {
				List<SeriesLink> list = s.getLink();
				ret = new LinkedList<SeriesLink>();
				for(SeriesLinkMaster m  : master) {
					SeriesLink setSL = null;
					for(SeriesLink sl : list) {
						if (sl.getLinkMaster().getName().equals(m.getName())) {
							setSL = sl;
							break;
						}
					}
					if (setSL == null) {
						SeriesLink seLink = new SeriesLink();
						seLink.setLinkMaster(m);
						seLink.setUrl("");
						ret.add(seLink);
					} else {
						ret.add(setSL);
					}
				}
			}

		}

		return ret;
	}
}

package com.smc.webcatalog.web;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.FaqCategoryForm;
import com.smc.webcatalog.model.Series;
import com.smc.webcatalog.model.SeriesFaq;
import com.smc.webcatalog.model.SeriesFaqForm;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.model.ViewState;
import com.smc.webcatalog.service.CategoryService;
import com.smc.webcatalog.service.FaqCategoryService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/login/admin/category/faq")
@SessionAttributes(value= {"SessionScreenState", "SessionUser"})
public class FaqCategoryController extends BaseController {

	@Autowired
	CategoryService categoryService;
	
	@Autowired
	FaqCategoryService service;

	@Autowired
    HttpServletRequest req;

	/**
	 * 管理系 > カテゴリ > テンプレート
	 * @param myform
	 * @param mav
	 * @return
	 */
	@GetMapping({ "/{categoryId}"})
	public ModelAndView upsert(
			ModelAndView mav,
			@ModelAttribute("faqCategoryForm") FaqCategoryForm myform,
			@ModelAttribute("SessionUser") User s_user,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state,
			@PathVariable(name = "categoryId", required = true) String  categoryId) {

		// Set view
		mav.setViewName("/login/admin/category/faq");
		s_state.setView(ViewState.CATEGORY.toString());

		//リスト取得
		ErrorObject err = new ErrorObject();
		Category c = categoryService.getWithSeries(categoryId, false, err);
		if (c != null) {
			myform = getForm(categoryId);
		}

		//Add Form to View
		mav.addObject(myform);
		
		mav.addObject("categoryId", categoryId);
		setBreadcrumb(mav, categoryId);
		return mav;
	}
	

	/**
	 * POSTされたデータからDB更新
	 * @param mav
	 * @param form
	 * @param result
	 * @return
	 */
	@RequestMapping(value = "/post", method = RequestMethod.POST)
	public ModelAndView post(
			ModelAndView mav,
			@Validated @ModelAttribute("faqCategoryForm") FaqCategoryForm form,
			@ModelAttribute("SessionUser") User s_user,
			@RequestParam(name = "categoryId", required = false) String categoryId,
			BindingResult result) {
		// XXX BindingResultは@Validatedの直後の引数にする

		// Set view
		mav.setViewName("/login/admin/category/faq");

		log.debug(form.toString());

		// エラー判定
		if (!result.hasErrors()) {

			List<SeriesFaq> list = new ArrayList<>();
			for(SeriesFaqForm f : form.getSeriesFaqFormList()) {
				SeriesFaq faq = new SeriesFaq();
				modelMapper.map(f, faq);
				list.add(faq);
			}
			// 保存
			ErrorObject obj = service.saveAll(list, s_user);

			mav.addObject("is_success", !obj.isError());

		} else {

			// 戻りのページ この場合はedit.htmlなので何もしない
		}
		ErrorObject err = new ErrorObject();
		Category c = categoryService.getWithSeries(categoryId, false, err);
		if (c != null) {
			form = getForm(categoryId);
		}

		// フォームを更新(再編集用)
		mav.addObject(form);
		mav.addObject("categoryId", categoryId);
		setBreadcrumb(mav, categoryId);
		return mav;
	}

	// ===== private =====
	private FaqCategoryForm getForm(String  categoryId) {
		FaqCategoryForm ret = new FaqCategoryForm();
		ErrorObject err = new ErrorObject();
		Category c = categoryService.getWithSeries(categoryId, null, err);
		if (c != null) {
			List<Series> list = c.getSeriesList();
			for(Series s : list) {
				SeriesFaqForm sForm = new SeriesFaqForm();
				SeriesFaq faq = service.getSeriesId(s.getId(), err);
				if (faq != null) {
					modelMapper.map(faq, sForm);
				} else {
					sForm.setFaq("");
					sForm.setLang(s.getLang());
				}
				sForm.setSeriesId(s.getId());
				sForm.setName(s.getName()+" "+s.getNumber());
				sForm.setModelNumber(s.getModelNumber());
				ret.getSeriesFaqFormList().add(sForm);
			}
		}
		
		return ret;
	}
	private void setBreadcrumb(ModelAndView mav, String categoryId) {
		List<Category> breadcrumb = null;
		ErrorObject err = new ErrorObject();
		Category c = categoryService.get(categoryId, err);

		if (c != null && !StringUtils.isEmpty(c.getParentId())) {
			breadcrumb = categoryService.getParents(c.getId(), null, err);
		}
		//Add to View
		if (breadcrumb != null) {
			mav.addObject("breadcrumb", breadcrumb);
		}

		//debug
		if (breadcrumb == null) {
			log.debug(err.getMessage());
		} else {
			for (Category _c : breadcrumb) {
				if (_c != null) {
					log.debug(_c.getName());
				}
			}
		}
	}
}

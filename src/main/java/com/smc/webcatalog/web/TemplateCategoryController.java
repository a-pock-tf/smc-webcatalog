package com.smc.webcatalog.web;

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
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.TemplateCategory;
import com.smc.webcatalog.model.TemplateCategoryForm;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.model.ViewState;
import com.smc.webcatalog.service.CategoryService;
import com.smc.webcatalog.service.TemplateCategoryFormValidator;
import com.smc.webcatalog.service.TemplateCategoryService;

import lombok.extern.slf4j.Slf4j;

/**
 * カテゴリごとの大カテゴリ
 * @author tfujishima
 *
 * @note lang はja-jp固定。使っていない。
 */

@Controller
@Slf4j
@RequestMapping("/login/admin/category/template")
@SessionAttributes(value= {"SessionScreenState", "SessionUser"})
public class TemplateCategoryController extends BaseController {

	@Autowired
	CategoryService categoryService;

	@Autowired
	TemplateCategoryService service;

	@Autowired
	TemplateCategoryFormValidator validator;

	@Autowired
    HttpServletRequest req;

	/**
	 * 管理系 > カテゴリ > テンプレート
	 * @param myform
	 * @param mav
	 * @return
	 */
	@GetMapping({ "/{id}"})
	public ModelAndView get(
			ModelAndView mav,
			@ModelAttribute("templateCategoryForm") TemplateCategoryForm myform,
			@ModelAttribute("SessionUser") User s_user,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state,
			@PathVariable(name = "id", required = false) String id) {

		// Set view
		mav.setViewName("/login/admin/category/template");
		s_state.setView(ViewState.CATEGORY.toString());

		//リスト取得
		ErrorObject err = new ErrorObject();
		Category c = categoryService.get(id, err);
		TemplateCategory temp = service.getCategory(c, err);
		if (temp == null) {
			temp = new TemplateCategory();
			temp.setActive(true);
			temp.setLang(c.getLang());
			temp.setCategoryId(id);
		}
		// Map(Copy) Template -> Form
		modelMapper.map(temp, myform);

		//Add Form to View
		mav.addObject(myform);

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
			@Validated @ModelAttribute("templateCategoryForm") TemplateCategoryForm form,
			@RequestParam(name = "replace", required = false) String replace,
			@RequestParam(name = "toProd", required = false, defaultValue = "") String toProd,
			@ModelAttribute("SessionUser") User s_user,
			BindingResult result) {
		// XXX BindingResultは@Validatedの直後の引数にする

		// Set view
		mav.setViewName("/login/admin/category/template");

		log.debug(form.toString());

		// 1) - 7)までが基本的な更新処理の流れ

		TemplateCategory temp = null;

		// 1) idがあれば(=編集) dbから取得
		if (!StringUtils.isEmpty(form.getId())) {
			ErrorObject obj = new ErrorObject();
			temp = service.get(form.getId(), obj);
			// 3) フォームvalidate
			validator.validateUpate(result, form);
		} else {
			temp = new TemplateCategory();
			validator.validateNew(result, form);
		}

		log.debug(result.toString());

		// 4) エラー判定
		if (!result.hasErrors()) {

			ErrorObject obj = new ErrorObject();
			String errorMessage = "";
			// 5) フォームからTemplateにコピー
			modelMapper.map(form, temp);
			log.debug("Lang(FORM)=" + temp.toString());

			// 新規、またはReplaceのチェックがあれば取得
			if ( (StringUtils.isEmpty(form.getId()) && StringUtils.isEmpty(form.getHeartCoreId())) || (replace != null && replace.equals("1") )) {
				service.setHeartCore(temp);
				if (temp.getContent() == null || temp.getContent().equals("") || temp.getTemplate() == null || temp.getTemplate().indexOf("$$$content$$$") == -1) {
					//result.rejectValue("content", "my.validation.empty", new String[] { "" }, "contentの入力形式が不正です。");
					errorMessage = "contentの入力形式が不正です。";
				}
			}
			
			// TODO しばらくしたら削除OK
			{
				Category c = categoryService.get(form.getCategoryId(), obj);
				if (temp.getLang() != null && c.getLang() != null && temp.getLang().equals(c.getLang()) == false) {
					temp.setLang(c.getLang()); // TemplateCategoryのLangがja-jpのみだった。
				}
			}


			// 6) 保存
			if (!result.hasErrors() && errorMessage.isEmpty()) {
				obj = service.save(temp);
				mav.addObject("is_success", !obj.isError());
				if (temp.isActive()) {
					if ( temp.getState().equals(ModelState.TEST) && toProd !=  null && toProd.equals("1") ) { // TESTで Save&Prod
						Category c = categoryService.get(form.getCategoryId(), obj);
						Category pC = categoryService.getStateRefId(c, ModelState.PROD, obj);
						service.changeStateToProd(temp.getId(), pC.getId(), s_user);
					}
				}
				service.refreshTemplateCategories(); // メモリ上のtemplate更新
			} else {
				mav.addObject("is_error", errorMessage);
			}
			// TEST用。大カテゴリにすべて登録
/*			if (temp.getMemo().equals("AllCategory")) {
				temp.setMemo("");
				service.save(temp);
				ErrorObject err = new ErrorObject();
				List<Category> list = categoryService.listAll(temp.getLang(), temp.getState(), CategoryType.CATALOG, err);
				Category rootC = list.get(0);
				String nowC = temp.getCategoryId();
				for(Category c : list) {
					// 大カテゴリのみ
					if (c.getId().equals(rootC.getId()) == false && c.getId().equals(nowC) == false && c.getParentId().equals(rootC.getId())) {
						TemplateCategory t = service.getCategory(c.getId(), err);
						if (t != null) {
							t.setCatpan(temp.getCatpan());
							t.setContent(temp.getContent());
							t.setFormbox(temp.getFormbox());
							t.setH1box(temp.getH1box());
							t.setHeartCoreID(temp.getHeartCoreID());
							t.setSidebar(temp.getSidebar());
							t.setTemplate(temp.getTemplate());
							obj = service.save(t);
						} else {
							temp.setId(null);
							temp.setCategoryId(c.getId());
							obj = service.save(temp);
						}
					}
				}
			}
*/
			// 7) フォームを更新(再編集用)
			modelMapper.map(temp, form);


		} else {

			// 戻りのページ この場合はedit.htmlなので何もしない
		}

		mav.addObject(form);
		mav.addObject("form", temp);

		return mav;
	}


}

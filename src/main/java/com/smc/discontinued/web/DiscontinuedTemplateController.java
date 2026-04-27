package com.smc.discontinued.web;

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

import com.smc.discontinued.config.DiscontinuedConfig;
import com.smc.discontinued.model.DiscontinuedTemplate;
import com.smc.discontinued.model.DiscontinuedTemplateForm;
import com.smc.discontinued.service.DiscontinuedTemplateFormValidator;
import com.smc.discontinued.service.DiscontinuedTemplateService;
import com.smc.webcatalog.config.AppConfig;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.Lang;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.model.ViewState;
import com.smc.webcatalog.service.LangService;
import com.smc.webcatalog.util.LibOkHttpClient;
import com.smc.webcatalog.web.ScreenStatusHolder;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/login/admin/discontinued/template")
@SessionAttributes(value= {"SessionScreenState", "SessionUser"})
public class DiscontinuedTemplateController extends DiscontinuedBaseController {

	@Autowired
	DiscontinuedTemplateService service;

	@Autowired
	DiscontinuedTemplateFormValidator validator;

	@Autowired
	LangService langService;

	@Autowired
    HttpServletRequest req;

	@Autowired
	com.smc.webcatalog.util.LibHtml html;

	/**
	 * 管理系 > 言語 > 一覧
	 * @param myform
	 * @param mav
	 * @return
	 */
	@GetMapping({ "", "/"})
	public ModelAndView edit(
			ModelAndView mav,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		// Set view
		mav.setViewName("/login/admin/discontinued/template/list");
		s_state.setView(ViewState.DISCON_TEMPLATE.toString());
		s_state.setProd(false);

		//リスト取得
		ErrorObject err = new ErrorObject();
		List<Lang> list = langService.listAll(null, err);

		log.debug("list=" + list.toString());

		//Add Form to View
		mav.addObject("list", list);

		return mav;
	}

	/**
	 * 管理系 > カテゴリ > テンプレート
	 * @param myform
	 * @param mav
	 * @return
	 */
	@GetMapping({ "/{lang}"})
	public ModelAndView upsert(
			ModelAndView mav,
			@ModelAttribute("discontinuedTemplateForm") DiscontinuedTemplateForm myform,
			@ModelAttribute("SessionUser") User s_user,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state,
			@PathVariable(name = "lang", required = false) String lang) {

		// Set view
		mav.setViewName("/login/admin/discontinued/template/edit");
		s_state.setView(ViewState.DISCON_TEMPLATE.toString());
		s_state.setProd(false);

		//リスト取得
		ErrorObject err = new ErrorObject();
		DiscontinuedTemplate temp = service.getLang(lang, err);
		if (temp == null) {
			temp = new DiscontinuedTemplate();
			temp.setActive(true);
			temp.setLang(lang);
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
			@Validated @ModelAttribute("discontinuedTemplateForm") DiscontinuedTemplateForm form,
			@RequestParam(name = "replace", required = false) String replace,
			BindingResult result) {
		// XXX BindingResultは@Validatedの直後の引数にする

		// Set view
		mav.setViewName("/login/admin/discontinued/template/edit");

		log.debug(form.toString());

		// 1) - 7)までが基本的な更新処理の流れ

		DiscontinuedTemplate temp = null;

		// 1) idがあれば(=編集) dbから取得
		if (!StringUtils.isEmpty(form.getId())) {
			ErrorObject obj = new ErrorObject();
			temp = service.get(form.getId(), obj);
			// 3) フォームvalidate
			validator.validateUpate(result, form);

		}else {
			temp = new DiscontinuedTemplate();
			validator.validateNew(result, form);
		}

		log.debug(result.toString());

		// 4) エラー判定
		if (!result.hasErrors()) {

			// 5) フォームからTemplateにコピー
			modelMapper.map(form, temp);
			log.debug("Lang(FORM)=" + temp.toString());

			// 新規、またはReplaceのチェックがあれば取得
			if ( (StringUtils.isEmpty(form.getId()) && StringUtils.isEmpty(form.getHeartCoreId())) || (replace != null && replace.equals("1") )) {
				String src = LibOkHttpClient.getHttpsHtml(AppConfig.PageCDNIdUrl + form.getHeartCoreId());
				if (src != null) {
					List<String> list = html.divHtml(src, AppConfig.TemplateDiv);
					String templateHead = list.get(0);
					String templateFoot = list.get(2);
					String template = null;
					if (list.size() == AppConfig.TemplateDiv.length+1) {
						template = AppConfig.TemplateDiv[0] + list.get(1);
					}
					list = html.divHtmlLimit(src, AppConfig.CatpanArea, 2); // </div>が複数あるのでLimit付き
					if (list.size() == AppConfig.CatpanArea.length+1) {
						String work = list.get(1);
						int end = work.lastIndexOf(";");
						work = work.substring(0, end+1);
						String t = AppConfig.CatpanArea[0]+ list.get(1) + AppConfig.CatpanArea[1];
						template = template.replace(t, "$$$catpan$$$");
						t = AppConfig.CatpanArea[0] + work + "$$$title$$$" + AppConfig.CatpanArea[1];
						temp.setCatpan(t);
					}
					list = html.divHtml(template, DiscontinuedConfig.SidebarArea);
					if (list.size() == DiscontinuedConfig.SidebarArea.length+1) {
						String sidebar = DiscontinuedConfig.SidebarArea[0]+ "$$$category$$$" + DiscontinuedConfig.SidebarArea[1];
						// sidebarはCategoryをTemplate化
						{
							temp.setSidebar(sidebar);
							template = list.get(0) + "$$$sidebar$$$" + list.get(2);
						}
					}
					list = html.divHtml(template, AppConfig.FormboxArea);
					if (list.size() == AppConfig.FormboxArea.length+1) {
						String t = AppConfig.FormboxArea[0]+list.get(1) + AppConfig.FormboxArea[1];
						temp.setFormbox(t);
						template = template.replace(t, "$$$formbox$$$");
					}
					list = html.divHtml(template, AppConfig.H1boxArea);
					if (list.size() == AppConfig.H1boxArea.length+1) {
						String h1 = list.get(1);
						int s = h1.indexOf(">");
						int e = h1.indexOf("</");
						if (s > 0 && e > 0 && s < e) {
							String tmp = h1.substring(s+1, e);
							h1 = h1.replace(tmp, "$$$title$$$");
						}
						String t = AppConfig.H1boxArea[0] + h1 + AppConfig.H1boxArea[1];
						temp.setH1box(t);
						template = template.replace(AppConfig.H1boxArea[0] + list.get(1) + AppConfig.H1boxArea[1], "$$$h1box$$$");
					}
					list = html.divHtml(template, AppConfig.ContentArea);
					if (list.size() == AppConfig.ContentArea.length+1) {
						String t = AppConfig.ContentArea[0]+list.get(1) + AppConfig.ContentArea[1];
						temp.setContent(t);
						template = template.replace(t, "$$$content$$$");
					}
					temp.setTemplate(templateHead + template + AppConfig.TemplateDiv[1] + templateFoot);
				}
			}

			// 6) 保存
			ErrorObject obj = service.save(temp);

			// 7) フォームを更新(再編集用)
			modelMapper.map(temp, form);

			mav.addObject("is_success", !obj.isError());

		} else {

			// 戻りのページ この場合はedit.htmlなので何もしない
		}

		mav.addObject(form);
		mav.addObject("form", temp);

		return mav;
	}
}

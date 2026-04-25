package com.smc.webcatalog.service;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.mongodb.MongoException;
import com.smc.exception.ModelExistsException;
import com.smc.exception.ModelNotFoundException;
import com.smc.webcatalog.config.AppConfig;
import com.smc.webcatalog.config.ErrorCode;
import com.smc.webcatalog.dao.TemplateRepository;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Template;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.util.LibHtml;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TemplateServiceImpl implements TemplateService {

	@Autowired
	TemplateRepository repo;
	
	@Autowired
	@Qualifier("templates")
	List<Template> templates;

	@Autowired
	LibHtml html;

	@Autowired
    HttpServletRequest req;

	@Override
	public ErrorObject save(Template temp) {
		ErrorObject ret = new ErrorObject();
		try {

			temp = repo.save(temp);

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
	public ErrorObject changeStateToProd(String id, User u) {
		ErrorObject ret = new ErrorObject();
		try {
			// TEST以外ならエラー
			Template t = repo.findById(id).orElseThrow(() -> new ModelNotFoundException("Template.id=" + id));;
			if (t != null) {
				if (t.getState() != null && t.getState().equals(ModelState.TEST) == false) {
					throw new ModelExistsException("Template is not TEST. state="+t.getState());
				}
				Optional<Template> oT = repo.findByStateRefId(id);
				if (oT.isPresent()) {
					// update
					Template prodT = oT.get();
					prodT.setUpdateParam(t);
					prodT = repo.save(prodT);
				} else {
					// new 
					Template prodT = new Template(t.getLang());
					prodT.setUpdateParam(t);
					prodT.setId(null);
					prodT.setState(ModelState.PROD);
					prodT.setStateRefId(t.getId());
					// templateはlangRefIdの設定無し。
					prodT = repo.save(prodT);
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
	public Template get(String id, ErrorObject err) {
		Template ret = null;
		try {
			ret = repo.findById(id).orElseThrow(() -> new ModelNotFoundException("Template.id=" + id));
		} catch (ModelNotFoundException e) {
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

/*	@Override
	public Template getLang(String lang, ErrorObject err) {
		Template ret = null;
		try {
			ret = repo.findByLang(lang).orElseThrow(() -> new ModelNotFoundException("Template.lang=" + lang));
		} catch (ModelNotFoundException e) {
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
	}*/

	@Override
	public Template getLangAndModelState(String lang, ModelState state, Boolean active, ErrorObject err) {
		Template ret = null;
		try {
			ret = repo.findByLangAndModelState(lang, state, active).orElseThrow(() -> new ModelNotFoundException("Template.lang=" + lang + " state=" + state));
		} catch (ModelNotFoundException e) {
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
	public void setHeartCore(Template temp) {
		String url = AppConfig.PageCDNIdUrl + temp.getHeartCoreId();
//		String url = AppConfig.PageProdUrl + "/page.jsp?id=" + temp.getHeartCoreId();
		
		List<String> list = html.getDivHtml(url, AppConfig.TemplateDiv);
		if (list.size() == 3 && list.get(1).indexOf("<main") > -1) { // 2026
			temp.setHeader(list.get(0));
			temp.setContents(list.get(1));
			temp.setFooter(list.get(2));
		} else if (list.size() == AppConfig.TemplateDiv.length+1) {
			temp.setHeader(list.get(0));
			temp.setContents(AppConfig.TemplateDiv[0] + list.get(1));
			temp.setFooter(AppConfig.TemplateDiv[1] + list.get(2));
		}
	}
	
	// ===== 以下、List<Template>の処理 =====

	@Override
	public void refreshTemplates() {
		templates = repo.findAll();
	}

	@Override
	public void addTemplates(Template temp) {
		if (templates != null) {
			templates.add(temp);
		}
	}

	@Override
	public void removeTemplates(Template temp) {
		if (templates != null) {
			for (Template t : templates) {
				if (t.getId().equals(temp.getId()) ) {
					templates.remove(t);
					break;
				}
			}
		}
	}

	@Override
	public Template getTemplateByTemplates(String lang, ModelState s) {
		Template ret = null;
		if (templates != null) {
			for (Template t : templates) {
				if (t.getLang().equals(lang) && t.getState().equals(s)) {
					ret = t;
					break;
				}
			}
		}
		return ret;
	}

}

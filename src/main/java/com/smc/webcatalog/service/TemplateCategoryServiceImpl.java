package com.smc.webcatalog.service;

import java.util.LinkedList;
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
import com.smc.webcatalog.dao.CategoryRepository;
import com.smc.webcatalog.dao.TemplateCategoryRepository;
import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.TemplateCategory;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.util.LibHttpClient;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TemplateCategoryServiceImpl implements TemplateCategoryService {

	@Autowired
	TemplateCategoryRepository repo;

	@Autowired
	CategoryRepository categoryRepo;
	
	@Autowired
	@Qualifier("templateCategories")
	List<TemplateCategory> templateCategories;

	@Autowired
	com.smc.webcatalog.util.LibHtml html;

	@Autowired
    HttpServletRequest req;

	@Override
	public ErrorObject save(TemplateCategory temp) {
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
	public ErrorObject changeStateToProd(String id, String categoryId, User u) {
		ErrorObject ret = new ErrorObject();
		try {
			// TEST以外ならエラー
			TemplateCategory t = repo.findById(id).orElseThrow(() -> new ModelNotFoundException("Template.id=" + id));;
			if (t != null) {
				if (t.getState() != null && t.getState().equals(ModelState.TEST) == false) {
					throw new ModelExistsException("Template is not TEST. state="+t.getState());
				}
				Optional<TemplateCategory> oT = repo.findByStateRefId(id);
				if (oT.isPresent()) {
					// update
					TemplateCategory prodT = oT.get();
					prodT.SetUpdateParam(t, u);
					prodT.setCategoryId(categoryId);
					prodT = repo.save(prodT);
				} else {
					// new 
					TemplateCategory prodT = new TemplateCategory();
					prodT.SetUpdateParam(t, u);
					prodT.setId(null);
					prodT.setCategoryId(categoryId);
					prodT.setLang(t.getLang());
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
	public TemplateCategory get(String id, ErrorObject err) {
		TemplateCategory ret = null;
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
	@Override
	public TemplateCategory getCategory(String categoryId, ErrorObject err) {
		TemplateCategory ret = null;
		try {
/*			Category c = null;
			Optional<Category> oC = categoryRepo.findById(categoryId);
			if (oC.isPresent() && oC.get().getState().equals(ModelState.PROD)) {
				oC = categoryRepo.findById(oC.get().getStateRefId());
			}
			if (oC.isPresent()) c = oC.get();
			if (c != null) ret = repo.findByCategoryId(c.getId()).orElseThrow(() -> new ModelNotFoundException("Template.categoryId=" + categoryId));*/
			ret = repo.findByCategoryId(categoryId).orElseThrow(() -> new ModelNotFoundException("Template.categoryId=" + categoryId));
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
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
	public TemplateCategory getCategory(Category c, ErrorObject err) {
		TemplateCategory ret = null;
		try {
/*			Category c = null;
			Optional<Category> oC = categoryRepo.findById(categoryId);
			if (oC.isPresent() && oC.get().getState().equals(ModelState.PROD)) {
				oC = categoryRepo.findById(oC.get().getStateRefId());
			}
			if (oC.isPresent()) c = oC.get();
			if (c != null) ret = repo.findByCategoryId(c.getId()).orElseThrow(() -> new ModelNotFoundException("Template.categoryId=" + categoryId));*/
			// 2026/04/11 カテゴリが２つ出来てしまっている場合はcontentがカラを削除
			// ret = repo.findByCategoryId(categoryId).orElseThrow(() -> new ModelNotFoundException("Template.categoryId=" + categoryId));
			List<TemplateCategory> list = repo.findAllByCategoryId(c.getId());
			if (list.size() >= 2) {
				int max = list.size();
				int cnt = 0;
				for (TemplateCategory tc : list) {
					if (tc.getContent() == null || tc.getContent().isEmpty() || tc.getState().equals(c.getState()) == false) {
						repo.delete(tc);
						cnt++;
						if (max-1 <= cnt) break;
					}
				}
				list = repo.findAllByCategoryId(c.getId());
				if (list.size() == 1) {
					ret = list.get(0);
				}
			} else if (list.size() == 1) {
				ret = list.get(0);
			}
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
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
	// 複数あったら１つにする
	public TemplateCategory getHeartCoreID(String id, ErrorObject err) {
		TemplateCategory ret = null;
		try {
			List<TemplateCategory> list = repo.findAllByHeartCoreID(id);
			if (list == null || list.size() == 0) {
				throw new ModelNotFoundException("Not found. heartCoreId = "+id);
			} else if (list.size() > 1) {
				ret = list.get(0);
				int cnt = 0;
				List<TemplateCategory> temp = new LinkedList<TemplateCategory>();
				for(TemplateCategory t : list)
				{
					if (cnt > 0) {
						temp.add(t);
					}
					cnt++;
				}
				repo.deleteAll(temp);
			} else {
				ret = list.get(0);
			}
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

	public void setHeartCore(TemplateCategory temp) {

		String src = LibHttpClient.getHttpsHtml(AppConfig.PageCDNIdUrl + temp.getHeartCoreID());
		if (src != null) {
			if (src.indexOf("<main") > -1) {
				setHeartCore2026(src, temp); // 2026リニューアル
			} else {
				List<String> list = html.divHtml(src, AppConfig.TemplateDiv);
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
				list = html.divHtml(template, AppConfig.SidebarArea);
				if (list.size() == AppConfig.SidebarArea.length+1) {
					String sidebar = AppConfig.SidebarArea[0]+list.get(1) + AppConfig.SidebarArea[1];
					// sidebarはCategoryをTemplate化
					String[] tmp = sidebar.split(AppConfig.CategoryArea[0]);
					if (list.size() == 3) {
						String[] tmp2 = tmp[1].split(AppConfig.CategoryArea[1]);
						String[] tmp3 = tmp[2].split(AppConfig.CategoryArea[1]);
						template = template.replace(sidebar, "$$$sidebar$$$");
						sidebar = sidebar.replace(AppConfig.CategoryArea[1] + tmp2[1], "$$$category$$$");
						sidebar = sidebar.replace(AppConfig.CategoryArea[1] + tmp3[1], "$$$category2$$$");
						sidebar = sidebar.replace("</ul>\r\n<h4>" , "</ul>\r\n$$$narrowdown$$$\r\n<h4>"); // 2024/10/24 絞り込み検索
						sidebar += AppConfig.CategoryArea[2];
						temp.setSidebar(sidebar);
					} else if (list.size() == 2) {
						String[] tmp2 = tmp[1].split(AppConfig.CategoryArea[1]);
						template = template.replace(sidebar, "$$$sidebar$$$");
						sidebar = sidebar.replace(tmp2[1], "$$$category$$$");
						temp.setSidebar(sidebar);
					} else {
						temp.setSidebar(sidebar);
						template = template.replace(sidebar, "$$$sidebar$$$");
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
					int e = h1.indexOf("</h1");
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
				} else {
					temp.setContent("");
				}
				temp.setTemplate(template);
			}
		}
	}
	
	private void setHeartCore2026(String src, TemplateCategory temp) {
		List<String> list = html.divHtml2026(src);
		String template = null;
		if (list.size() == 3) {
			template = list.get(1); // <main>
			String catpan = html.extractHtml(src, AppConfig.CatpanArea2026);
			if (catpan != null && catpan.isEmpty() == false) {
				String t = AppConfig.CatpanArea2026 + catpan + AppConfig.CatpanArea2026;
				template = template.replace(t, "$$$catpan$$$");
				// <!-- catpan_title -->
				String c = html.extractHtml(t, "<!-- catpan_title -->");
				t = t.replace(c, "$$$catpan_title$$$").trim();
				temp.setCatpan(t);
			}
			String formbox = html.extractHtml(src, AppConfig.FormboxArea2026);
			if (formbox != null && formbox.isEmpty() == false) {
				String t = AppConfig.FormboxArea2026 + formbox + AppConfig.FormboxArea2026;
				template = template.replace(t, "$$$formbox$$$");
				temp.setFormbox(t.trim());
			}
			String sidebar = html.extractHtml(src, AppConfig.SidebarArea2026);
			if (sidebar != null && sidebar.isEmpty() == false) {
				String t = AppConfig.SidebarArea2026 + sidebar + AppConfig.SidebarArea2026;
				template = template.replace(t, "$$$sidebar$$$");
				// narrowdown
				String n = html.extractHtml(t, AppConfig.NarrowDownArea2026);
				if (n != null && n.isEmpty() == false) {
					String c = AppConfig.NarrowDownArea2026 + n + AppConfig.NarrowDownArea2026;
					t = t.replace(c, "$$$narrowdown$$$");
				}
				// category
				String c = html.extractHtml(t, AppConfig.CategoryArea2026);
				if (c != null && c.isEmpty() == false) {
					String ca = AppConfig.CategoryArea2026 + c + AppConfig.CategoryArea2026;
					t = t.replace(ca, "$$$category$$$");
				}
				String strCate2 = AppConfig.CategoryArea2026.replace("category", "category2");
				String c2 = html.extractHtml(t, strCate2);
				if (c2 != null && c2.isEmpty() == false) {
					String ca = strCate2 + c2 + strCate2;
					t = t.replace(ca, "$$$category2$$$");
				}
				t = t.trim();
				temp.setSidebar(t);
			}
			String h1box = html.extractHtml(src, AppConfig.H1boxArea2026);
			if (h1box != null && h1box.isEmpty() == false) {
				String replaceStr = AppConfig.H1boxArea2026 + h1box + AppConfig.H1boxArea2026;
				String t = replaceStr;
				if (t != null) {
					String[] arr = t.split("\n");
					for(String tmp : arr) {
						// <h3 class="f fm gap-8 mb8 m-mb12 s-mb12"><span class="s6 bg-primary circle"></span><span class="text-sm leading-tight fw5 text-primary">Directional Control Valves</span></h3>
						if (tmp.indexOf("<h3 ") > -1) {
							String reg = "text-primary\">(.*)</span>";
							String after = tmp.replaceAll(reg, "text-primary\">"+"\\$\\$\\$title31\\$\\$\\$"+"</span>"); 
							t = t.replace(tmp, after);
						}
						// <h2 class="text-6xl leading-tight fw5 s-fw6 s-text-3xl m-fw6 m-text-3xl"><span class="text-primary">方</span><span class="text-base-foreground-default">向制御機器</span></h2>
						if (tmp.indexOf("<h2 ") > -1) {
							String reg = "text-primary\">(.*)</span><span ";
							String after = tmp.replaceAll(reg, "text-primary\">"+"\\$\\$\\$title21\\$\\$\\$"+"</span><span "); 
							t = t.replace(tmp, after);
							tmp = after;
							String reg2 = "foreground-default\">(.*)</span>";
							String after2 = tmp.replaceAll(reg2, "foreground-default\">"+"\\$\\$\\$title22\\$\\$\\$"+"</span>"); 
							t = t.replace(tmp, after2);
						}
					}
				}
				template = template.replace(replaceStr, "$$$h1box$$$");
				temp.setH1box(t.trim());
			}
			String content = html.extractHtml(src, AppConfig.ContentArea2026);
			if (content != null && content.isEmpty() == false) {
				String t = AppConfig.ContentArea2026 + content + AppConfig.ContentArea2026;
				template = template.replace(t, "$$$content$$$");
				temp.setContent(t.trim());
			}
			temp.setTemplate(template);
		}
	}
	
	// ===== 以下、List<TemplateCategory>の処理 =====

	@Override
	public void refreshTemplateCategories() {
		templateCategories = repo.findAll();
		
	}

	@Override
	public void addTemplateCategory(TemplateCategory temp) {
		if (templateCategories != null) {
			templateCategories.add(temp);
		}
	}

	@Override
	public void removeTemplateCategory(TemplateCategory temp) {
		if (templateCategories != null) {
			for (TemplateCategory t : templateCategories) {
				if (t.getId().equals(temp.getId()) ) {
					templateCategories.remove(t);
					break;
				}
			}
		}
	}

	@Override
	public TemplateCategory findByCategoryIdFromTemplateCategories(String lang, ModelState s, String id) {
		TemplateCategory ret = null;
		if (templateCategories != null) {
			for (TemplateCategory t : templateCategories) {
				if (t.getLang().equals(lang) && t.getState().equals(s) && t.getCategoryId().equals(id)) {
					ret = t;
					break;
				}
			}
		}
		return ret;
	}

}

package com.smc.webcatalog.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.smc.webcatalog.config.ErrorCode;
import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.NarrowDownColumn;
import com.smc.webcatalog.model.NarrowDownColumnForm;
import com.smc.webcatalog.model.NarrowDownSeriesForm;
import com.smc.webcatalog.model.NarrowDownValue;
import com.smc.webcatalog.model.NarrowDownValueForm;
import com.smc.webcatalog.model.Series;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.model.ViewState;
import com.smc.webcatalog.service.CategoryService;
import com.smc.webcatalog.service.NarrowDownService;

import au.com.bytecode.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/login/admin/category/narrowdown")
@SessionAttributes(value= {"SessionScreenState", "SessionUser"})
public class NarrowDownController extends BaseController {

	@Autowired
	CategoryService categoryService;
	
	@Autowired
	NarrowDownService service;

	@Autowired
    HttpServletRequest req;

	/**
	 * 管理系 > カテゴリ > 項目
	 * @param myform
	 * @param mav
	 * @return
	 */
	@GetMapping({ "/column/{categoryId}"})
	public ModelAndView list(
			ModelAndView mav,
			@ModelAttribute("narrowDownColumnForm") NarrowDownColumnForm myform,
			@ModelAttribute("SessionUser") User s_user,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state,
			@PathVariable(name = "categoryId", required = true) String  categoryId) {

		// Set view
		mav.setViewName("/login/admin/category/narrowdown_column");
		s_state.setView(ViewState.CATEGORY.toString());

		//リスト取得
		ErrorObject err = new ErrorObject();
		Category c = categoryService.getWithSeries(categoryId, false, err);
		if (c != null) {
			myform = getColumnForm(categoryId);
		}

		//Add Form to View
		mav.addObject(myform);
		mav.addObject("category", c);
		mav.addObject("categoryId", categoryId);
		setBreadcrumb(mav, c);
		return mav;
	}
	
	/**
	 * 管理系 > カテゴリ > 値
	 * @param myform
	 * @param mav
	 * @return
	 */
	@GetMapping({ "/value/{categoryId}"})
	public ModelAndView listValue(
			ModelAndView mav,
			@ModelAttribute("narrowDownValueForm") NarrowDownValueForm vform,
			@ModelAttribute("SessionUser") User s_user,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state,
			@PathVariable(name = "categoryId", required = true) String  categoryId) {

		// Set view
		mav.setViewName("/login/admin/category/narrowdown_value");
		s_state.setView(ViewState.CATEGORY.toString());

		//リスト取得
		ErrorObject err = new ErrorObject();
		Category c = categoryService.getWithSeries(categoryId, null, err);
		if (c != null) {
			vform = getValueForm(c);
		}

		//Add Form to View
		mav.addObject(vform);
		mav.addObject("category", c);
		mav.addObject("categoryId", categoryId);
		setBreadcrumb(mav, c);
		return mav;
	}
	
	/**
	 * 管理系 > カテゴリ > 値 > CSVダウンロード
	 * @param myform
	 * @param mav
	 * @return
	 */
	@GetMapping({ "/valueCSV/{categoryId}"})
	public void download(
			@PathVariable(name = "categoryId", required = true) String  categoryId,
			HttpServletResponse response) throws IOException {

		String csvFileName = "narrow_down_value.csv";
		try{
			//リスト取得
	        ErrorObject err = new ErrorObject();
			Category c = categoryService.getWithSeries(categoryId, null, err);
			if (c != null) {
				response.setContentType("text/csv");
				String headerKey = "Content-Disposition";
		        String headerValue = String.format("attachment; filename=\"%s\"",
		                csvFileName);
		        response.setHeader(headerKey, headerValue);
		        if (c.getLang().equals("ja-jp") || c.getLang().indexOf("en-") > -1) {
		        	response.setHeader("Content-Type", "text/csv; charset=MS932");
		        } else {
		        	response.setHeader("Content-Type", "text/csv; charset=UTF-8");
		        }

		        NarrowDownValueForm vform = getValueForm(c);
				PrintWriter pw = response.getWriter();
				List<String> cols = vform.getColumns();
				List<NarrowDownSeriesForm> sList =vform.getSeriesList();
				String str = categoryId+","+c.getLang()+",";
				for(String col : cols) {
					str += "\"" +col+"\",";
				}
				str = str.substring(0, str.length()-1);
				pw.println(str);
				
				for (NarrowDownSeriesForm s : sList) {
					str = "";
					str += s.getSeriesId()+",\""+s.getModelNumber()+"\",";
					for(String v : s.getParams()) {
						str+="\""+v+"\",";
					}
					str = str.substring(0, str.length()-1);
					pw.println(str);
				}
				pw.close();
			}
		} catch (Exception e) {
			throw new IOException();
		}

		return ;
	}
	/**
	 * POSTされたデータからDB更新
	 * @param mav
	 * @param form
	 * @param result
	 * @return
	 */
	@RequestMapping(value = "/column/post", method = RequestMethod.POST)
	public ModelAndView postColumn(
			ModelAndView mav,
			@Validated @ModelAttribute("narrowDownColumnForm") NarrowDownColumnForm form,
			@ModelAttribute("SessionUser") User s_user,
			@RequestParam(name = "categoryId", required = false) String categoryId,
			BindingResult result) {
		// XXX BindingResultは@Validatedの直後の引数にする

		// Set view
		mav.setViewName("/login/admin/category/narrowdown_column");

		log.debug(form.toString());

		// エラー判定
		if (!result.hasErrors()) {
			JSONArray jsonArray = new JSONArray(form.getJson());
			List<String[]> list = new ArrayList<>();
			for(Object obj : jsonArray.toList()) {
				List<Object> oList = (List<Object>)obj;
				String[] arr = new String[(oList).size()];
				int cnt = 0;
				for(Object o : oList) {
					if (o == null) {
						arr[cnt] = null;
					} else {
						arr[cnt] = ((String)o).trim();
					}
					cnt++;
				}
				list.add(arr);
			}
			{
				// 変更前のリスト取得。同一のIDが無ければ削除
				ErrorObject err = new ErrorObject();
				List<NarrowDownColumn> clist = service.getCategoryColumn(categoryId, null, err);
				if (list.size() > 2) {
					for(NarrowDownColumn c : clist) {
						boolean isFind = false;
						// 列を追加削除されると順番が変わるので、全列を比較。
						for(String tmp : list.get(2)) { // handsontableの隠し行にidがある。
							if (tmp != null && c.getId().equals(tmp)) isFind = true;
						}
						if (isFind == false) {
							// 項目入れ替え。一旦すべて削除
							service.deleteColumn(c.getId());
							// 本番も削除。
							service.deleteProdColumn(c.getId());
							// Valueも削除
							service.deleteColumnValue(c.getId());
						}
					}
				} else {
					// 全削除。3行目が無い = すべて新規のhandsontable
					service.deleteCategoryColumn(categoryId);
					service.deleteCategoryValue(categoryId);

					List<Category> p = categoryService.getParents(categoryId, null, err);
					if (p != null) {
						for(Category pC : p) {
							service.deleteCategoryColumn(pC.getId());
							service.deleteCategoryValue(pC.getId());
						}
					}
				}
			}
			int idx = 0;
			for(String str : list.get(0)) {
				NarrowDownColumn col = new  NarrowDownColumn();
				col.setActive(form.isActive());
				col.setCategoryId(form.getCategoryId());
				col.setTitle(str);
				col.setSelect(list.get(1)[idx]);
				if (list.size() > 2 && list.get(2)[idx] != null && list.get(2)[idx].isEmpty() == false) {
					col.setId(list.get(2)[idx]);
				}
				col.setOrder(idx);
				// 保存
				ErrorObject obj = service.saveColumn(col);
				mav.addObject("is_success", !obj.isError());
				idx++;
			}
			service.refreshNarrowDownColumns(); // Bean更新
		} else {
			// 戻りのページ この場合はedit.htmlなので何もしない
		}
		ErrorObject err = new ErrorObject();
		Category c = categoryService.getWithSeries(categoryId, false, err);
		if (c != null) {
			form = getColumnForm(categoryId);
		}

		// フォームを更新(再編集用)
		mav.addObject(form);
		mav.addObject("category", c);
		mav.addObject("categoryId", categoryId);
		setBreadcrumb(mav, c);
		return mav;
	}
	
	@RequestMapping(value = "/value/post", method = RequestMethod.POST)
	public ModelAndView postValue(
			ModelAndView mav,
			@Validated @ModelAttribute("narrowDownValueForm") NarrowDownValueForm vform,
			@RequestParam(name = "categoryId", required = false) String categoryId,
			BindingResult result) {
		// XXX BindingResultは@Validatedの直後の引数にする

		// Set view
		mav.setViewName("/login/admin/category/narrowdown_value");

		log.debug(vform.toString());

		ErrorObject err = new ErrorObject();
		// エラー判定
		if (!result.hasErrors()) {
			List<NarrowDownColumn> clist = service.getCategoryColumn(categoryId, null, err);
			List<NarrowDownSeriesForm> sList = vform.getSeriesList();
			int cnt = 0;
			for(NarrowDownColumn c : clist) {
				c.setValues(null);
				for(NarrowDownSeriesForm s : sList) {
					String id = null;
					if (s.getIdList().size() > cnt) id = s.getIdList().get(cnt);
					if (id == null || id.isEmpty()) {
						/// IDがカラなら新規
						NarrowDownValue v = new NarrowDownValue();
						v.setId(null);
						v.setCategoryId(categoryId);
						v.setSeriesId(s.getSeriesId());
						v.setColumnId(c.getId());
						if (c.getSelect().equals("range")) {
							try {
								String tmp = s.getParams().get(cnt).trim();
								String[] arr = tmp.split("-");
								if (arr.length == 2) {
									v.setStart(Integer.parseInt(arr[0]));
									v.setEnd(Integer.parseInt(arr[1]));
								} else {
									if (tmp != null && tmp.isEmpty() == false) {
										err.setCode(ErrorCode.E10001);
										err.setMessage("invalid range.");
									}
								}
								v.setParam(null);
								
								c.setParamRange(tmp);
							} catch (Exception e) {
								log.error("postValue()"+e.toString());
								err.setCode(ErrorCode.E10001);
								err.setMessage(e.getMessage());
								result.rejectValue("seriesList", "my.validation.empty", null, "invalid range.");
								break;
							}
							
						} else {
							v.setStart(0);
							v.setEnd(0);
							v.setParam(s.getParams().get(cnt).trim().split(","));

							c.setParamValue(v.getParam());
						}
						v.setActive(true);
						service.saveValue(v);
					} else {
						NarrowDownValue v = service.getValue(id, err);
						if (v != null) {
							if (c.getSelect().equals("range")) {
								try {
									String tmp = s.getParams().get(cnt).trim();
									String[] arr = tmp.split("-");
									if (arr.length == 2) {
										v.setStart(Integer.parseInt(arr[0]));
										v.setEnd(Integer.parseInt(arr[1]));
									} else {
										if (tmp != null && tmp.isEmpty() == false) {
											// カラ以外ならエラー設定
											err.setCode(ErrorCode.E10001);
											err.setMessage("invalid range.");
										}
									}
									v.setParam(null);
	
									c.setParamRange(tmp);
								} catch (Exception e) {
									log.error("postValue() L377="+e.toString());
									result.rejectValue("seriesList", "my.validation.empty", null, "invalid range.");
									break;
								}
							} else {
								v.setStart(0);
								v.setEnd(0);
								v.setParam(s.getParams().get(cnt).trim().split(","));

								c.setParamValue(v.getParam());
							}
							service.saveValue(v);
						} else {
							log.error("NarrowDownValue is Empty. id="+id);
						}
					}
				}
				// カテゴリ内の全体のパラメータ設定
				service.saveColumn(c);
				cnt++;
			}
		} else {
			// 戻りのページ この場合はedit.htmlなので何もしない
		}
		mav.addObject("is_success", !err.isError());

		Category c = categoryService.getWithSeries(categoryId, null, err);
		if (c != null) {
			vform = getValueForm(c);
		}

		// フォームを更新(再編集用)
		mav.addObject(vform);
		mav.addObject("category", c);
		mav.addObject("categoryId", categoryId);
		setBreadcrumb(mav, c);
		return mav;
	}

	
	@PostMapping(value = "/valueCSV/post")
	public ModelAndView uploadFile(
			ModelAndView mav,
			@RequestParam("file") MultipartFile uploadFile,
			@RequestParam(name = "categoryId", required = false) String categoryId) {
		// Set view
		mav.setViewName("/login/admin/category/narrowdown_value");
		
		ErrorObject err = new ErrorObject();
		Category c = categoryService.getWithSeries(categoryId, null, err);
		NarrowDownValueForm checkform = null;
		if (c != null) {
			checkform = getValueForm(c);
		}
		try {
			BufferedReader br = null;
			if (c.getLang().equals("ja-jp") || c.getLang().indexOf("en-") > -1) {
				br = new BufferedReader(new InputStreamReader(uploadFile.getInputStream(), "MS932"));
			} else {
				br = new BufferedReader(new InputStreamReader(uploadFile.getInputStream(), StandardCharsets.UTF_8));
			}
			String line;
			int cnt = 0;
			List<NarrowDownSeriesForm> sList = checkform.getSeriesList();
			List<NarrowDownColumn> colList = service.getCategoryColumn(categoryId, null, err);

			char separator = ',';
			char quotechar = '"';
			char escape = '\\';

			CSVReader csvreader = new CSVReader(br, separator, quotechar, escape);

			List<String[]> list = csvreader.readAll();

			csvreader.close();
			
			for (; cnt < list.size(); cnt++) {
				String[] split = list.get(cnt);
		        // 1行目で言語と項目チェック
		        if (cnt == 0) {
		        	if (split.length != checkform.getColumns().size()+2) {
		        		mav.addObject("is_error", "ERROR! different column count.");
		        		err.setCode(ErrorCode.E10001);
						err.setMessage("ERROR! different column count.");
		        		break;
		        	}
		        	if (split[0].equals(categoryId) == false) {
		        		mav.addObject("is_error", "ERROR! different categoryId.");
		        		err.setCode(ErrorCode.E10001);
						err.setMessage("ERROR! different categoryId.");
		        		break;
		        	}
		        	if (split[1].equals(c.getLang()) == false) {
		        		mav.addObject("is_error", "ERROR! different lang.");
		        		err.setCode(ErrorCode.E10001);
						err.setMessage("ERROR! different lang.");
		        		break;
		        	}
		        	int count = 2;
		        	for(; count < split.length; count++) {
		        		if (colList.get(count-2).getTitle().equals(split[count].replace("\"", "")) == false) {
			        		mav.addObject("is_error", "ERROR! different column. count="+count);
			        		err.setCode(ErrorCode.E10001);
							err.setMessage("ERROR! different column.count="+count);
			        		break;
		        		}
		        	}
		        	// ここまでOKならNarrowDownColumn.valuesを一旦クリア。
		        	for(NarrowDownColumn col : colList) {
		        		col.setValues(null);
		        	}
		        } else {
		        	NarrowDownSeriesForm form = sList.get(cnt-1);
		        	if (split[0].equals(form.getSeriesId()) == false) {
		        		mav.addObject("is_error", "ERROR! different SeriesId. id="+split[0]);
		        		err.setCode(ErrorCode.E10001);
						err.setMessage("ERROR! different SeriesId. id="+split[0]);
		        		break;
		        	}
		        	if (split[1].replace("\"", "").equals(form.getModelNumber()) == false) {
		        		mav.addObject("is_error", "ERROR! different ModelNumber. modelNumber="+split[1]);
		        		err.setCode(ErrorCode.E10001);
						err.setMessage("ERROR! different ModelNumber. modelNumber="+split[1]);
		        		break;
		        	}
		        	int count = 2;
		        	List<String> idList = form.getIdList();
		        	try {
			        	for(; count < split.length; count++) {
			        		String param = split[count].replace("\"", "");
			        		NarrowDownValue v = service.getValue(idList.get(count-2), err);
				        	NarrowDownColumn col = colList.get(count-2);
				        	if (v == null) {
				        		// 新規
				        		v = new NarrowDownValue();
				        		v.setColumnId(col.getId());
				        		v.setCategoryId(categoryId);
				        		v.setSeriesId(split[0]);
				        		if (col.getSelect().equals("range")) {
				        			v.setRangeParam(param);
			        				col.setParamRange(param);
				        		} else {
				        			String[] arr = param.split(",");
				        			int ct = 0;
				        			for(String tmp : arr) {
				        				arr[ct] = tmp.trim();
				        				ct++;
				        			}
				        			v.setParam(arr);
			        				col.setParamValue(param.split(","));
				        		}
				        		v.setActive(true);
		        				service.saveValue(v);
				        	} else {
				        		if (col.getSelect().equals("range")) {
				        			if (param.equals(v.getRangeParam()) == false) {
				        				v.setRangeParam(param);
				        				service.saveValue(v);
				        				col.setParamRange(param);
				        			}
				        		} else {
				        			String[] arr = param.split(",");
				        			int ct = 0;
				        			for(String tmp : arr) {
				        				arr[ct] = tmp.trim();
				        				ct++;
				        			}
			        				v.setParam(arr);
			        				service.saveValue(v);
			        				col.setParamValue(param.split(","));
				        		}
				        	}
			        	}
		        	} catch(Exception e) {
		        		mav.addObject("is_error", "ERROR! Exception e.message="+e.getMessage());
		        		err.setCode(ErrorCode.E10001);
						err.setMessage("ERROR! Exception e.message="+e.getMessage());
		        	}
		        }
			} // end for( cnt < list.size
		    if (!err.isError()) {
			    for (NarrowDownColumn col : colList) {
			    	service.saveColumn(col);
			    }
		    }
	    } catch (IOException e) {
	      throw new RuntimeException("ファイルが読み込めません", e);
	    }

		mav.addObject("is_success", !err.isError());
		if (err.isError()) {
			mav.addObject("is_error", "Error!"+err.getMessage());
		}

		NarrowDownValueForm vform = null;
		if (c != null) {
			vform = getValueForm(c);
		}

		// フォームを更新(再編集用)
		mav.addObject(vform);
		mav.addObject("category", c);
		mav.addObject("categoryId", categoryId);
		setBreadcrumb(mav, c);
		return mav;
	}
	
	// ===== private =====
	private NarrowDownColumnForm getColumnForm(String  categoryId) {
		NarrowDownColumnForm ret = new NarrowDownColumnForm();
		ErrorObject err = new ErrorObject();
		List<NarrowDownColumn> list = service.getCategoryColumn(categoryId, null, err);
		if (list != null) {
			ret.setCategoryId(categoryId);
			List<String> tmp = new ArrayList<>();
			List<String> tmp2 = new ArrayList<>();
			List<String> tmp3 = new ArrayList<>();
			for(NarrowDownColumn c : list) {
				tmp.add(c.getTitle());
				tmp2.add(c.getSelect());
				tmp3.add(c.getId());
			}
			if (list.size() > 0) {
				ret.setActive(list.get(0).isActive());
				JSONArray jsonArray = new JSONArray();
				jsonArray.put(0,tmp);
				jsonArray.put(1, tmp2);
				jsonArray.put(2, tmp3);
				ret.setJson(jsonArray.toString());
			} else {
				ret.setActive(true); // 新規ならtrue
			}
		}
		
		return ret;
	}
	private NarrowDownValueForm getValueForm(Category c) {
		NarrowDownValueForm ret = new NarrowDownValueForm();
		ErrorObject err = new ErrorObject();
		if (c != null) {
			List<NarrowDownSeriesForm> sList = new ArrayList<>();
			List<Series> list = c.getSeriesList();
			
			List<NarrowDownColumn> cList = service.getCategoryColumn(c.getId(), null, err);
			List<String> setList = new ArrayList<>();
			for(NarrowDownColumn col : cList) {
				setList.add(col.getTitle());
			}
			ret.setColumns(setList);
			
			for(Series s : list) {
				NarrowDownSeriesForm sForm = new NarrowDownSeriesForm();
				sForm.setModelNumber(s.getModelNumber());
				sForm.setName(s.getName());
				sForm.setSeriesId(s.getId());
				List<NarrowDownValue> vList = service.getCategorySeriesValue(c.getId(), s.getId(), null, err);
				if (vList.size() == cList.size()) { // 同じなら
					List<String> params = new ArrayList<>();
					List<String> ids = new ArrayList<>();
					int cnt = 0;
					for(NarrowDownValue v : vList) {
						if (cList.get(cnt).getSelect().equals("range")) {
							params.add(v.getRangeParam());
						} else {
							params.add(String.join(",", v.getParam()));
						}
						ids.add(v.getId());
						cnt++;
					}
					sForm.setParams(params);
					sForm.setIdList(ids);
				} else {
					// 一致するところは設定。一致しなければ空をセット
					List<String> params = new ArrayList<>();
					List<String> ids = new ArrayList<>();
					for(NarrowDownColumn col : cList) {
						boolean isFind = false;
						for(NarrowDownValue val : vList) {
							if (val.getColumnId().equals(col.getId())) {
								if (col.getSelect().equals("range")) {
									params.add(val.getRangeParam());
								} else {
									params.add(String.join(",", val.getParam()));
								}
								ids.add(val.getId());
								isFind = true;
								break;
							}
						}
						if (isFind == false) {
							params.add("");
							ids.add("");
						}
					}
					sForm.setParams(params);
					sForm.setIdList(ids);
				}
				sList.add(sForm);
			}
			ret.setSeriesList(sList);
		}
		
		return ret;
	}

	private void setBreadcrumb(ModelAndView mav, Category c) {
		List<Category> breadcrumb = null;
		ErrorObject err = new ErrorObject();

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

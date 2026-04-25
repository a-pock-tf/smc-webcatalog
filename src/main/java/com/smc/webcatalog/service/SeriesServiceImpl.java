package com.smc.webcatalog.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.StringTokenizer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.mongodb.MongoException;
import com.smc.exception.ModelConditionDifferentException;
import com.smc.exception.ModelExistsException;
import com.smc.exception.ModelNotFoundException;
import com.smc.omlist.service.OmlistServiceImpl;
import com.smc.webcatalog.config.AppConfig;
import com.smc.webcatalog.config.ErrorCode;
import com.smc.webcatalog.dao.CategoryRepository;
import com.smc.webcatalog.dao.CategorySeriesRepository;
import com.smc.webcatalog.dao.CategorySeriesTemplateImpl;
import com.smc.webcatalog.dao.CategoryTemplateImpl;
import com.smc.webcatalog.dao.SeriesFaqRepository;
import com.smc.webcatalog.dao.SeriesLinkMasterTemplateImpl;
import com.smc.webcatalog.dao.SeriesLinkRepository;
import com.smc.webcatalog.dao.SeriesLinkTemplateImpl;
import com.smc.webcatalog.dao.SeriesRepository;
import com.smc.webcatalog.dao.SeriesTemplateImpl;
import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.CategorySeries;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Series;
import com.smc.webcatalog.model.SeriesHtml;
import com.smc.webcatalog.model.SeriesLink;
import com.smc.webcatalog.model.SeriesLinkMaster;
import com.smc.webcatalog.model.Template;
import com.smc.webcatalog.model.TemplateCategory;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.util.LibHtml;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SeriesServiceImpl implements SeriesService {

	@Autowired
	SeriesRepository repo;

	@Autowired
	SeriesTemplateImpl temp;

	@Autowired
	SeriesLinkRepository sLinkRepo;

	@Autowired
	SeriesLinkTemplateImpl sLinkTemp;

	@Autowired
	SeriesLinkMasterTemplateImpl sLinkMasterTemp;

	@Autowired
	CategorySeriesRepository csRepo;

	@Autowired
	CategorySeriesTemplateImpl csTemp;

	@Autowired
	CategoryRepository cRepo;

	@Autowired
	CategoryTemplateImpl cTemp;

	@Autowired
	TemplateCategoryServiceImpl templateCategoryService;

	@Autowired
	OmlistServiceImpl omlistService;
    
	@Autowired
    SeriesFaqRepository faqRepo;
    
	@Autowired
    MessageSource messagesource;

	@Autowired
	LibHtml html;

	@Override
	public ErrorObject save(Series series) {
		ErrorObject ret = new ErrorObject();
		try {
			// 新規の場合
			if (StringUtils.isEmpty(series.getId())) {
				// 同名チェック
				if (isModelNumberExists(series.getModelNumber(), series.getState(), null, ret)) throw new ModelExistsException("SeriesID is exists.");
				else if (ret.isError()) return ret;
				series.setId(null);
			} else {
				Series s = get(series.getId(), ret);
				if (ret.isError()) return ret;

				if (s.getModelNumber().equals(series.getModelNumber()) == false) {
					if (isModelNumberExists(series.getModelNumber(), series.getState(), null,ret)) throw new ModelExistsException("SeriesID is exists.");
				}
			}

			series.setMtime(new Date()); // Always update mtime

			series = repo.save(series);
			ret.setCount(1);

		} catch (ModelExistsException e) {
			ret.setCode(ErrorCode.E10001);
			ret.setMessage(e.getMessage());
		} catch (MongoException e) {
			ret.setCode(ErrorCode.E50001);
			ret.setMessage(e.getMessage());
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public boolean isNameExists(Series series, ErrorObject err) {
		boolean ret = false;
		try {
			Optional<Series> os = repo.findByName(series.getName(), series.getLang(), series.getState());
			if (os.isPresent()) {
				ret = true;
			}
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public boolean isModelNumberExists(String seid, ModelState state, @Nullable Boolean active, ErrorObject err) {
		boolean ret = false;
		try {
			Optional<Series> os = repo.findByModelNumber(seid, state, active);
			if (os.isPresent()) {
				ret = true;
			}
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public	Series getFromModelNumber(String seid, ModelState state, ErrorObject err) {
		Series ret = null;
		try {
			Optional<Series> os = repo.findByModelNumber(seid, state, null);
			if (os.isPresent()) {
				ret = os.get();
			}
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public Series get(String id, ErrorObject err) {
		Series ret = null;
		try {
			ret =repo.findById(id).orElseThrow(() -> new ModelNotFoundException("Series.id=" + id));
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public Series getWithCategory(String id, @Nullable Boolean active, ErrorObject err) {
		Series ret = null;
		try {
			Series s = get(id, err);
			if (active != null && active != s.isActive()) {
				throw new ModelNotFoundException("param.active=" + active + ":DB active=" + s.isActive());
			}
			ret = s;
			List<CategorySeries> csList = csTemp.findBySeriesId(id);
			ret.setCategorySeries(csList);
			List<SeriesLink> sLinkList = sLinkTemp.findBySeriesId(id);
			ret.setLink(sLinkList);
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public Series getWithLink(String id, @Nullable Boolean active, ErrorObject err) {
		Series ret = null;
		try {
			Series s = get(id, err);
			if (active != null && active != s.isActive()) {
				throw new ModelNotFoundException("param.active=" + active + ":DB active=" + s.isActive());
			}

			ret = s;
			List<SeriesLink> sLinkList = sLinkTemp.findBySeriesId(id);
			List<SeriesLinkMaster> masterList = sLinkMasterTemp.listAll(null);
			for(SeriesLink k : sLinkList) {
				for(SeriesLinkMaster m : masterList) {
					if (k.getLinkMaster().getId().equals(m.getId())) {
						k.setLinkMaster(m);
					}
				}
			}
			ret.setLink(sLinkList);
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public List<SeriesLink> getLink(String id, ErrorObject err){
		List<SeriesLink> ret = null;
		try {
			ret = sLinkTemp.findBySeriesId(id);
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public List<Series> getStateRefId(Series c,  ModelState state, ErrorObject err) {
		List<Series> ret = null;
		try {
			if (state.equals(ModelState.TEST)) {
				Series s = repo.findById(c.getStateRefId()).orElseThrow(() -> new ModelNotFoundException("Series.id=" + c.getStateRefId()));
				ret = new ArrayList<Series>();
				ret.add(s);
			} else {
				ret = temp.findByStateRefId(c.getId(), state);
			}
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public Series getLangRefId(Series c, String lang, ErrorObject err) {
		Series ret = null;
		try {
			if (c.getLang().equals(lang)) {
				ret = c;
			} else {
				List<Series> list = null;
				if (StringUtils.isEmpty(c.getLangRefId())) {
					list = repo.findByLangRefId(c.getId(), c.getState());
				}
				else {
					list = repo.findByLangRefId(c.getLangRefId(), c.getState());
				}
				if (list != null && list.size() > 0) {
					for(Series ca : list) {
						if (ca.getLang().equals(lang)) {
							ret = ca;
							break;
						}
					}
					// langRefIdが有って、langにlangRefIdが無いものを選択された場合
					if (ret == null) {
						ret = repo.findById(c.getLangRefId()).orElseThrow(() -> new ModelNotFoundException("Series.id=" + c.getStateRefId()));
					}
				}
			}
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public List<Series> listLangRef(String id, ErrorObject err) {
		return temp.findByLangRefId(id);
	}

	@Override
	public List<Series> listSlug(String lang, String slug, int level, Boolean active, ErrorObject err) {
		List<Series> ret = null;
		// 該当するSlugのCategoryを取得し、CategoryのIDのシリーズ一覧を戻す。
		List<Category> list = cTemp.findBySlug(slug, lang, ModelState.PROD, null, active);
		if (list != null && list.size() > 0) {
			Category c  = null;
			if (list.size() == 1) c = list.get(0);
			else {
				if (list.get(0).getId().equals(list.get(1).getParentId())) c = list.get(1);
				else  c = list.get(0);
			}
			ret = new LinkedList<Series>();
			Iterable<CategorySeries> cs = csRepo.findAllByCategoryId(c.getId());
			for (CategorySeries categorySeries : cs) {
				ret.addAll(categorySeries.getSeriesList());
			}

		}
		return ret;
	}

	@Override
	public List<Series> listSlug(Category c,  Boolean active, ErrorObject err){
		List<Series> ret = null;
		ret = new LinkedList<Series>();
		Iterable<CategorySeries> cs = csRepo.findAllByCategoryId(c.getId());
		for (CategorySeries categorySeries : cs) {
			List<Series> list = categorySeries.getSeriesList();
			for(Series s : list) {
				if (s.isActive()) {
					ret.add(s);
				}
			}
		}
		return ret;
	}
	@Override
	public List<Series> listAll200(String lang, ModelState state, Boolean active, ErrorObject err) {

		return listAll(lang, state, active, 200, err);
	}

	@Override
	public List<Series> listAll(String lang, ModelState state, Boolean active, Integer limit, ErrorObject err) {
		List<Series> ret = null;
		try {
			ret = temp.listAll(lang, state, active, limit);
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public List<Series> getPage(String[] kwArr, String lang, ModelState state, int page, int max, ErrorObject err) {
		List<Series> ret = null;
		try {
			if (kwArr != null) {
				if (kwArr.length >= 1 && kwArr[0].trim().isEmpty() == false) {
					ret = temp.getPage(kwArr, lang, state, page, max);
				}
			}
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public int searchCount(String[] kwArr, String lang, ModelState state, ErrorObject err) {
		int ret = 0;
		try {
			if (kwArr != null) {
				if (kwArr.length >= 1 && kwArr[0].trim().isEmpty() == false) {
					boolean st = true;
					Boolean active = true;
					if (state.equals(ModelState.TEST)) {
						st = false;
						active = null;
					}
					ret = (int)temp.searchAndOrCount(kwArr, lang, st, active);
				}
			}
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public List<Series> search(String keyword, String lang, ModelState state, @Nullable Boolean active, ErrorObject err) {
		List<Series> ret = null;
		try {
			if (keyword != null) {
				keyword = keyword.replace("　", "");
			}
			String[] arr = keyword.split(" ");
			ret = temp.search(arr, lang, state, active);
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public List<Series> indexSearch(String h, String lang, ModelState state, @Nullable Boolean active, ErrorObject err) {
		List<Series> ret = null;
		try {
			ret = temp.indexSearch(h, lang, state, active);
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public ErrorObject sort(String id, List<String> ids) {
		ErrorObject ret = new ErrorObject();
		try {
			if (id.isEmpty() || ids == null || ids.isEmpty()) {
				 throw new ModelNotFoundException("sort() id or ids is empty." );
			}
			Optional<CategorySeries> cs = csRepo.findByCategoryId(id);
			if (cs.isPresent()) {
				CategorySeries saveCs = cs.get();
				List<Series> list = saveCs.getSeriesList();
				List<Series> saveSeries = new ArrayList<Series>();

				for(String i : ids) {
					boolean isFind = false;
					for(Series c : list) {
						if (c.getId().contentEquals(i)) {
							c.setMtime(new Date());
							saveSeries.add(c);
							isFind = true;
							break;
						}
					}
					if (isFind == false) throw new ModelNotFoundException("sort() ids not Found. id="+i );
				}
				if (saveSeries.size() != list.size()) {
					// idsに含まれないlistのorderを再設定
					for(Series c : list) {
						boolean isFind = false;
						for(Series s : saveSeries) {
							if (s.getId().equals(c.getId())) {
								isFind = true;
								break;
							}
						}
						if (isFind == false) {
							c.setMtime(new Date());
							saveSeries.add(c);
						}
					}
				}
				if (saveSeries.size() > 0) {
					saveCs.setSeriesList(saveSeries);
					CategorySeries res = csRepo.save(saveCs);
					ret.setCount(saveSeries.size());
				}
			}
			else {
				throw new ModelNotFoundException("sort() id hasnot Series." );
			}

		} catch (ModelNotFoundException e) {
			ret.setCode(ErrorCode.E10001);
			ret.setMessage(e.getMessage());
		} catch (ModelExistsException e) {
			ret.setCode(ErrorCode.E10003);
			ret.setMessage(e.getMessage());
		} catch (ModelConditionDifferentException e) {
			ret.setCode(ErrorCode.E10005);
			ret.setMessage(e.getMessage());
		} catch (MongoException e) {
			ret.setCode(ErrorCode.E50001);
			ret.setMessage(e.getMessage());
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public ErrorObject changeStateToProd(String id, User u) {
		ErrorObject ret = new ErrorObject();
		try {
			Series se = get(id, ret);
			if (ret.isError()) return ret;
			if (se.getState().equals(ModelState.TEST) == false) {
				throw new ModelConditionDifferentException("Series is not TEST.");
			}
			// 取得したIDにPRODが無ければ作成
			List<Series> list =  temp.findByStateRefId(id, ModelState.PROD);
			Series s = null;
			if (list == null || list.size() == 0) {
				s = se;
				s.setId(null);
				Date dt = new Date();
				s.setUpdateParam(se);
				s.setCtime(dt);
				s.setMtime(dt);
				s.setStateRefId(id);
				s.setState(ModelState.PROD);
				// TESTにlangRefIdがあればPRODのlangRefIdを設定。
				if (se.getLangRefId() != null && se.getLangRefId().isEmpty() == false) {
					List<Series> prodList = temp.findByStateRefId(se.getLangRefId(), ModelState.PROD);
					if (prodList != null && prodList.isEmpty() == false) {
						s.setLangRefId(prodList.get(0).getId());
					}
				}
				s = repo.save(s);
			} else {
				// PRODが既に作成されていればUpdate
				s = list.get(0);
				s.setUpdateParam(se);
				s = repo.save(s);
			}
			// Series_link
			if (s != null && s.getId().isEmpty() == false) {
				sLinkTemp.deleteBySeriesId(s.getId()); // PRODを一旦削除
				List<SeriesLink> sList = sLinkTemp.findBySeriesId(id); // TESTの一覧を取得
				List<SeriesLink> addList = new LinkedList<SeriesLink>();
				for(SeriesLink sl : sList) {
					sl.setSeriesId(s.getId());
					sl.setState(ModelState.PROD); // TESTをPRODへ変換
					sl.setId(null);
					addList.add(sl);
				}
				if (addList.size() > 0) sLinkRepo.saveAll(addList);
			}
			ret.setCount(1);
			String prodId = s.getId();

			// TESTのCategorySeriesがあり、PRODのCategoryがある場合はPRODのCategorySeriesを作成
			List<CategorySeries> listCs = csTemp.findBySeriesId(id);
			if (listCs.size() > 0) {
				for(CategorySeries cs : listCs) {
					String cId = cs.getCategoryId();
					Optional<Category> testCa = cRepo.findById(cId);
					if (testCa.isPresent()) {
						Category tCa = testCa.get();
						Optional<Category> oCa = cTemp.findByStateRefId(tCa.getId(), ModelState.PROD, tCa.getType());
						if (oCa.isPresent()) {
							String prodCaId = oCa.get().getId();
							Optional<Category> prodOpCa = cRepo.findById(prodCaId);
							if (prodOpCa.isPresent()) {
								Optional<CategorySeries> prodCsList = csRepo.findByCategoryId(prodOpCa.get().getId());
								if (prodCsList.isPresent()) {
									// PRODのCategorySeriesがあり、Seriesが無ければ追加。
									boolean isFind = false;
									List<Series> seriesList = prodCsList.get().getSeriesList();
									for(Series ser : seriesList) {
										if (ser.getId().equals(prodId)) {
											isFind = true; // 既にPRODのCategorySeriesがあれば何もしない。
											break;
										}
									}
									if (isFind == false) {
										CategorySeries prodCs = prodCsList.get();
										List<Series> sList = prodCs.getSeriesList();
										sList.add(s);
										prodCs.setSeriesList(sList);
										csRepo.save(prodCs);
									}
								} else {
									// 無ければ新規登録
									CategorySeries newCs = new CategorySeries();
									newCs.setCategoryId(prodCaId);
									List<Series> sList = new ArrayList<Series>();
									sList.add(s);
									newCs.setSeriesList(sList);
									newCs.setId(null);
									csRepo.save(newCs);
								}
							}
						}
					}
				}
			}
		} catch (ModelNotFoundException e) {
			ret.setCode(ErrorCode.E10001);
			ret.setMessage(e.getMessage());
		} catch (ModelExistsException e) {
			ret.setCode(ErrorCode.E10003);
			ret.setMessage(e.getMessage());
		} catch (ModelConditionDifferentException e) {
			ret.setCode(ErrorCode.E10005);
			ret.setMessage(e.getMessage());
		} catch (MongoException e) {
			ret.setCode(ErrorCode.E50001);
			ret.setMessage(e.getMessage());
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public ErrorObject changeStateToArchive(String id, User u) {
		ErrorObject ret = new ErrorObject();
		try {
			Series se = get(id, ret);
			if (ret.isError()) return ret;
			if (se.getState().equals(ModelState.TEST) == false) {
				throw new ModelConditionDifferentException("Series id is not TEST.");
			}
			// 取得したIDを元にArchiveを作成。CategorySeriesもCategory.TESTとSeries.Archiveで新たに作成。
			Series s = se;
			s.setId(null);
			Date dt = new Date();
			s.setMtime(dt);
			s.setStateRefId(id);
			s.setState(ModelState.ARCHIVE);
			s = repo.save(s);

			// CategorySeriesは保持しないので、何もしない。
/*			// TESTのCategorySeriesがある場合のみ作成
			List<CategorySeries> csList = csTemp.findBySeriesId(id);
			if (csList.isEmpty() == false) {
				for(CategorySeries cs : csList) {
					String cId = cs.getCategoryId();
					Optional<Category> testCa = cRepo.findById(cId);
					if (testCa.isPresent()) {
						Category tCa = testCa.get();
						// 新規登録
						cs.setCategoryId(tCa.getId());
						List<Series> sList = new ArrayList<Series>();
						sList.add(s);
						cs.setSeriesList(sList);
						cs.setId(null);
						csRepo.save(cs);
					}
				}
			}*/
		} catch (ModelNotFoundException e) {
			ret.setCode(ErrorCode.E10001);
			ret.setMessage(e.getMessage());
		} catch (ModelExistsException e) {
			ret.setCode(ErrorCode.E10003);
			ret.setMessage(e.getMessage());
		} catch (ModelConditionDifferentException e) {
			ret.setCode(ErrorCode.E10005);
			ret.setMessage(e.getMessage());
		} catch (MongoException e) {
			ret.setCode(ErrorCode.E50001);
			ret.setMessage(e.getMessage());
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public ErrorObject changeStateToTest(String id) {
		ErrorObject ret = new ErrorObject();
		try {
			Series se = get(id, ret);
			if (ret.isError()) return ret;
			if (se.getState().equals(ModelState.ARCHIVE) == false) {
				throw new ModelConditionDifferentException("Series id is not ARCHIVE.");
			}

			// 取得したArchiveを元にTESTを作成。TESTをUpdate。
			// CategorySeriesは何もしない。（ARCHIVEは削除しないし、Series.TESTのIDも変えないため）
			Series testSeries = get(se.getStateRefId(), ret);
			if (ret.isError()) return ret;

			Series s = testSeries;
			Date dt = new Date();
			s.setName(se.getName());
			s.setNumber(se.getNumber());
			s.setModelNumber(se.getModelNumber());
			s.setKeyword(se.getKeyword());
			s.setBreadcrumb(se.getBreadcrumb());

			s.setDetail(se.getDetail());
			s.setAdvantage(se.getAdvantage());
			s.setOther(se.getOther());
			s.setSpec(se.getSpec());
			s.setCtime(se.getCtime());
			s.setMtime(dt);
			s.setOrder(se.getOrder());
			s.setStateRefId(null);

			s.setNotice(se.getNotice());
			s.setImageTop(se.getImageTop());
			s.setImageBottom(se.getImageBottom());

			s.setCad3d(se.isCad3d());
			s.setOrderMade(se.isOrderMade());
			s.setCustom(se.isCustom());
			s.setActive(se.isActive());
			s = repo.save(s);

		} catch (ModelNotFoundException e) {
			ret.setCode(ErrorCode.E10001);
			ret.setMessage(e.getMessage());
		} catch (ModelExistsException e) {
			ret.setCode(ErrorCode.E10003);
			ret.setMessage(e.getMessage());
		} catch (ModelConditionDifferentException e) {
			ret.setCode(ErrorCode.E10005);
			ret.setMessage(e.getMessage());
		} catch (MongoException e) {
			ret.setCode(ErrorCode.E50001);
			ret.setMessage(e.getMessage());
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
		}
		return ret;

	}
	@Override
	public ErrorObject checkDelete(String id) {
		ErrorObject ret = new ErrorObject();
		try {
			Series s = get(id, ret);
			if (ret.isError()) return ret;

			List<Series> list = temp.findByLangRefId(id);
			if (ret.isError()) return ret;
			if (list.size() > 0){
				throw new ModelConditionDifferentException("This series is referenced by other countries.");
			}

			if (s.getState().equals(ModelState.PROD)) {
				// PRODならActiveでなければ削除OK
				if (s.isActive()) {
					throw new ModelConditionDifferentException("This id is Prod and Active.");
				}
			} else {
				List<Series> listProd = temp.findByStateRefId(s.getId(), ModelState.PROD);
				if (listProd.size() > 0) {
					boolean isActive = false;
					for (Series se : listProd) {
						if (se.isActive()) {
							isActive = true;
							break;
						}
					}
					if (isActive) {
						throw new ModelConditionDifferentException("Prod is active.");
					}
				}
			}

		} catch (ModelConditionDifferentException e) {
			ret.setCode(ErrorCode.E10005);
			ret.setMessage(e.getMessage());
		} catch (MongoException e) {
			ret.setCode(ErrorCode.E50001);
			ret.setMessage(e.getMessage());
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
		}
		return ret;
	}
	@Override
	public ErrorObject delete(String id) {
		ErrorObject ret = new ErrorObject();
		int cnt = 0;
		try {
			Series s = get(id, ret);
			if (ret.isError()) return ret;
			ret = checkDelete(id);
			if (ret.isError()) return ret;

			if (s.getState().equals(ModelState.PROD)) {
				// PRODはPRODのみ削除。
				/*				{
					Optional<Series> refCa = repo.findById(s.getStateRefId());
					Series test = refCa.get();
					List<CategorySeries> csList = csTemp.findBySeriesId(test.getId());
					if (csList.isEmpty() == false) {
						csTemp.deleteSeriesFromSeriesList(test.getId());
					}
					List<SeriesLink> sLinkList = sLinkTemp.findBySeriesId(test.getId());
					if (sLinkList.isEmpty() == false) {
						sLinkTemp.deleteBySeriesId(test.getId());
					}
					repo.deleteById(test.getId());
					cnt++;
			}*/
				{
					List<CategorySeries> csList = csTemp.findBySeriesId(id);
					if (csList.isEmpty() == false) {
						csRepo.deleteSeriesFromSeriesList(id);
					}
					List<SeriesLink> sLinkList = sLinkTemp.findBySeriesId(id);
					if (sLinkList.isEmpty() == false) {
						sLinkTemp.deleteBySeriesId(id);
					}
					repo.deleteById(id);
					cnt++;
				}
			} else if (s.getState().equals(ModelState.ARCHIVE)) {
				// ARCHIVEなら削除OK
				List<CategorySeries> csList = csTemp.findBySeriesId(id);
				if (csList.isEmpty() == false) {
					csRepo.deleteSeriesFromSeriesList(id);
				}
				List<SeriesLink> sLinkList = sLinkTemp.findBySeriesId(id);
				if (sLinkList.isEmpty() == false) {
					sLinkTemp.deleteBySeriesId(id);
				}
				repo.deleteById(id);
				cnt++;
			} else {
				boolean isDelete = false;
				// TESTならPRODがActiveでなければ削除OK。PRODが無ければ削除OK。
				List<Series> listProd = temp.findByStateRefId(s.getId(), ModelState.PROD);
				if (listProd.size() > 0) {
					boolean isActive = false;
					for (Series se : listProd) {
						if (se.isActive()) {
							isActive = true;
							break;
						}
					}
					if (isActive) {
						throw new ModelConditionDifferentException("Prod is active.");
					} else {
						isDelete = true;
					}
				} else {
					isDelete = true;
				}
				if (isDelete) {
					for (Series se : listProd) {
						List<CategorySeries> cs = csTemp.findBySeriesId(se.getId());
						if (cs.isEmpty() == false) {
							csRepo.deleteSeriesFromSeriesList(se.getId());
						}
						List<SeriesLink> sLinkList = sLinkTemp.findBySeriesId(se.getId());
						if (sLinkList.isEmpty() == false) {
							sLinkTemp.deleteBySeriesId(se.getId());
						}
						repo.deleteById(se.getId());
						cnt++;
					}
					// TEST
					List<CategorySeries> cs = csTemp.findBySeriesId(s.getId());
					if (cs.isEmpty() == false) {
						csRepo.deleteSeriesFromSeriesList(s.getId());
					}
					List<SeriesLink> sLinkList = sLinkTemp.findBySeriesId(s.getId());
					if (sLinkList.isEmpty() == false) {
						sLinkTemp.deleteBySeriesId(s.getId());
					}
					repo.deleteById(s.getId());
				}
			}
			ret.setCount(cnt);
		} catch (ModelConditionDifferentException e) {
			ret.setCode(ErrorCode.E10005);
			ret.setMessage(e.getMessage());
		} catch (MongoException e) {
			ret.setCode(ErrorCode.E50001);
			ret.setMessage(e.getMessage());
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
		}
		return ret;
	}

	@Override
	public ErrorObject deleteHtml(Series s) {
		ErrorObject ret = new ErrorObject();
		// カテゴリ配下の該当SeirsIDを削除
		Series withC = getWithCategory(s.getId(), null, ret);
		if (!ret.isError()) {
			html.Init(getLocale(s.getLang()), messagesource);
			for(CategorySeries cs : withC.getCategorySeries()) {
				Optional<Category> oC = cRepo.findById(cs.getCategoryId());
				if (oC.isPresent()) {
					Optional<Category> oPC = cRepo.findById(oC.get().getParentId());
					if (oPC.isPresent()) {
						Category p = oPC.get();
						if (p.isRoot() == false) {
							html.deleteSeries(s.getLang(), p.getSlug(), oC.get().getSlug(), s.getModelNumber());
						} else {
							html.deleteSeries(s.getLang(), oC.get().getSlug(), null, s.getModelNumber());
						}
					} else {
						html.deleteSeries(s.getLang(), oC.get().getSlug(), null, s.getModelNumber());
					}
				}
				csRepo.delete(cs);
			}
		} else {
			ret.setMessage("Error! getWithCategory()");
		}
		return ret;
	}
	@Override
	public ErrorObject updateSlug(String id, String before, String after) {
		ErrorObject ret = new ErrorObject();
		String match = "%"+before+"%";
		try {
			Iterable<CategorySeries> listSeries = csRepo.findAllByCategoryId(id);
			for(CategorySeries cs : listSeries) {
				List<Series> list = cs.getSeriesList();
				for(Series s : list) {
					String str = "";
					String slug = s.getBreadcrumb();
					StringTokenizer st = new StringTokenizer(slug,"●");
					while(st.hasMoreTokens()){
						str+= "●" + str;
						List<String> line = new ArrayList<String>();
						StringTokenizer st2 = new StringTokenizer((String)st.nextToken(),"#");
						while(st2.hasMoreTokens()){
							String cattxt = (String)st2.nextElement();
							if (cattxt.indexOf(match) >= 0) {
								line.add(cattxt.replace(match, "%" + after + "%"));
							}
							else {
								line.add(cattxt);
							}
						}
						for(String n : line) {
							str+= n + "#";
						}
					}
					s.setBreadcrumb(str);
					repo.save(s);
				}
			}
		} catch (ModelNotFoundException e) {
			ret.setCode(ErrorCode.E10001);
			ret.setMessage(e.getMessage());
		} catch (ModelExistsException e) {
			ret.setCode(ErrorCode.E10003);
			ret.setMessage(e.getMessage());
		} catch (ModelConditionDifferentException e) {
			ret.setCode(ErrorCode.E10005);
			ret.setMessage(e.getMessage());
		} catch (MongoException e) {
			ret.setCode(ErrorCode.E50001);
			ret.setMessage(e.getMessage());
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
		}
		return ret;
	}


	// ========= SeriesLink =========
	@Override
	public ErrorObject linkUpsert(String id, List<SeriesLinkMaster> master, String[] link, ModelState state, User u) {
		ErrorObject ret = new ErrorObject();
		try {
			if (master != null && link != null && master.size() == link.length) {
				sLinkTemp.deleteBySeriesId(id);

				int cnt = 0;
				for(SeriesLinkMaster m : master) {
					if (link[cnt] != null && StringUtils.isEmpty(link[cnt]) == false) {
						SeriesLink s = new SeriesLink(m, m.getLang(), u, link[cnt], state);
						s.setSeriesId(id);
						sLinkRepo.save(s);
					}
					cnt++;
				}
			} else if(link == null) {
				log.info("linkUpsert link is NULL.");
			} else {
				throw new ModelConditionDifferentException("master and input box size is different.");
			}
		} catch (ModelNotFoundException e) {
			ret.setCode(ErrorCode.E10001);
			ret.setMessage(e.getMessage());
		} catch (ModelExistsException e) {
			ret.setCode(ErrorCode.E10003);
			ret.setMessage(e.getMessage());
		} catch (ModelConditionDifferentException e) {
			ret.setCode(ErrorCode.E10005);
			ret.setMessage(e.getMessage());
		} catch (MongoException e) {
			ret.setCode(ErrorCode.E50001);
			ret.setMessage(e.getMessage());
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
		}
		return ret;
	}


	@Override
	@Async("Thread2")
	public ErrorObject cad3DUpdate(Template t, List<String> list, String lang, List<Category> prodList) {
		// for TEST
/*		{
			try{
			Thread.sleep(20000);
			}catch (Exception e) {
				log.debug("cad3DUpdate() thread Exception.e="+e.getMessage());
			}
			log.debug("cad3DUpdate() thread end.");
			condition[0] = "END";
			return new ErrorObject();
		}*/
		ErrorObject ret = new ErrorObject();
		List<Series> saveList = new ArrayList<Series>();
		Locale loc = null;
		try {
			if (list != null && list.size() > 0) {
				// 差分を見る。変更があればcad3Dの表示変更
				List<Series> listSeries = listAll(lang, ModelState.PROD, true, 0, ret);
				log.info("listSeries = "+listSeries.toString() + " size()="+listSeries.size());
				if(ret.isError() == false) {
					if (lang != null && lang.equals("") == false) {
						loc = getLocale(lang);
					} else {
						loc = getLocale(prodList.get(0).getLang());
					}

					html.Init(loc, messagesource);
					log.debug("html.init()");
					SeriesHtml sHtml = new SeriesHtml(loc, messagesource, omlistService, faqRepo);

					for(Series s : listSeries) {
						log.info("series id="+s.getModelNumber());
						boolean cad3d = s.isCad3d();
						String modelNumber = s.getModelNumber();
						boolean find = false;
						ErrorObject err = new ErrorObject();
						for(String sid : list) {
							if (sid.equals(modelNumber)) {
								find = true;
								break;
							}
						}
						if ((find && cad3d == false) || (find == false && cad3d == true)) {
							log.debug(s.getModelNumber());
							Optional<Series> oT = repo.findById(s.getStateRefId());
							Series test = null;
							if (oT.isPresent()) test = oT.get();

							if (s.isCad3d()) {
								s.setCad3d(false);
								if (test != null) test.setCad3d(false);
							} else {
								s.setCad3d(true);
								if (test != null) test.setCad3d(true);
							}
							if (test != null) saveList.add(test);
							saveList.add(s);

							Series withC = getWithCategory(s.getId(), null, ret);

							List<CategorySeries> csList = withC.getCategorySeries();
							for(CategorySeries cs : csList) {
								Category c = null;
								Category c2 = null;
								Optional<Category> opC2 = cRepo.findById(cs.getCategoryId());
								if (opC2.isPresent()) {
									c2 = opC2.get();
									if (c2.getParentId() != null) {
										Optional<Category> opC = cRepo.findById(c2.getParentId());
										if (opC.isPresent() && opC.get().getParentId() != null) {
											c = opC.get();
										} else {
											c = c2;
											c2 = null;
										}
									}
								}
								if (c != null ) {
									String url = AppConfig.ProdRelativeUrl + c.getLang()+"/"+c.getSlug();
									if (c2 != null) {
										url+="/"+c2.getSlug();
									}
									// 特長無し
									s.setLink(getLink(s.getId(), err));
									String str = sHtml.get(s, c, c2, url, c.getLang(), false, false);
									html.outputHtml( c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/" + s.getModelNumber() + "/s.html", str);
									// 特長有り
									String catpan ="<a href='"+AppConfig.ProdRelativeUrl+c.getLang()+"/"+c.getSlug()+"'>"+c.getName()+"</a>";
									if (c2 != null) {
										catpan +="&nbsp;»&nbsp;";
										catpan +="<a href='"+ url +"'>"+c2.getName()+"</a>";
									}
									List<String> category = html.getCategoryMenu(c, c2, prodList);
									TemplateCategory tc = templateCategoryService.getCategory(c.getId(), err);
									String temp = tc.getTemplate();
									String sidebar =  tc.getSidebar();
									sidebar = StringUtils.replace(sidebar,"$$$category$$$",category.get(0));
									sidebar = StringUtils.replace(sidebar,"$$$category2$$$",category.get(1));
									temp = StringUtils.replace(temp,"$$$sidebar$$$",sidebar);
									String preContent = temp;
									preContent = StringUtils.replace(preContent,"$$$h1box$$$", ""); // 特長書き出し用。検索ボックス、h1box無し。
									preContent = StringUtils.replace(preContent,"$$$formbox$$$", ""); // 特長書き出し用。検索ボックス、h1box無し。
									String strA = sHtml.get(s, c, c2, url, c.getLang(), true, false);
									String output = StringUtils.replace(preContent, "$$$content$$$", s.getModelNumber());
									output = output.replaceFirst("<span class.* CUCA_IDS\">.*</span>", "").replaceFirst("child open", "");
									output = t.getHeader() + output + SeriesHtml._seriesCadModal + t.getFooter();
									html.outputHtml( c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/" +  s.getModelNumber() + "/index.html", output);
									html.outputHtml( c.getLang()+"/series/" +  s.getModelNumber() + "/index.html", output);
									// ガイド
									str = sHtml.get(s, c, c2, url, c.getLang(), false, true);
									str = "<div class=\"p_block\">\r\n" + str.replace("$$$backUrl$$$", "") + "\r\n</div><!-- .p_block -->";
									output = output.replaceFirst("<span class.* CUCA_IDS\">.*</span>", "").replaceFirst("child open", "");
									output = t.getHeader() + output + SeriesHtml._seriesCadModal + t.getFooter();
									output = StringUtils.replace(preContent, "$$$content$$$", str);
									html.outputHtml( c.getLang()+"/series/" + s.getModelNumber() + "/guide.html", output);
								} else {
									log.error("Category is null. SID="+s.getModelNumber());
								}

							}

						}
					}
					// DB変更
					if (saveList.size() > 0) repo.saveAll(saveList);
				}
			} else {
				throw new ModelConditionDifferentException("ERROR! SID list is null.");
			}
		} catch (ModelNotFoundException e) {
			ret.setCode(ErrorCode.E10001);
			ret.setMessage(e.getMessage());
		} catch (ModelExistsException e) {
			ret.setCode(ErrorCode.E10003);
			ret.setMessage(e.getMessage());
		} catch (ModelConditionDifferentException e) {
			ret.setCode(ErrorCode.E10005);
			ret.setMessage(e.getMessage());
		} catch (MongoException e) {
			ret.setCode(ErrorCode.E50001);
			ret.setMessage(e.getMessage());
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
			StringWriter sw = new StringWriter();
		    PrintWriter pw = new PrintWriter(sw);

		    pw.append("+++Start printing trace:\n");
		    e.printStackTrace(pw);
		    pw.append("---Finish printing trace");
		    System.out.println(sw.toString());
		}
		if (ret.isError()) {
			log.error(ret.getCode().toString());
			log.error(ret.getMessage());

		}

		return ret;
	}

	@Override
	public ErrorObject resetOrder(String lang, ModelState state, User u) {
		ErrorObject ret = new ErrorObject();
		List<Series> list = listAll(lang, state, null, null, ret);
		List<Series> saveList = new ArrayList<>();
		if (list != null) {
			for(Series s : list) {
				s.setOrder(99999);
				saveList.add(s);
			}
			if (saveList.size() > 0) {
				repo.saveAll(saveList);
			}
		}
		return ret;
	}

	@Override
	public ErrorObject updateOrder(List<Category> list, String lang, ModelState state, User u) {
		ErrorObject ret = new ErrorObject();
		if (list != null) {
			for(Category c : list) {
				if (c != null) {
					List<CategorySeries> csList = csTemp.findAllByCategoryId(c.getId());
					if (csList != null) {
						int i = 1;
						List<Series> saveList  = new ArrayList<>();
						for(CategorySeries cs : csList) {
							List<Series> sList = cs.getSeriesList();
							if (sList != null) {
								for(Series s : sList) {
									s.setOrder(i);
									saveList.add(s);
									i++;
								}
							}
						}
						if (saveList.size() > 0) {
							repo.saveAll(saveList);
						}
					}
				}
			}
		}
		return ret;
	}



	// ========= private =========
	private boolean deleteCategorySeriesInSeries(CategorySeries cs, String sId) {
		boolean ret = false;
		List<Series> list = cs.getSeriesList();
		List<Series> saveList = new ArrayList<Series>();
		for(Series s : list) {
			if (s.getId().equals(sId) == false) {
				saveList.add(s);
			}
			else {
				ret = true;
			}
		}
		if (ret) {
			cs.setSeriesList(saveList);
			csRepo.save(cs);
		}
		return ret;
	}

	private boolean setCategorySeriesInSeries(CategorySeries cs, String sId) {
		boolean ret = false;
		List<Series> list = cs.getSeriesList();
		List<Series> saveList = new ArrayList<Series>();
		for(Series s : list) {
			if (s.getId().equals(sId)) {
				ret = true;
			}
			saveList.add(s);
		}
		if (ret) {
			cs.setSeriesList(saveList);
			csRepo.save(cs);
		}
		return ret;
	}
	private Locale getLocale(String lang) {
		Locale loc = Locale.JAPANESE;
		if (lang.indexOf("en") > -1) loc = Locale.ENGLISH;
		else if (lang.indexOf("zh-tw") > -1) loc = Locale.TAIWAN;
		else if (lang.indexOf("zh") > -1)  loc = Locale.CHINESE;
		return loc;
	}

	@Override
	public List<Series> searchKeywordAndOr(String[] kwArr, String lang, int max, Boolean isProd, Boolean active) {
		List<Series> list = temp.searchAndOr(kwArr, lang, max, isProd,  active);
		return list;
	}

	@Override
	public long searchKeywordAndOrCount(String[] kwArr, String lang, Boolean isProd, Boolean active) {
		return temp.searchAndOrCount(kwArr, lang, isProd, active);
	}

}

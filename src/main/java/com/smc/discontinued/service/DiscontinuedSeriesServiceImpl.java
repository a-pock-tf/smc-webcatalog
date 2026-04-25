package com.smc.discontinued.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.mongodb.MongoException;
import com.smc.discontinued.dao.DiscontinuedCategoryTemplateImpl;
import com.smc.discontinued.dao.DiscontinuedSeriesRepository;
import com.smc.discontinued.dao.DiscontinuedSeriesTemplateImpl;
import com.smc.discontinued.model.DiscontinuedCategory;
import com.smc.discontinued.model.DiscontinuedModelState;
import com.smc.discontinued.model.DiscontinuedSeries;
import com.smc.exception.ModelConditionDifferentException;
import com.smc.exception.ModelExistsException;
import com.smc.exception.ModelNotFoundException;
import com.smc.webcatalog.config.ErrorCode;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.User;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DiscontinuedSeriesServiceImpl implements DiscontinuedSeriesService {

	@Autowired
	DiscontinuedSeriesRepository repo;

	@Autowired
	DiscontinuedSeriesTemplateImpl temp;

	@Autowired
	DiscontinuedCategoryTemplateImpl cTemp;


	@Autowired
    HttpServletRequest req;

	@Override
	public  List<DiscontinuedSeries> listAll(String lang, DiscontinuedModelState state, ErrorObject err) {

		List<DiscontinuedSeries> ret = null;
		try{

			ret = repo.listAll(lang, state);
		}catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		}
		return ret;
	}

	@Override
	public List<DiscontinuedSeries> listAllSortByEndDate(String lang, DiscontinuedModelState state, boolean asc,
			ErrorObject err) {
		List<DiscontinuedSeries> ret = null;
		try{

			ret = repo.listAllSortByEndDate(lang, state, asc);
		}catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		}
		return ret;
	}

	@Override
	public List<DiscontinuedSeries> listCategory(String categoryId, DiscontinuedModelState state, Boolean active, ErrorObject err) {
		List<DiscontinuedSeries> ret = null;
		try{

			ret = temp.listCategory(categoryId, state, active);
		}catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		}
		return ret;
	}

	@Override
	public List<DiscontinuedSeries> listLang(String lang, Boolean active, ErrorObject err) {
		List<DiscontinuedSeries> ret = null;
		try{

			ret = temp.listLang(lang, active);
		}catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		}
		return ret;
	}


	@Override
	public ErrorObject save(DiscontinuedSeries series) {
		ErrorObject ret = new ErrorObject();
		try {
			// 新規の場合
			if (StringUtils.isEmpty(series.getId())) {

				// この階層のorderの最大値を取得
				int order = listAll(series.getLang(), series.getState(), ret ).size();
				if (ret.isError()) return ret;
				order++;
				series.setOrder(order);
			} else {
				DiscontinuedSeries c = get(series.getId(), ret);
				if (ret.isError()) return ret;


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
			log.error(e.getMessage() + e.toString());
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		}
		return ret;
	}

	@Override
	public DiscontinuedSeries get(String id, ErrorObject err) {
		DiscontinuedSeries ret = null;
		try {
			ret =repo.findById(id).orElseThrow(() -> new ModelNotFoundException("DiscontinuedSeries.id=" + id));
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		}
		return ret;
	}
	@Override
	public DiscontinuedSeries getSeriesId(String seriesId, DiscontinuedModelState state, ErrorObject err) {
		DiscontinuedSeries ret = null;
		try {
			ret =repo.findBySeriesId(seriesId, state).orElseThrow(() -> new ModelNotFoundException("DiscontinuedSeries.seriesId=" + seriesId));
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		}
		return ret;
	}
	@Override
	public List<DiscontinuedSeries> getStateRefId(DiscontinuedSeries c, DiscontinuedModelState state, ErrorObject err) {
		List<DiscontinuedSeries> ret = null;
		try {
			if (state.equals(DiscontinuedModelState.TEST)) {
				DiscontinuedSeries s = repo.findById(c.getStateRefId()).orElseThrow(() -> new ModelNotFoundException("Series.id=" + c.getStateRefId()));
				ret = new ArrayList<DiscontinuedSeries>();
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
			log.error(e.getMessage() + e.toString());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		}
		return ret;
	}

	@Override
	public DiscontinuedSeries getLangRefId(DiscontinuedSeries s, String lang, ErrorObject err) {
		DiscontinuedSeries ret = null;
		try {
			if (s.getLang().equals(lang)) {
				ret = s;
			} else {
				List<DiscontinuedSeries> list = null;
				if (StringUtils.isEmpty(s.getLangRefId())) {
					list = repo.findByLangRefId(s.getId());
				}
				else {
					list = repo.findByLangRefId(s.getLangRefId(), s.getState());
				}
				if (list != null && list.size() > 0) {
					for(DiscontinuedSeries ca : list) {
						if (ca.getLang().equals(lang)) {
							ret = ca;
							break;
						}
					}
					// langRefIdが有って、langにlangRefIdが無いものを選択された場合
					if (ret == null) {
						ret = repo.findById(s.getLangRefId()).orElseThrow(() -> new ModelNotFoundException("Series.id=" + s.getStateRefId()));
					}
				}
			}
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		}
		return ret;
	}

	@Override
	public List<DiscontinuedSeries> listSlug(String lang, String slug, int level, Boolean active, ErrorObject err) {
		List<DiscontinuedSeries> ret = null;
		// 該当するSlugのCategoryを取得し、CategoryのIDのシリーズ一覧を戻す。
		Optional<DiscontinuedCategory> oC = cTemp.findBySlug(slug, lang, DiscontinuedModelState.PROD, active);
		if (oC != null && oC.isPresent()) {
			ret = repo.findAllByCategoryId(oC.get().getId());
		}
		return ret;
	}
	@Override
	public List<DiscontinuedSeries> listLangRef(String id, ErrorObject err) {
		return temp.findByLangRefId(id);
	}
	@Override
	public List<DiscontinuedSeries> search(String keyword, String lang, DiscontinuedModelState state, @Nullable Boolean active, ErrorObject err) {
		List<DiscontinuedSeries> ret = null;
		try {
			if (keyword != null) {
				keyword = keyword.replace("　", " ");
			}
			String[] arr = keyword.split(" ");
			String str = "";
			for(String k: arr) {
				str += k.trim() + "|";
			}
			str = str.substring(0, str.length()-1);
			ret = temp.search(str, lang, state, active);
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		}
		return ret;
	}
	
	@Override
	public boolean hitSearch(String keyword, String lang, DiscontinuedModelState state, @Nullable Boolean active, ErrorObject err) {
		boolean ret = false;
		try {
			if (keyword != null) {
				keyword = keyword.replace("　", " ");
			}
			String[] arr = keyword.split(" ");
			String str = "";
			for(String k: arr) {
				str += k.trim() + "|";
			}
			str = str.substring(0, str.length()-1);
			ret = temp.hitSearch(str, lang, state, active);
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		}
		return ret;
	}

	@Override
	public List<DiscontinuedSeries> indexSearch(String h, String lang, DiscontinuedModelState state, @Nullable Boolean active, ErrorObject err) {
		List<DiscontinuedSeries> ret = null;
		if (h == null || h.isEmpty() || h.length() > 1) return ret;
		try {
			ret = temp.indexSearch(h, lang, state, active);
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		}
		return ret;
	}
	
	@Override
	public boolean hitIndexSearch(String h, String lang, DiscontinuedModelState state, @Nullable Boolean active, ErrorObject err) {
		boolean ret = false;
		if (h == null || h.isEmpty() || h.length() > 1) return ret;
		try {
			ret = temp.hitIndexSearch(h, lang, state, active);
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
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
			List<DiscontinuedSeries> list = repo.findAllByCategoryId(id);
			if (list.size() > 0) {
				List<DiscontinuedSeries> saveSeries = new ArrayList<DiscontinuedSeries>();

				int cnt = 0;
				for(String i : ids) {
					boolean isFind = false;
					for(DiscontinuedSeries c : list) {
						if (c.getId().contentEquals(i)) {
							c.setMtime(new Date());
							c.setOrder(cnt);
							saveSeries.add(c);
							isFind = true;
							cnt++;
							break;
						}
					}
					if (isFind == false) throw new ModelNotFoundException("sort() ids not Found. id="+i );
				}
				if (saveSeries.size() != list.size()) {
					// idsに含まれないlistのorderを再設定
					for(DiscontinuedSeries c : list) {
						boolean isFind = false;
						for(DiscontinuedSeries s : saveSeries) {
							if (s.getId().equals(c.getId())) {
								isFind = true;
								break;
							}
						}
						if (isFind == false) {
							c.setMtime(new Date());
							c.setOrder(cnt);
							saveSeries.add(c);
							cnt++;
						}
					}
				}
				if (saveSeries.size() > 0) {
					List<DiscontinuedSeries> res = repo.saveAll(saveSeries);
					ret.setCount(res.size());
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
			log.error(e.getMessage() + e.toString());
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		}
		return ret;
	}

	@Override
	public ErrorObject changeStateToProd(String testid, String prodCategoryId, User u) {
		ErrorObject ret = new ErrorObject();
		try {
			DiscontinuedSeries se = get(testid, ret);
			if (ret.isError()) return ret;
			if (se.getState().equals(DiscontinuedModelState.TEST) == false) {
				throw new ModelConditionDifferentException("Series is not TEST.");
			}
			// 取得したIDにPRODが無ければ作成
			List<DiscontinuedSeries> list = repo.findByStateRefId(testid, DiscontinuedModelState.PROD);
			DiscontinuedSeries s = null;
			if (list == null || list.isEmpty() ) {
				s = se.Copy(); // コピー
				s.setId(null);
				Date dt = new Date();
				s.setCtime(dt);
				s.setMtime(dt);
				s.setCategoryId(prodCategoryId);
				s.setStateRefId(testid);
				s.setState(DiscontinuedModelState.PROD);
				// TESTにlangRefIdがあればPRODのlangRefIdを設定。
				if (se.getLangRefId() != null && se.getLangRefId().isEmpty() == false) {
					List<DiscontinuedSeries> prodList = repo.findByStateRefId(se.getLangRefId(), DiscontinuedModelState.PROD);
					if (prodList != null && prodList.isEmpty() == false) {
						s.setLangRefId(prodList.get(0).getId());
					}
				}

				s = repo.save(s);
			} else {
				// PRODが既に作成されていればUpdate
				s = list.get(0);
				String id =  s.getId();
				String langRef = s.getLangRefId();
				s = se.Copy(); // コピー
				s.setId(id);
				s.setCategoryId(prodCategoryId);
				s.setStateRefId(testid);
				s.setState(DiscontinuedModelState.PROD);
				Date dt = new Date();
				s.setMtime(dt);
				// テストがlangRef有りなら、ProdのIdを設定。
				if (se.getLangRefId() != null && se.getLangRefId().isEmpty() == false) {
					s.setLangRefId(langRef);
				}
				s = repo.save(s);
			}
			ret.setCount(1);
			String prodId = s.getId();

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
			log.error(e.getMessage() + e.toString());
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		}
		return ret;
	}

	@Override
	public ErrorObject changeStateToArchive(String id, User u) {
		ErrorObject ret = new ErrorObject();
		try {
			DiscontinuedSeries se = get(id, ret);
			if (ret.isError()) return ret;
			if (se.getState().equals(DiscontinuedModelState.TEST) == false) {
				throw new ModelConditionDifferentException("Series id is not TEST.");
			}
			// 取得したIDを元にArchiveを作成。CategorySeriesもCategory.TESTとSeries.Archiveで新たに作成。
			DiscontinuedSeries s = se.Copy();
			s.setId(null);
			Date dt = new Date();
			s.setMtime(dt);
			s.setStateRefId(id);
			s.setState(DiscontinuedModelState.ARCHIVE);
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
	public ErrorObject changeStateToTest(String id) {
		ErrorObject ret = new ErrorObject();
		try {
			DiscontinuedSeries se = get(id, ret);
			if (ret.isError()) return ret;
			if (se.getState().equals(DiscontinuedModelState.ARCHIVE) == false) {
				throw new ModelConditionDifferentException("Series id is not ARCHIVE.");
			}

			// 取得したArchiveを元にTESTを作成。TESTをUpdate。
			// CategorySeriesは何もしない。（ARCHIVEは削除しないし、Series.TESTのIDも変えないため）
			DiscontinuedSeries testSeries = get(se.getStateRefId(), ret);
			if (ret.isError()) return ret;

			DiscontinuedSeries s = se.Copy();
			s.setId(testSeries.getId());
			Date dt = new Date();
			s.setMtime(dt);
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
			log.error(e.getMessage() + e.toString());
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		}
		return ret;

	}
	@Override
	public ErrorObject checkDelete(String id) {
		ErrorObject ret = new ErrorObject();
		try {
			DiscontinuedSeries s = get(id, ret);
			if (ret.isError()) return ret;

			List<DiscontinuedSeries> list = temp.findByLangRefId(id);
			if (ret.isError()) return ret;
			if (list.size() > 0){
				throw new ModelConditionDifferentException("This series is referenced by other countries.");
			}

			if (s.getState().equals(DiscontinuedModelState.PROD)) {
				// PRODならActiveでなければ削除OK
				if (s.isActive()) {
					throw new ModelConditionDifferentException("This id is Prod and Active.");
				}
			} else {
				list = repo.findByStateRefId(s.getId(), DiscontinuedModelState.PROD);
				if (list != null && list.isEmpty() == false) {
					boolean isActive = list.get(0).isActive();
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
			log.error(e.getMessage() + e.toString());
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		}
		return ret;
	}
	@Override
	public ErrorObject delete(String id) {
		ErrorObject ret = new ErrorObject();
		int cnt = 0;
		try {
			DiscontinuedSeries s = get(id, ret);
			if (ret.isError()) return ret;
			ret = checkDelete(id);
			if (ret.isError()) return ret;

			if (s.getState().equals(DiscontinuedModelState.PROD)) {
				repo.deleteById(id);
				cnt++;
			} else if (s.getState().equals(DiscontinuedModelState.ARCHIVE)) {
				// ARCHIVEなら削除OK
				repo.deleteById(id);
				cnt++;
			} else {
				// TESTならPRODがActiveでなければ削除OK。PRODが無ければ削除OK。
				List<DiscontinuedSeries> list = repo.findByStateRefId(s.getId(), DiscontinuedModelState.PROD);
				if (list != null && list.isEmpty() == false) {
					boolean isActive = list.get(0).isActive();
					if (isActive) {
						throw new ModelConditionDifferentException("Prod is active.");
					}
				}
				{
					repo.deleteById(s.getId());
					cnt++;
				}
			}
			ret.setCount(cnt);
		} catch (ModelConditionDifferentException e) {
			ret.setCode(ErrorCode.E10005);
			ret.setMessage(e.getMessage());
		} catch (MongoException e) {
			ret.setCode(ErrorCode.E50001);
			ret.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
			log.error(e.getMessage() + e.toString());
		}
		return ret;
	}



	// ========= private =========


}

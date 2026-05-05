package com.smc.discontinued.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.mongodb.MongoException;
import com.smc.discontinued.dao.DiscontinuedCategoryRepository;
import com.smc.discontinued.dao.DiscontinuedCategoryTemplateImpl;
import com.smc.discontinued.dao.DiscontinuedSeriesRepository;
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
public class DiscontinuedCategoryServiceImpl implements DiscontinuedCategoryService {

	@Autowired
	DiscontinuedCategoryRepository repo;

	@Autowired
	DiscontinuedCategoryTemplateImpl temp;

	@Autowired
	DiscontinuedSeriesRepository seriesRepo;

	@Autowired
    HttpServletRequest req;

	@Override
	public  List<DiscontinuedCategory> listAllActive(String lang, DiscontinuedModelState state, ErrorObject err) {

		List<DiscontinuedCategory> ret = null;
		try{
			ret = temp.listAll(lang, state, true);
		}catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error("DiscontinuedCategoryServiceImpl.listAllActive()="+e.getMessage() + e.toString());
		}
		return ret;
	}

	@Override
	public  List<DiscontinuedCategory> listAll(String lang, DiscontinuedModelState state, ErrorObject err) {

		List<DiscontinuedCategory> ret = null;
		try{
			ret = temp.listAll(lang, state, null);
		}catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error("DiscontinuedCategoryServiceImpl.listAll()="+e.getMessage() + e.toString());
		}
		return ret;
	}

	@Override
	public List<DiscontinuedCategory> listLangRef(String id, ErrorObject err) {
		return temp.findByLangRefId(id);
	}

	@Override
	public ErrorObject save(DiscontinuedCategory category) {
		ErrorObject ret = new ErrorObject();
		try {
			// 新規の場合
			if (StringUtils.isEmpty(category.getId())) {
				// 同名 slugチェック
				if (isSlugExists(category.getSlug(), category.getLang(), category.getState(), null, ret)) throw new ModelExistsException("Category slug is exists.");
				else if (ret.isError()) return ret;

				// この階層のorderの最大値を取得
				int order = listAll(category.getLang(), category.getState(), ret ).size();
				if (ret.isError()) return ret;
				order++;
				category.setOrder(order);
			} else {
				DiscontinuedCategory c = get(category.getId(), ret);
				if (ret.isError()) return ret;

				if (c.getSlug() != null && c.getSlug().equals(category.getSlug()) == false) {
					if (isSlugExists(category.getSlug(), category.getLang(), category.getState(), null, ret)) throw new ModelExistsException("Category slug is exists.");
					else if (ret.isError()) return ret;
				}
			}

			category.setMtime(new Date()); // Always update mtime

			category = repo.save(category);
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
	public boolean isSlugExists(String slug, String lang, DiscontinuedModelState state, Boolean active,
			ErrorObject err) {
		boolean ret = false;
		try {
			Optional<DiscontinuedCategory> c = temp.findBySlug(slug, lang, state, active);

			if (c != null && c.isPresent() ) {
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
	public DiscontinuedCategory get(String id, ErrorObject err) {
		DiscontinuedCategory ret = null;
		try {
			ret =repo.findById(id).orElseThrow(() -> new ModelNotFoundException("DiscontinuedCategory.id=" + id));
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
	public DiscontinuedCategory getSlug(String slug, String lang, DiscontinuedModelState state, ErrorObject err) {
		DiscontinuedCategory ret = null;
		try {
			Optional<DiscontinuedCategory> c = repo.findBySlug(slug, lang, state, true );
			if (c != null && c.isPresent() ) {
				ret = c.get();
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
	public DiscontinuedCategory getOldId(String oldId, String lang, DiscontinuedModelState state, ErrorObject err) {
		DiscontinuedCategory ret = null;
		try {
			Optional<DiscontinuedCategory> c = repo.findByOldId(oldId, lang, state, true );
			if (c != null && c.isPresent() ) {
				ret = c.get();
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
	public DiscontinuedCategory getStateRefId(DiscontinuedCategory c,  DiscontinuedModelState state, ErrorObject err) {
		DiscontinuedCategory ret = null;
		try {
			if (state.equals(DiscontinuedModelState.TEST)) {
				ret =repo.findById(c.getStateRefId()).orElseThrow(() -> new ModelNotFoundException("Category.id=" + c.getStateRefId()));
			} else {
				ret =repo.findByStateRefId(c.getId()).orElseThrow(() -> new ModelNotFoundException("Category.id=" + c.getId()));
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
	public DiscontinuedCategory getLangRefId(DiscontinuedCategory c, String lang, ErrorObject err) {
		DiscontinuedCategory ret = null;
		try {
			if (c.getLang().equals(lang)) {
				ret =repo.findById(c.getId()).orElseThrow(() -> new ModelNotFoundException("Category.id=" + c.getStateRefId()));
			} else {
				List<DiscontinuedCategory> list = null;
				if (StringUtils.isEmpty(c.getLangRefId()) == false) {
					list =repo.findByLangRefId(c.getLangRefId());
					Optional<DiscontinuedCategory> tmp = repo.findById(c.getLangRefId());
					if (tmp != null) list.add(tmp.get());
				} else {
					list =repo.findByLangRefId(c.getId());
				}
				for(DiscontinuedCategory ca : list) {
					if (ca.getLang().equals(lang)) {
						ret = ca;
						break;
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
	public ErrorObject changeStateToProd(String id, User u) {
		ErrorObject ret = new ErrorObject();
		try {
			DiscontinuedCategory c = get(id, ret);
			if (ret.isError()) return ret;
			if (c.getState().equals(DiscontinuedModelState.TEST) == false) {
				throw new ModelConditionDifferentException("Series is not TEST.");
			}
			// 取得したIDにPRODが無ければ作成
			Optional<DiscontinuedCategory> oPC = repo.findByStateRefId(id, DiscontinuedModelState.PROD);
			DiscontinuedCategory pC = null;
			if (oPC == null || oPC.isPresent() == false) {
				pC = c.Copy();
				pC.setId(null);
				Date dt = new Date();
				pC.setCtime(dt);
				pC.setMtime(dt);
				pC.setStateRefId(id);
				pC.setState(DiscontinuedModelState.PROD);
				pC = repo.save(pC);
			} else {
				// PRODが既に作成されていればUpdate
				pC = oPC.get().Copy();
				pC.setId(oPC.get().getId());
				Date dt = new Date();
				pC.setMtime(dt);
				pC.setSlug(c.getSlug());
				pC.setStateRefId(id);
				pC.setState(DiscontinuedModelState.PROD);
				pC = repo.save(pC);
			}
			ret.setCount(1);
			String prodId = pC.getId();


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
	public ErrorObject sort(String lang, DiscontinuedModelState state, List<String> ids) {
		ErrorObject ret = new ErrorObject();
		try {
			if (lang.isEmpty() || ids == null || ids.isEmpty()) {
				 throw new ModelNotFoundException("sort() id or ids is empty." );
			}

			List<DiscontinuedCategory> list = repo.listAll(lang, state, null);
			List<DiscontinuedCategory> saveList = new ArrayList<DiscontinuedCategory>();
			int order = 1;
			for(String i : ids) {
				boolean isFind = false;
				for(DiscontinuedCategory c : list) {
					if (c.getId().contentEquals(i)) {
						c.setOrder(order);
						c.setMtime(new Date());
						saveList.add(c);
						order++;
						isFind = true;
						break;
					}
				}
				if (isFind == false) throw new ModelNotFoundException("sort() ids not Found. id="+i );
			}
			if (saveList.size() != list.size()) {
				// idsに含まれないlistのorderを再設定
				for(DiscontinuedCategory c : list) {
					boolean isFind = false;
					for(DiscontinuedCategory s : saveList) {
						if (s.getId().equals(c.getId())) {
							isFind = true;
							break;
						}
					}
					if (isFind == false) {
						c.setOrder(order);
						c.setMtime(new Date());
						saveList.add(c);
						order++;
					}
				}
			}
			if (saveList.size() > 0) {
				List<DiscontinuedCategory> res = repo.saveAll(saveList);
				ret.setCount(res.size());

			}
		} catch (ModelNotFoundException e) {
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
	public ErrorObject sortSeries(String categoryid, List<String> ids) {
		ErrorObject ret = new ErrorObject();
		try {
			if (categoryid == null || categoryid.isEmpty() || ids == null || ids.isEmpty()) {
				 throw new ModelNotFoundException("sort() id or ids is empty." );
			}
			DiscontinuedCategory ca = get(categoryid, ret);
			if (ret.isError()) return ret;

			List<DiscontinuedSeries> list = seriesRepo.findAllByCategoryId(categoryid);
			List<DiscontinuedSeries> saveList = new ArrayList<DiscontinuedSeries>();
			for(String i : ids) {
				boolean isFind = false;
				for(DiscontinuedSeries s : list) {
					if (s.getId().equals(i)) {
						saveList.add(s);
						isFind = true;
						break;
					}
				}
				if (isFind == false) throw new ModelNotFoundException("sort() ids not Found. id="+i );
			}
			if (saveList.size() != list.size()) {
				// idsに含まれないlistのorderを再設定
				for(DiscontinuedSeries c : list) {
					boolean isFind = false;
					for(DiscontinuedSeries s : saveList) {
						if (s.getId().equals(c.getId())) {
							isFind = true;
							break;
						}
					}
					if (isFind == false) {
						saveList.add(c);
					}
				}
			}
			if (saveList.size() > 0) {
				List<DiscontinuedSeries> res = seriesRepo.saveAll(saveList);
				ret.setCount(res.size());
			}
		} catch (ModelNotFoundException e) {
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

	public ErrorObject checkDelete(String id) {
		ErrorObject ret = new ErrorObject();
		try {
			DiscontinuedCategory c = get(id, ret);
			if (ret.isError()) return ret;

			List<DiscontinuedCategory> list = listLangRef(id, ret);
			if (ret.isError()) return ret;
			if (list.size() > 0){
				throw new ModelConditionDifferentException("This category is referenced by other countries.");
			}

			if (c.getState().equals(DiscontinuedModelState.PROD)) {
				// PRODならActiveでなければ削除OK
				if (c.isActive()) {
					throw new ModelConditionDifferentException("id is Prod. Active Prod is not Delete.");
				}
			} else {
				// TESTならPRODがActiveでなければ削除OK。PRODが無ければ削除OK。
				DiscontinuedCategory prod = null;
				Optional<DiscontinuedCategory> oProd = repo.findByStateRefId(c.getId());
				if (oProd.isPresent()) {
					prod = oProd.get();
					if (prod.isActive()) {
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

	public ErrorObject delete(String id) {
		ErrorObject ret = new ErrorObject();
		try {
			DiscontinuedCategory c = get(id, ret);
			if (ret.isError()) return ret;

			ret = checkDelete(id);
			if (ret.isError()) return ret;

			if (c.getState().equals(DiscontinuedModelState.PROD)) {
				// PRODはPRODのみ削除。
				repo.deleteById(id);
				setOrder(c);
			} else {
				DiscontinuedCategory prod = null;
				Optional<DiscontinuedCategory> oProd = repo.findByStateRefId(c.getId());
				if (oProd.isPresent()) {
					prod = oProd.get();
					if (prod != null) {
						repo.deleteById(prod.getId());
						setOrder(prod);
					}
				}
				repo.deleteById(c.getId());

				setOrder(c);

				ret.setCount(1);
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
	// ========= private =========
	// orderを振り直し
	private boolean setOrder(DiscontinuedCategory cate ) {
		boolean ret = false;

		ErrorObject obj = new ErrorObject();
		if (!obj.isError()) {
			List<DiscontinuedCategory> list = repo.listAll(cate.getLang(), cate.getState(), null);

			int order = 1;
			Date dt = new Date();
			for(DiscontinuedCategory c : list) {
				c.setOrder(order);
				c.setMtime(dt);
				list.add(c);
				order++;
			}
			if (list.isEmpty() == false) {
				repo.saveAll(list);
				ret = true;
			}
		}
		return ret;
	}






}

package com.smc.webcatalog.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.mongodb.MongoException;
import com.smc.exception.ModelConditionDifferentException;
import com.smc.exception.ModelExistsException;
import com.smc.exception.ModelNotFoundException;
import com.smc.psitem.dao.PsItemRepository;
import com.smc.psitem.model.PsItem;
import com.smc.webcatalog.config.ErrorCode;
import com.smc.webcatalog.dao.CategoryRepository;
import com.smc.webcatalog.dao.CategorySeriesRepository;
import com.smc.webcatalog.dao.CategorySeriesTemplateImpl;
import com.smc.webcatalog.dao.CategoryTemplateImpl;
import com.smc.webcatalog.dao.SeriesFaqRepository;
import com.smc.webcatalog.dao.SeriesLinkRepository;
import com.smc.webcatalog.dao.SeriesLinkTemplateImpl;
import com.smc.webcatalog.dao.SeriesRepository;
import com.smc.webcatalog.dao.SeriesTemplateImpl;
import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.CategorySeries;
import com.smc.webcatalog.model.CategoryType;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Series;
import com.smc.webcatalog.model.SeriesFaq;
import com.smc.webcatalog.model.SeriesLink;
import com.smc.webcatalog.model.TemplateCategory;
import com.smc.webcatalog.model.User;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

	@Autowired
	CategoryRepository repo;

	@Autowired
	CategoryTemplateImpl temp;

	@Autowired
	CategorySeriesRepository csRepo;

	@Autowired
	CategorySeriesTemplateImpl csTemp;

	@Autowired
	TemplateCategoryService tcService;

	@Autowired
	SeriesRepository seriesRepo;

	@Autowired
	SeriesTemplateImpl seriesTemp;

	@Autowired
	SeriesLinkTemplateImpl sLinkTemp;

	@Autowired
	SeriesLinkRepository sLinkRepo;
	
	@Autowired
	PsItemRepository psItemRepo;

	@Autowired
	SeriesFaqRepository faqRepo;
	
	@Autowired
	NarrowDownService narrowDownService;

    @Autowired
	Environment env;

	@Autowired
    HttpServletRequest req;

	@Override
	public ErrorObject save(Category category) {

		ErrorObject ret = new ErrorObject();
		try {
			Category parent = null;
			Optional<Category> oP = repo.findById(category.getParentId());
			if(oP.isPresent()) parent =  oP.get();
			// 新規の場合
			if (category.getId() == null || category.getId().isEmpty()) {
				// 同名 slugチェック
				if (isNameExists(category, ret)) throw new ModelExistsException("Category name is exists.");
				else if (ret.isError()) return ret;

				if (parent != null) {
					if (isSlugExists(category, ret)) throw new ModelExistsException("Category slug is exists.");
					else if (ret.isError()) return ret;
				} else {
					if (isSlugExists(category, ret)) throw new ModelExistsException("Category slug is exists.");
					else if (ret.isError()) return ret;
				}
				// この階層のorderの最大値を取得
				int order = getWithChildren(category.getParentId(), null, ret ).getChildren().size();
				if (ret.isError()) return ret;
				order++;
				category.setOrder(order);
				category.setId(null);
			} else {
				Category c = get(category.getId(), ret);
				if (ret.isError()) return ret;

				if (c.getName().equals(category.getName()) == false) {
					if (isNameExists(category, ret)) throw new ModelExistsException("Category name is exists.");
					else if (ret.isError()) return ret;
				}
				if (c.getSlug() != null && c.getSlug().equals(category.getSlug()) == false) {
					if (parent != null) {
						if (isSlugExists(category, ret)) throw new ModelExistsException("Category slug is exists.");
						else if (ret.isError()) return ret;
					} else {
						if (isSlugExists(category, ret)) throw new ModelExistsException("Category slug is exists.");
						else if (ret.isError()) return ret;
					}
				}
			}

			category.setMtime(new Date()); // Always update mtime

			setContext(category.getLang(), category.getState(), category.getType() );

			category = repo.save(category);
			ret.setCount(1);

		} catch (ModelExistsException e) {
			ret.setCode(ErrorCode.E10001);
			ret.setMessage(e.getMessage());
		} catch (MongoException e) {
			ret.setCode(ErrorCode.E50001);
			ret.setMessage(e.getMessage());
			log.error("save()"+e.toString() + e.getMessage() );

		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
			log.error("save()"+e.toString() + e.getMessage() );
		}
		return ret;
	}

	@Override
	public boolean isSlugExists(Category category, ErrorObject err) {
		boolean ret = false;
		boolean isUpdate = ( category.getId() != null && category.getId().isEmpty() == false);
		try {
			Category p = getWithChildren(category.getParentId(), null, err );
			List<Category> child = p.getChildren();
			for(Category c : child) {
				if (c.getSlug().equals(category.getSlug())) {
					if (isUpdate && c.getId().equals(category.getId()) ) continue;
					ret = true;
					break;
				}
			}
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
			log.error("isSlugExists()"+e.toString() + e.getMessage() );
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error("isSlugExists()"+e.toString() + e.getMessage() );
		}
		return ret;
	}


	@Override
	public boolean isNameExists(Category category, ErrorObject err) {
		boolean ret = false;
		boolean isUpdate = !( category.getId() == null || category.getId().isEmpty());
		try {
			Category p = getWithChildren(category.getParentId(), null, err );
			List<Category> child = p.getChildren();
			for(Category c : child) {
				if (c.getName().equals(category.getName())) {
					if (isUpdate && c.getId().equals(category.getId()) ) continue;
					ret = true;
					break;
				}
			}
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
			log.error("isNameExists()"+e.toString() + e.getMessage() );
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error("isNameExists()"+e.toString() + e.getMessage() );
		}
		return ret;
	}

	@Override
	public Category get(String id, ErrorObject err) {
		Category ret = null;
		try {
			ret =repo.findById(id).orElseThrow(() -> new ModelNotFoundException("Category.id=" + id));
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
	public Category getRoot(String lang, ModelState state, CategoryType type, ErrorObject err) {
		Category ret = null;
		try {
			ret = temp.findRoot(lang, state, type);
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
	public Category getLang(String lang, ModelState state, CategoryType type, Boolean active, ErrorObject err) {
		Category ret = null;
		try {
			Category r = temp.findRoot(lang, state, type);
			if (r != null) {
				List<Category> list = temp.findChild(r.getId(), r.getState(), r.getType(), null);
				if (list.size() > 0) {
					for (Category c : list) {
						if (c.getParentId() != null && c.getParentId().equals(r.getId())) {
							if (active == null || c.isActive() == active) {
								ret = c;
								break;
							}
						}
					}
				}
			}

		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
			log.error("getLang()"+e.toString() + e.getMessage() );
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error("getLang()"+e.toString() + e.getMessage() );
		}
		return ret;
	}

	@Override
	public Category getStateRefId(Category c,  ModelState state, ErrorObject err) {
		Category ret = null;
		try {
			if (state.equals(ModelState.TEST)) {
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
	public Category getLangRefId(Category c, String lang, ErrorObject err) {
		Category ret = null;
		try {
			if (c.getLang().equals(lang)) {
				ret =repo.findById(c.getId()).orElseThrow(() -> new ModelNotFoundException("Category.id=" + c.getStateRefId()));
			} else {
				List<Category> list = null;
				if (c.getLangRefId() != null && c.getLangRefId().isEmpty() == false) {
					list =repo.findByLangRefId(c.getLangRefId());
					Optional<Category> tmp = repo.findById(c.getLangRefId());
					if (tmp != null) list.add(tmp.get());
				} else {
					list =repo.findByLangRefId(c.getId());
				}
				for(Category ca : list) {
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
	public Category getWithSeries(String id, Boolean active, ErrorObject err) {
		Category ret = null;
		try {
			ret = repo.findById(id).orElseThrow(() -> new ModelNotFoundException("Category.id=" + id));
			if (active != null && active.equals(ret.isActive()) == false) {
				throw new ModelNotFoundException("active is not same. In active="+active+" DB active ="+ret.isActive());
			}
			// seriesListを取得
			Optional<CategorySeries> oCs = csRepo.findByCategoryId(ret.getId());
			if(oCs.isPresent()) {
				List<Series> dispList = new ArrayList<Series>();
				List<Series> csList = oCs.get().getSeriesList();
				for(Series s : csList) {
					if (s != null) {
						if (active == null) dispList.add(s);
						else if (s.isActive() == active) dispList.add(s);
					}
				}
				ret.setSeriesList(dispList);
			}
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
			log.error("getWithSeries()"+e.toString() + e.getMessage() );
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error("getWithSeries()"+e.toString() + e.getMessage() );
		}
		return ret;
	}



	@Override
	public Category getFromSlug(String slug ,String lang, ModelState state, CategoryType type, int level, Boolean active,  ErrorObject err) {
		Category ret = null;
		try {
			if (level < 1) level = 1;
			else if (level > 1) level = 2; // 2階層固定

			Category c1 = null;
			Category c2 = null;

			Category r = temp.findRoot(lang, state, type);
			List<Category> list = temp.findBySlug(slug, lang, state, type, active);
			if (list != null && list.size() > 0) {
				for(Category c : list) {
					if (c.getParentId().equals(r.getId())) {
						c1 = c;
					} else {
						c2 = c;
					}
				}
			}
			if (level == 1) {
				ret = c1;
			} else {
				ret = c2;
			}
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
			log.error("getFromSlug()"+e.toString() + e.getMessage() );
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error("getFromSlug()"+e.toString() + e.getMessage() );
		}
		return ret;
	};

	@Override
	public Category getFromSlugSecond(String slug , String parentId, String lang, ModelState state, CategoryType type, Boolean active,  ErrorObject err) {
		Category ret = null;
		try {

			List<Category> list = temp.findBySlug(slug, lang, state, type, active);
			if (list != null && list.size() > 0) {
				for(Category c : list) {
					if (c.getParentId().equals(parentId)) {
						ret = c;
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
	};
	@Override
	public Category getFromOldId(String id, ModelState state, CategoryType type, Boolean active, ErrorObject err) {
		Category ret = null;
		try {
			Optional<Category> oC = repo.findByOldIdAndStateAndType(id, state, type);
			if (oC.isPresent()) {
				ret = oC.get();
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
	public Category getWithChildren(String id, Boolean active, ErrorObject err) {
		Category ret = null;
		try {
			if (id == null || id.isEmpty()) {
				err.setCode(ErrorCode.E10005);
			} else {
				ret = get(id, err);
			}

			if (active != null && ret != null && active.equals(ret.isActive()) == false) {
				throw new ModelNotFoundException("active is not same. In active="+active+" DB active ="+ret.isActive());
			}

			//set children
			ret.setChildren(repo.findByParentId(ret.getId(), ret.getState(), ret.getType(), active));

		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
			log.error("getWithChildren()"+e.toString() + e.getMessage() );
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error("getWithChildren()"+e.toString() + e.getMessage() );
		}
		return ret;
	}

	@Override
	public  List<Category> getParents(String id, Boolean active, ErrorObject err) {
		List<Category> ret = null;
		try {
			boolean isNext = true;
			String findId = id;
			while(isNext) {
				Category c = get(findId, err);
				if (c != null) {
					if (ret == null) ret = new ArrayList<Category>();
					ret.add(0, c);
					if (active != null && c.isActive() != active) {
						isNext = false;
						err.setCode(ErrorCode.E10005);
					}
					if (c.getParentId().isEmpty() == false) {
						findId = c.getParentId();
					}
					else {
						isNext = false;
					}
				}
				else {
					isNext = false;
				}
			}
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
			log.error("getParents()"+e.toString() + e.getMessage() );
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error("getParents()"+e.toString() + e.getMessage() );
		}
		return ret;
	}

	@Override
	public ErrorObject move(String srcId, String dstId, boolean append) {

		ErrorObject ret = new ErrorObject();
		try {

			if (srcId.isEmpty() == false && dstId.isEmpty() == false && srcId.equals(dstId) == false) {
				Category src = get(srcId, ret);
				if (ret.isError())  return ret;
				Category dst = get(dstId, ret);
				if (ret.isError())  return ret;
				String sPId = src.getParentId();
				String dPId = dst.getParentId();

				List<Category> list = new ArrayList<Category>();
				if (dPId.equals(sPId) == false) {
					String pId = src.getParentId();
					if (pId.isEmpty()) {
						throw new ModelNotFoundException("Category.srcId=" + srcId + "dstId=" + dstId);
					}
					else {
						// 元のOrderを振り直す
						Category parent = getWithChildren(pId, null, ret );
						if (ret.isError())  return ret;
						List<Category> child = parent.getChildren();
						int order = 1;
						for(Category c : child) {
							if (c.getId().equals(src.getId())) continue;
							c.setOrder(order);
							c.setMtime(new Date());
							list.add(c);
							order++;
						}
					}
					src.setParentId(dst.getParentId());
				}
				if (list.size() > 0) {
					repo.saveAll(list);
					list = new ArrayList<Category>();
				}

				Category parent = getWithChildren(dst.getParentId(), null, ret );
				if (ret.isError())  return ret;
				List<Category> child = parent.getChildren();
				int order = 1;
				for(Category c : child) {
					if (c.getId().equals(dst.getId())) {
						if (append) {
							c.setOrder(order);
							c.setMtime(new Date());
							list.add(c);
							order++;

							src.setParentId(dst.getParentId());
							src.setOrder(order);
							src.setMtime(new Date());
							list.add(src);
							order++;
						}
						else {
							src.setParentId(dst.getParentId());
							src.setOrder(order);
							src.setMtime(new Date());
							list.add(src);
							order++;

							c.setOrder(order);
							c.setMtime(new Date());
							list.add(c);
							order++;
						}
					}
					else {
						c.setOrder(order);
						c.setMtime(new Date());
						list.add(c);
						order++;
					}
				}
				if (list.size() > 0) {
					List<Category> res = repo.saveAll(list);
					ret.setCount(res.size());
					setContext(src.getLang(), src.getState(), src.getType() );
				}
			}
		} catch (ModelNotFoundException e) {
			ret.setCode(ErrorCode.E10001);
			ret.setMessage(e.getMessage());
		} catch (MongoException e) {
			ret.setCode(ErrorCode.E50001);
			ret.setMessage(e.getMessage());
			log.error("move()"+e.toString() + e.getMessage() );
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
			log.error("move()"+e.toString() + e.getMessage() );
		}
		return ret;
	}


	@Override
	public ErrorObject changeStateToProd(String id) {
		ErrorObject ret = new ErrorObject();
		// 配下を全部アップの仕様確認。未テストのためコメントアウト。2020/2/18
/*		try {
			Category ca = get(id, ret);
			if (ret.isError()) return ret;
			if (ca.getState().equals(ModelState.TEST) == false) {
				throw new ModelConditionDifferentException("Category is Prod.");
			}
			// 子供が居る場合はエラー
			ErrorObject err = new ErrorObject();
			Category chi = getWithChildren(ca.getId(), null, err);
			if (err.isError()) return err;
			else if (chi.getChildren().size() > 0) {
				throw new ModelConditionDifferentException("Category have children.");
			}

			// TESTの前後を取得
			Category pa = getWithChildren(ca.getParentId(), null, err);
			if (err.isError()) return err;

			Category pre = null;
			Category aft = null;
			List<Category> child = pa.getChildren();
			int cnt = 0;
			for (Category c : child) {
				if (c.getId().equals(pa.getId())) {
					if (cnt > 0) pre = child.get(cnt--);
					if (cnt < child.size()) aft = child.get(cnt++);
					break;
				}
				cnt++;
			}

			// 取得したIDにPRODがあるか。
			Optional<Category> oProd = temp.findByStateRefId(ca.getId(), ModelState.PROD, ca.getType());
			if (oProd.isPresent()) {
				Category prod = oProd.get();
				// あれば順番が変わってないか。
				if (ca.getOrder() != prod.getOrder()) throw new ModelConditionDifferentException("Original and Prod different order.");
				// 削除
				err = delete(prod.getId());
				if (err.isError()) return err;
			}
			// 前後の順番が変わってないか。
			if (pre != null) {
				Optional<Category> prodP = temp.findByStateRefId(pre.getId(), ModelState.PROD, pre.getType());
				if (prodP.isPresent()) {
					Category p = prodP.get();
					if (p.getOrder() != pre.getOrder()) {
						throw new ModelConditionDifferentException("pre Category different order.");
					}
				} else {
					throw new ModelConditionDifferentException("pre Category different order.");
				}
			}

			if (aft != null) {
				Optional<Category> prodA = temp.findByStateRefId(aft.getId(), ModelState.PROD, aft.getType());
				if (prodA.isPresent()) {
					Category p = prodA.get();
					if (p.getOrder() != aft.getOrder()) {
						throw new ModelConditionDifferentException("after Category different order.");
					}
				} else {
					throw new ModelConditionDifferentException("after Category different order.");
				}
			}

			// TESTの親を見つけて、そのIDのPRODのRefIdを見つける。
			Category testParent = get(ca.getParentId(), ret);
			if (ret.isError()) return ret;
			Optional<Category> prodParent = temp.findByStateRefId(testParent.getId(), ModelState.PROD, testParent.getType());
			if (prodParent.isPresent() == false) throw new ModelNotFoundException("Parent Category is not exists.");

			String prodParentId = prodParent.get().getId();
			ca.setId(null);
			ca.setParentId(prodParentId);
			ca.setStateRefId(id);
			ca.setState(ModelState.PROD);
			Date dt = new Date();
			ca.setMtime(dt);

			boolean exists = isNameExists(ca, ret);
			if (ret.isError()) return ret;
			if (exists)  throw new ModelExistsException("Category name is exists.");
			ca = repo.save(ca);
			ret.setCount(1);

			// SeriesのPRODがある場合はPRODへのCategorySeriesを作成
			Optional<CategorySeries> oCs = csRepo.findByCategoryId(ca.getStateRefId()); // TESTを取得
			if (oCs.isPresent())
			{
				// CategorySeriesのPRODを作成し保存
				List<Series> prodList = new ArrayList<Series>();
				CategorySeries cs = oCs.get();
				List<Series> seriesList = cs.getSeriesList();
				for(Series se : seriesList)
				{
					Optional<Series> prodS = seriesTemp.findByStateRefId(se.getStateRefId(), ModelState.PROD);
					if (prodS.isPresent()) {
						prodList.add(prodS.get());
					}
				}
				if (prodList.size() > 0) {
					cs.setCategoryId(ca.getId());
					cs.setSeriesList(prodList);
					csRepo.save(cs);
				}
			}

			setContext(ca.getLang(), ca.getState(), ca.getType() );
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
		}*/
		return ret;
	}

	@Override
	public ErrorObject changeStateToProdAll(String id, User u) {

		ErrorObject ret = new ErrorObject();
		try {

			Category inputCategory = get(id, ret);
			CategoryType _type = inputCategory.getType();
			if (ret.isError()) return ret;
			if (inputCategory.getState().equals(ModelState.TEST) == false) {
				throw new ModelConditionDifferentException("Category is Prod. TEST only.");
			}

			TemplateCategory testTc = tcService.findByCategoryIdFromBean(inputCategory.getLang(), inputCategory.getState(), id);
			if (_type.equals(CategoryType.CATALOG)) {
				if (testTc == null) {
					// parentIdを確認
					testTc = tcService.findByCategoryIdFromBean(inputCategory.getLang(), inputCategory.getState(), inputCategory.getParentId());
					if (testTc == null) {
						throw new ModelConditionDifferentException("Category's Template is Empty.");
					} else {
						ret = new ErrorObject(); // ２階層目のカテゴリ。エラーは消しておく
					}
				}
			} else {
				ret = new ErrorObject(); // CategoryType.OTHERはテンプレート不要。エラーは消しておく
			}

			// TESTの親を見つけて、そのIDのPRODのRefIdを見つける。親のPRODが無ければエラー。
			Category testParent = get(inputCategory.getParentId(), ret);
			if (ret.isError()) return ret;
			Optional<Category> prodParent = temp.findByStateRefId(testParent.getId(), ModelState.PROD, testParent.getType());
			if (prodParent.isPresent() == false) throw new ModelNotFoundException("Parent Category is not exists.");

			String baseParentId = inputCategory.getParentId();
			String prodParentId = prodParent.get().getId();
			String testId = inputCategory.getId();

			// TESTを元にPRODを作成。lang,langRefなどはそのまま。既にPRODがあればUpdate
			Optional<Category> prodCategory = temp.findByStateRefId(inputCategory.getId(), ModelState.PROD, inputCategory.getType());
			Category up = null;
			boolean isTemplateUpdate = false;
			if (prodCategory.isPresent())
			{
				// 本番更新
				up = prodCategory.get();
				// slugが変わっていたらディレクトリを移動
				if (_type.equals(CategoryType.CATALOG)) {
					if (inputCategory.getSlug().equals(up.getSlug()) == false) {
						moveDir(up.getSlug(), inputCategory.getSlug(), up.getLang());
					}
				}
				up.SetUpdateParam(inputCategory, u);
				if (_type.equals(CategoryType.CATALOG)) isTemplateUpdate = true;
			} else {
				// ディレクトリを作成
				if (_type.equals(CategoryType.CATALOG)) mkDir(inputCategory.getSlug(), inputCategory.getLang());

				// 本番作成
				up = inputCategory;
				up.setId(null);
				up.setParentId(prodParentId);
				up.setStateRefId(id);
				up.setState(ModelState.PROD);
				if (inputCategory.getLangRefId() != null && inputCategory.getLangRefId().isEmpty() == false) {
					Optional<Category> oLangRefProd = temp.findByStateRefId(inputCategory.getLangRefId(), ModelState.PROD, inputCategory.getType());
					if (oLangRefProd.isPresent()) {
						up.setLangRefId(oLangRefProd.get().getId());
					}
				}
				Date dt = new Date();
				up.setMtime(dt);
			}

			/*boolean exists = isNameExists(up, ret);
			if (ret.isError()) return ret;
			if (exists)  throw new ModelExistsException("Category name is exists.");*/
			up = repo.save(up);

			String prodId = up.getId();
			// デザインリニューアルに伴い、テンプレートのupdateは一緒にはしない。
/*			if (_type.equals(CategoryType.CATALOG)) {
				// TESTのtemplateCategoryを親にコピー。既にPRODがあればUpdate
				if (isTemplateUpdate) {
					TemplateCategory tc = tcService.getCategory(up.getId(), ret);
					if (tc != null) {
						tc.SetUpdateParam(testTc, u);
						tcService.save(tc);
					} else {
						isTemplateUpdate = false;
						ret = new ErrorObject();
					}
				} else  {
					TemplateCategory tc = new TemplateCategory();
					tc.setCategoryId(prodId);
					tc.SetUpdateParam(testTc, u);
					tcService.save(tc);
				}
			}*/
			
			Category rootCategory = temp.findRoot(inputCategory.getLang(), ModelState.TEST, inputCategory.getType());
			if (rootCategory.getId().equals(inputCategory.getParentId()) == false) 
			{
				// 2階層目の本番アップならここでnarrowDownの本番アップ
				if (inputCategory.isNarrowdown()) {
					narrowDownService.changeStateColumnValue(testId, prodId);
				} else {
					narrowDownService.deleteCategoryValue(prodId);
					narrowDownService.deleteCategoryColumn(prodId);
				}
				// narrow_down_compareは削除。2025/11
				// narrowDownService.changeStateCompare(testId, prodId);
			}

			List<Category> list = new ArrayList<Category>();

			List<Category> child = listAll(testId, true, ret); // TESTの配下を取得
			if (ret.isError()) return ret;
			List<Category> prodChild = listAll(prodId, true, ret); //PROD
			if (ret.isError()) return ret;

			// PRODがすでにあればUpdate
			for(Category c : child) {
				Category prod = getStateRefId(c.getId(), prodChild);
				if (prod != null) {
					// slugが変わっていたらディレクトリを移動
					if (_type.equals(CategoryType.CATALOG)) {
						if (c.getSlug() != null && c.getSlug().equals(prod.getSlug()) == false) {
							moveDir(prod.getSlug(), c.getSlug(), prod.getLang());
						}
					}
					prod.SetUpdateParam(c, u);
					if (c.getLangRefId() != null && c.getLangRefId().isEmpty() == false) {
						Optional<Category> oC = temp.findByStateRefId(c.getLangRefId(), ModelState.PROD, c.getType());
						if (oC.isPresent()) {
							prod.setLangRefId(oC.get().getId());
						}
					}
					list.add(prod);
					// NarrowDownの更新
					if (c.isNarrowdown()) {
						narrowDownService.changeStateColumnValue(c.getId(), prod.getId());
						narrowDownService.changeStateCompare(c.getId(), prod.getId());
					} else {
						narrowDownService.deleteCategoryValue(prodId);
						narrowDownService.deleteCategoryColumn(prodId);
					}
				} else {
					c.setParentId(prodId);
					c.setStateRefId(c.getId());
					c.setState(ModelState.PROD);
					if (c.getLangRefId() != null && c.getLangRefId().isEmpty() == false) {
						Optional<Category> oC = temp.findByStateRefId(c.getLangRefId(), ModelState.PROD, c.getType());
						if (oC.isPresent()) {
							c.setLangRefId(oC.get().getId());
						}
					}
					c.setId(null);
					Date dt = new Date();
					c.setMtime(dt);
					list.add(c);
				}
			}

			List<Category> r = null;
			if (list.size() > 0) {
				r = repo.saveAll(list);
				// 新規。NarrowDownの作成
				for(Category c : r) {
					if (c.isNarrowdown()) {
						narrowDownService.changeStateColumnValue(c.getStateRefId(), c.getId());
						narrowDownService.changeStateCompare(c.getStateRefId(), c.getId());
					} else {
						narrowDownService.deleteCategoryValue( c.getId());
						narrowDownService.deleteCategoryColumn( c.getId());
					}
				}
				if (r != null) r.add(up);
			}
			if (r == null) {
				r = new ArrayList<Category>();
				r.add(up);
			}
			if (r != null) {

				ret.setCount(r.size());

				for(Category c : r) {
					// SeriesのPRODがある場合はPRODへのCategorySeriesを作成
					Iterable<CategorySeries> oCs = csRepo.findAllByCategoryId(c.getStateRefId()); // TESTのCategorySeriesを取得
					Iterator<CategorySeries> iter = oCs.iterator();
					while (iter.hasNext()) {
						CategorySeries cs = iter.next();
						// PRODのSeriesが無ければ作成し、CategorySeriesのPRODを作成し保存
						List<Series> prodList = new ArrayList<Series>();
						List<Series> seriesList = cs.getSeriesList();
						for(Series se : seriesList)
						{
							List<Series> prodS = seriesTemp.findByStateRefId(se.getId(), ModelState.PROD);
							if (prodS != null && prodS.size() > 0) {
								Series s = prodS.get(0);
								s.setUpdateParam(se);
								Series saved = seriesRepo.save(s);
								prodList.add(saved);
								
								// SeriesFaqのチェック。
								Optional<SeriesFaq> opFaq = faqRepo.findBySeriesId(s.getId());
								if (opFaq.isPresent()) {
									// TESTからfaqを取得して更新
									SeriesFaq faq = opFaq.get();
									Optional<SeriesFaq> otFaq = faqRepo.findBySeriesId(s.getStateRefId());
									if (otFaq.isPresent()) {
										faq.setSeriesId(saved.getId());
										faq.setFaq(otFaq.get().getFaq());
										faq.setMtime(new Date());
										faqRepo.save(faq);
									}
								} else {
									try {
										// SeriesFaqのPRODを作成
										Optional<SeriesFaq> otFaq = faqRepo.findBySeriesId(s.getStateRefId());
										if (otFaq.isPresent()) {
											SeriesFaq faq = new SeriesFaq();
											faq.setUpdateParam(otFaq.get());
											faq.setSeriesId(saved.getId());
											faq.setMtime(new Date());
											faq.setUser(u);
											faq.setState(ModelState.PROD);
											faq.setStateRefId(otFaq.get().getId());
											faq.setActive(true);
											faqRepo.save(faq);
										}
									}catch (Exception e) {
										// SeriesFaqが複数出来てしまっている場合。
										List<SeriesFaq> listFaq = faqRepo.findAllBySeriesId(s.getStateRefId());
										boolean isProd = false;
										for(SeriesFaq f : listFaq) {
											if (f.getState().equals(ModelState.PROD)) {
												faqRepo.delete(f);
												isProd = true;
											}
										}
										if (isProd == false) {
											// PRODがなければ１つ残す。
											int cnt = 0;
											for(SeriesFaq f : listFaq) {
												if (cnt > 0) {
													faqRepo.delete(f);
												}
												cnt++;
											}
										}
										// SeriesFaqのPRODを作成
										Optional<SeriesFaq> otFaq = faqRepo.findBySeriesId(s.getStateRefId());
										if (otFaq.isPresent()) {
											SeriesFaq faq = new SeriesFaq();
											faq.setUpdateParam(otFaq.get());
											faq.setId(null);
											faq.setSeriesId(saved.getId());
											faq.setMtime(new Date());
											faq.setUser(u);
											faq.setState(ModelState.PROD);
											faq.setStateRefId(otFaq.get().getId());
											faq.setActive(true);
											faqRepo.save(faq);
										}
									}
								}

							} else {
								// PRODのSeriesを作成
								Series prod = new Series();
								prod.setUpdateParam(se);
								prod.setStateRefId(se.getId());
								prod.setState(ModelState.PROD);
								prod.setId(null);
								if (prod.getLangRefId() != null && prod.getLangRefId().isEmpty() == false) {
									// PRODのlangRefIdを探す
									Optional<Series> langBaseSeries = seriesRepo.findById(prod.getLangRefId());
									if (langBaseSeries.isPresent()) {
										prodS = seriesTemp.findByStateRefId(langBaseSeries.get().getId(), ModelState.PROD);
										prod.setLangRefId(prodS.get(0).getId());
									}
								}
								Series saved = seriesRepo.save(prod);
								prodList.add(saved);
								
								// SeriesFaqの PRODを作成
								{
									Optional<SeriesFaq> otFaq = faqRepo.findBySeriesId(saved.getStateRefId());
									if (otFaq.isPresent()) {
										SeriesFaq faq = new SeriesFaq();
										faq.setUpdateParam(otFaq.get());
										faq.setId(null);
										faq.setSeriesId(saved.getId());
										faq.setMtime(new Date());
										faq.setUser(u);
										faq.setState(ModelState.PROD);
										faq.setActive(true);
										faqRepo.save(faq);
									}
								}
							}
						}
						if (prodList.size() > 0) {
							// PRODのCategorySeriesが無ければ作成
							for(Series pSe : prodList) {
								csTemp.upsert(c.getId(), pSe);
							}
							csTemp.updateProdOrder(c.getId(), seriesList);
							// PRODのseries_linkが無ければ作成
							for(Series pSe : prodList) {
								sLinkTemp.deleteBySeriesId(pSe.getId()); // 一旦削除
								List<SeriesLink> sList = sLinkTemp.findBySeriesId(pSe.getStateRefId()); //TESTのseries_linkを取得
								List<SeriesLink> addList = new LinkedList<SeriesLink>();
								for(SeriesLink sl : sList) {
									sl.setSeriesId(pSe.getId());
									sl.setState(ModelState.PROD);
									sl.setId(null);
									addList.add(sl);
								}
								if (addList.size() > 0) sLinkRepo.saveAll(addList);
							}
							// psItemの表示・非表示を更新
							for(Series pSe : prodList) {
								List<PsItem> oItem = psItemRepo.findAllBySid(pSe.getModelNumber());
								if (oItem != null && oItem.size() > 0) {
									for(PsItem item : oItem) {
										if (item.isActive() != pSe.isActive()) {
											item.setActive(pSe.isActive());
											psItemRepo.save(item);
										}
									}
								}
							}
						}
					}
				}
			}
			if (_type.equals(CategoryType.CATALOG)) setContext(up.getLang(), up.getState(), up.getType() );

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
			log.error("changeStateToProdAll()"+e.toString() + e.getMessage() );
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
			log.error("changeStateToProdAll()"+e.toString() + e.getMessage() );
		}
		return ret;
	}

	@Override
	public ErrorObject changeActive(String id, Boolean active) {
		ErrorObject ret = new ErrorObject();
		try {
			Category ca = get(id, ret);
			if (ret.isError()) return ret;

			ca.setActive(active);
			Date dt = new Date();
			ca.setMtime(dt);
			List<Category> list = new ArrayList<Category>();
			list.add(ca);
			List<Category> child = listAll(ca.getId(), null, ret);
			if (ret.isError()) return ret;
			for(Category c : child) {
				c.setActive(active);
				c.setMtime(dt);
				list.add(c);
			}
			List<Category> r = repo.saveAll(list);
			if (r != null) {
				ret.setCount(r.size());
			}

			setContext(ca.getLang(), ca.getState(), ca.getType() );
		} catch (ModelNotFoundException e) {
			ret.setCode(ErrorCode.E10001);
			ret.setMessage(e.getMessage());
		} catch (MongoException e) {
			ret.setCode(ErrorCode.E50001);
			ret.setMessage(e.getMessage());
			log.error("changeActive()"+e.toString() + e.getMessage() );
		} catch (Exception e) {
			ret.setCode(ErrorCode.E99999);
			ret.setMessage(e.getMessage());
			log.error("changeActive()"+e.toString() + e.getMessage() );
		}
		return ret;
	}

	@Override
	public List<Category> listLangRef(String id, ErrorObject err) {
		return temp.findByLangRefId(id);
	}

	final String _contextNamePrefix = "CATEGORY_LIST_";
	@Override
	public  List<Category> listAll(String lang, ModelState state, CategoryType type, ErrorObject err) {

		List<Category> ret = null;
		try{

			Object obj = req.getServletContext().getAttribute(getContextPrefix(lang, state, type));
			if (obj == null) {
				ret = setContext(lang, state, type );
			}
			else {
				ret = (List<Category>)obj;
			}
		}catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error("listAll()"+e.toString() + e.getMessage() );
		}
		return ret;
	}

	@Override
	public List<Category> listAll(String id, Boolean active, ErrorObject err) {
		List<Category> ret = null;
		try {
			Category c = get(id, null);
			if (c == null) throw new ModelNotFoundException("Category id = " + id);
			ret = temp.findChild(c.getId(), c.getState(), c.getType(), active);
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
			log.error("listAll()"+e.toString() + e.getMessage() );
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error("listAll()"+e.toString() + e.getMessage() );
		}
		return ret;
	}

	@Override
	public List<Category> listCategoryFromSeries(String seriesId, @Nullable Boolean active, ErrorObject err)
	{
		List<Category> ret = null;
		try {
			List<CategorySeries> list = csTemp.findBySeriesId(seriesId);
			if (list.size() > 0)
			{
				ret = new ArrayList<Category>();
				for(CategorySeries cs : list) {
					try {
						Category c =get(cs.getCategoryId(), null);
						if (c != null) {
							ret.add( c );
						}
					}catch (Exception e) {
						log.error("Category not Found. id="+cs.getCategoryId());
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
	public List<Category> listCategoryFromSeriesWithCheck(String seriesId, ErrorObject err)
	{
		List<Category> ret = null;
		try {
			List<CategorySeries> list = csTemp.findBySeriesId(seriesId);
			if (list.size() > 0)
			{
				ret = new ArrayList<Category>();
				for(CategorySeries cs : list) {
					try {
						Category c =get(cs.getCategoryId(), null);
						if (c != null) {
							ret.add( c );
						}
					}catch (Exception e) {
						csRepo.delete(cs);
						log.error("Category not Found. Delete id="+cs.getCategoryId());
					}
				}
			}
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
			log.error("listAll()"+e.toString() + e.getMessage() );
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error("listAll()"+e.toString() + e.getMessage() );
		}
		return ret;
	}


	@Override
	public List<Category> search(String keyword, String lang, ModelState state, CategoryType type, Boolean active,
			ErrorObject err) {
		List<Category> ret = null;
		try {
			ret = temp.search(keyword, lang, state, type, active);
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
			Category ca = get(id, ret);
			if (ret.isError()) return ret;

			List<Category> list = temp.findByParentId(ca.getId(), ca.getState(), ca.getType(), null);
			List<Category> saveList = new ArrayList<Category>();
			int order = 1;
			for(String i : ids) {
				boolean isFind = false;
				for(Category c : list) {
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
			// List<Category> list = repo.listAll(lang, state, null);で全部取っているので、
			// これだと全カテゴリを振り直してしまう。コメントアウト。2023/5/5
/*			if (saveList.size() != list.size()) {
				// idsに含まれないlistのorderを再設定
				for(Category c : list) {
					boolean isFind = false;
					for(Category s : saveList) {
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
			}*/
			if (saveList.size() > 0) {
				List<Category> res = repo.saveAll(saveList);
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
			if (ret.isError()) return ret;

			Optional<CategorySeries> oCs = csRepo.findByCategoryId(categoryid);
			List<Series> sList = oCs.get().getSeriesList();
			List<Series> saveList = new ArrayList<Series>();
			for(String i : ids) {
				boolean isFind = false;
				for(Series s : sList) {
					if (s.getId().equals(i)) {
						saveList.add(s);
						isFind = true;
						break;
					}
				}
				if (isFind == false) throw new ModelNotFoundException("sort() ids not Found. id="+i );
			}
			if (saveList.size() != sList.size()) {
				// idsに含まれないlistのorderを再設定
				for(Series c : sList) {
					boolean isFind = false;
					for(Series s : saveList) {
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
				CategorySeries cs = oCs.get();
				cs.setSeriesList(saveList);
				CategorySeries res = csRepo.save(cs);
				ret.setCount(res.getSeriesList().size());
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
	public ErrorObject createRootCategory(String lang) {
		ErrorObject ret = new ErrorObject();
		try {
			String testId = temp.createRootCategory(lang, ModelState.TEST, null, null);
			Category testJpRoot = repo.findRoot("ja-jp",  ModelState.PROD, CategoryType.CATALOG);
			temp.createRootCategory(lang, ModelState.PROD, testId, testJpRoot.getId());
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

	public ErrorObject checkDelete(String id) {
		ErrorObject ret = new ErrorObject();
		try {
			Category c = get(id, ret);
			if (ret.isError()) return ret;

			List<Category> list = listLangRef(id, ret);
			if (ret.isError()) return ret;
			if (list.size() > 0){
				throw new ModelConditionDifferentException("This category is referenced by other countries.");
			}

			if (c.getState().equals(ModelState.PROD)) {
				// PRODならActiveでなければ削除OK
				if (c.isActive()) {
					throw new ModelConditionDifferentException("id is Prod. Active Prod is not Delete.");
				}
				// 子供もいれば子供もチェック
				List<Category> chList = temp.findChild(id, c.getState(), c.getType(), null);
				for(Category ch : chList) {
					if (ch.isActive()) {
						throw new ModelConditionDifferentException("Active Child Prod is not Delete.");
					}
				}
			} else {
				// TESTならPRODがActiveでなければ削除OK。PRODが無ければ削除OK。
				Category prod = null;
				Optional<Category> oProd = repo.findByStateRefId(c.getId());
				if (oProd.isPresent()) {
					prod = oProd.get();
					if (prod.isActive()) {
						throw new ModelConditionDifferentException("Prod is exists.");
					}
					// 子供もいれば子供もチェック
					List<Category> chList = temp.findChild(prod.getId(), prod.getState(), prod.getType(), null);
					for(Category ch : chList) {
						if (ch.isActive()) {
							throw new ModelConditionDifferentException("Active Child Prod is not Delete.");
						}
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
			Category c = get(id, ret);
			if (ret.isError()) return ret;

			ret = checkDelete(id);
			if (ret.isError()) return ret;

			if (c.getState().equals(ModelState.PROD)) {
				// PRODはPRODのみ削除。
/*				{
					Optional<Category> refCa = repo.findById(c.getStateRefId());
					Category test = refCa.get();
					Optional<CategorySeries> cs = csRepo.findByCategoryId(test.getId());
					if (cs.isPresent()) {
						csRepo.delete(cs.get());
					}
					repo.deleteById(test.getId());
					setChildOrder(test.getParentId());
					setContext(test.getLang(), test.getState(), test.getType() );

					ret.setCount(1);
				}*/
				{
					// 子供もいれば子供も削除。
					List<Category> chList = temp.findChild(c.getId(), c.getState(), c.getType(), null);
					for(Category ch : chList) {
						Iterable<CategorySeries> csList = csRepo.findAllByCategoryId(ch.getId());
						if (csList != null) {
							for(CategorySeries cs : csList) {
								csRepo.delete(cs);
							}
						}
						repo.deleteById(ch.getId());
					}
					{
						Iterable<CategorySeries> csList = csRepo.findAllByCategoryId(id);
						if (csList != null) {
							for(CategorySeries cs : csList) {
								csRepo.delete(cs);
							}
						}
					}
					repo.deleteById(id);

					setChildOrder(c.getParentId());
					setContext(c.getLang(), c.getState(), c.getType() );

				}
			} else {
				Category prod = null;
				Optional<Category> oProd = repo.findByStateRefId(c.getId());
				if (oProd.isPresent()) {
					prod = oProd.get();
					if (prod != null) {
						// 子供もいれば子供も削除。
						List<Category> chList = temp.findChild(prod.getId(), prod.getState(), prod.getType(), null);
						for(Category ch : chList) {
							Iterable<CategorySeries> csList = csRepo.findAllByCategoryId(ch.getId());
							if (csList != null) {
								for(CategorySeries cs : csList) {
									csRepo.delete(cs);
								}
							}
							repo.deleteById(ch.getId());
						}
						Iterable<CategorySeries> csList = csRepo.findAllByCategoryId(prod.getId());
						if (csList != null) {
							for(CategorySeries cs : csList) {
								csRepo.delete(cs);
							}
						}
						repo.deleteById(prod.getId());
						setChildOrder(prod.getParentId());
						setContext(prod.getLang(), prod.getState(), prod.getType() );
					}
				}
				// 子供もいれば子供も削除。
				List<Category> chList = temp.findChild(c.getId(), c.getState(), c.getType(), null);
				for(Category ch : chList) {
					Iterable<CategorySeries> csList = csRepo.findAllByCategoryId(ch.getId());
					if (csList != null) {
						for(CategorySeries cs : csList) {
							csRepo.delete(cs);
						}
					}
					repo.deleteById(ch.getId());
				}
				Optional<CategorySeries> cs = csRepo.findByCategoryId(c.getId());
				if (cs.isPresent()) {
					csRepo.delete(cs.get());
				}
				repo.deleteById(c.getId());

				setChildOrder(c.getParentId());
				setContext(c.getLang(), c.getState(), c.getType());

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

	@Override
	public List<Category> listOtherAll(String index, String lang, ModelState state, Boolean active, ErrorObject err) {
		List<Category> ret = null;
		try {
			Category root = temp.findRoot(lang, state, CategoryType.OTHER);
			ret = temp.findChildOther(root.getId(), state, CategoryType.OTHER, index, true);
		} catch (ModelNotFoundException e) {
			err.setCode(ErrorCode.E10001);
			err.setMessage(e.getMessage());
		} catch (MongoException e) {
			err.setCode(ErrorCode.E50001);
			err.setMessage(e.getMessage());
			log.error("listAll()"+e.toString() + e.getMessage() );
		} catch (Exception e) {
			err.setCode(ErrorCode.E99999);
			err.setMessage(e.getMessage());
			log.error("listAll()"+e.toString() + e.getMessage() );
		}
		return ret;
	}


	// ========= private =========
	private boolean moveDir(String from, String to, String lang) {
		boolean ret = false;
		String htmlPath = "";
		try {
			htmlPath = env.getProperty("smc.webcatalog.static.page.path");
		} catch (Exception e) {
			e.printStackTrace();
	    }
		{

			if (from != null && from.equals(to) == false) {
				// ディレクトリの移動
				File file = FileUtils.getFile(htmlPath + lang + "/" + from);
				File file2 = FileUtils.getFile(htmlPath + lang + "/" + to);
				try {
					FileUtils.moveDirectory(file, file2);
					ret = true;
				} catch (IOException e) {
					log.error("moveDirectory() from:"+file+ " to:"+ file2);
				}
			}
		}
		return ret;
	}
	private boolean mkDir(String to, String lang) {
		boolean ret = false;
		String htmlPath = "";
		try {
			htmlPath = env.getProperty("smc.webcatalog.static.page.path");
		} catch (Exception e) {
			e.printStackTrace();
	    }
		try {
			File file2 = FileUtils.getFile(htmlPath + lang + "/" + to);
			FileUtils.forceMkdir(file2);
			ret = true;
		} catch (IOException e) {
			log.error("mkDirectory() to:"+ htmlPath + lang + "/" + to);
		}
		return ret;
	}

	// DBに変更があった場合、Contextも変更
	private List<Category> setContext(String lang, ModelState state, CategoryType type) {
		List<Category> ret = null;
		try{
			Category root = temp.findRoot(lang, state, type);
			ret = temp.findChild(root.getId(), state, type, true);
			ret.add(0, root);
			req.getServletContext().setAttribute(getContextPrefix(lang, state, type), ret);
		}catch (Exception e) {
			log.error(e.getMessage() + e.toString());
		}
		return ret;
	}

	private String getContextPrefix(String lang, ModelState state, CategoryType type)
	{
		return _contextNamePrefix + lang + "_" + state.toString() + "_" + type.toString();
	}

	// 該当IDの子のorderを振り直し
	private boolean setChildOrder(String id ) {
		boolean ret = false;

		ErrorObject obj = new ErrorObject();
		Category parent = getWithChildren(id, null, obj );
		if (!obj.isError()) {
			List<Category> child = parent.getChildren();

			List<Category> list = new ArrayList<Category>();
			int order = 1;
			Date dt = new Date();
			for(Category c : child) {
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

	// StateRefが一致するPRODのCategory
	// listは
	private Category getStateRefId(String testId, List<Category> list) {
		Category ret = null;
		if (list == null) return ret;

		for(Category c : list) {
			if (c.getStateRefId() != null && c.getStateRefId().equals(testId)) {
				ret = c;
				break;
			}
		}
		return ret;
	}


}

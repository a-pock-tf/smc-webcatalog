package com.smc.webcatalog.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.mongodb.MongoException;
import com.smc.exception.ModelExistsException;
import com.smc.webcatalog.config.ErrorCode;
import com.smc.webcatalog.dao.NarrowDownColumnRepository;
import com.smc.webcatalog.dao.NarrowDownColumnTemplateImpl;
import com.smc.webcatalog.dao.NarrowDownValueRepository;
import com.smc.webcatalog.dao.NarrowDownValueTemplateImpl;
import com.smc.webcatalog.dao.SeriesRepository;
import com.smc.webcatalog.dao.SeriesTemplateImpl;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.NarrowDownColumn;
import com.smc.webcatalog.model.NarrowDownValue;
import com.smc.webcatalog.model.Series;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NarrowDownServiceImpl implements NarrowDownService {

	@Autowired
	NarrowDownColumnRepository repo;
	
	@Autowired
	NarrowDownColumnTemplateImpl template;
	
	@Autowired
	NarrowDownValueRepository vRepo;

	@Autowired
	NarrowDownValueTemplateImpl vTemp;

	@Autowired
	SeriesRepository seriesRepo;

	@Autowired
	SeriesTemplateImpl seriesTemp;
	
	@Autowired
	@Qualifier("narrowDownColumns")
	List<NarrowDownColumn> narrowDownColumns;
	
	@Override
	public ErrorObject saveColumn(NarrowDownColumn col) {
		ErrorObject ret = new ErrorObject();
		try {
			// 新規の場合
			if (StringUtils.isEmpty(col.getId())) {
				col.setId(null);
			} 
			col.setMtime(new Date()); // Always update mtime

			col = repo.save(col);
			ret.setCount(1);
			
			refreshNarrowDownColumns(); // Bean更新

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
	public ErrorObject saveValue(NarrowDownValue val) {
		ErrorObject ret = new ErrorObject();
		try {
			// 新規の場合
			if (StringUtils.isEmpty(val.getId())) {
				val.setId(null);
			} 
			val.setMtime(new Date()); // Always update mtime

			val = vRepo.save(val);
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
	public ErrorObject deleteCategoryColumn(String categoryId) {
		ErrorObject ret = new ErrorObject();
		try {
			repo.deleteByCategoryId(categoryId);
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
	public ErrorObject deleteColumn(String columnId) {
		ErrorObject ret = new ErrorObject();
		try {
			repo.deleteById(columnId);
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
	public ErrorObject deleteProdColumn(String childColumnId) {
		ErrorObject ret = new ErrorObject();
		try {
			Optional<NarrowDownColumn> oC = repo.findByStateRefId(childColumnId);
			if (oC.isPresent()) {
				repo.deleteByStateRefId(childColumnId);
				vRepo.deleteAllByColumnId(oC.get().getId());
			}
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
	public ErrorObject deleteSeriesValue(String seriesId) {
		ErrorObject ret = new ErrorObject();
		try {
			vRepo.deleteBySeriesId(seriesId);
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
	public ErrorObject deleteCategoryValue(String categoryId) {
		ErrorObject ret = new ErrorObject();
		try {
			vRepo.deleteByCategoryId(categoryId);
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
	public ErrorObject deleteColumnValue(String columnId) {
		ErrorObject ret = new ErrorObject();
		try {
			vRepo.deleteAllByColumnId(columnId);
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
	public List<NarrowDownColumn> getCategoryColumn(String categoryId, Boolean active, ErrorObject err) {
		List<NarrowDownColumn> ret = new ArrayList<>();
		try {
			ret = template.findByCategoryId(categoryId, active);
		} catch (ModelExistsException e) {
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
	public List<NarrowDownValue> getCategorySeriesValue(String categoryId, String seriesId, Boolean active,
			ErrorObject err) {
		List<NarrowDownValue> ret = vRepo.findAllByCategoryIdAndSeriesId(categoryId, seriesId);
		return ret;
	}

	@Override
	public NarrowDownColumn getColumn(String id, ErrorObject err) {
		NarrowDownColumn ret = null;
		try {
			Optional<NarrowDownColumn> op = repo.findById(id);
			if (op.isPresent()) {
				ret = op.get();
			}
		} catch (ModelExistsException e) {
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
	public NarrowDownValue getValue(String id, ErrorObject err) {
		NarrowDownValue ret = null;
		try {
			Optional<NarrowDownValue> op = vRepo.findById(id);
			if (op.isPresent()) {
				ret = op.get();
			}
		} catch (ModelExistsException e) {
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
	public List<Series> getNarrowDown(String categoryId, HttpServletRequest request, ErrorObject err) {
		List<Series> ret = null;
		try {
			List<NarrowDownColumn> list = template.findByCategoryId(categoryId, null);
			if (list != null) {
				HashMap<String, List<String>> map = new HashMap<>();
				List<NarrowDownColumn> inList = new LinkedList<>();
				int cnt = 0;
				for(NarrowDownColumn c : list) {
					String[] arr = request.getParameterValues("nVal"+cnt);
					if (arr != null && arr.length > 0) {
						if (arr[0].isEmpty() == false) { // 1つのみでカラは除外。select
							List<String> tempList = new LinkedList<>();
							String[] vals = c.getValues();
							for(String strIdx : arr) {
								int idx = Integer.parseInt(strIdx)-1;
								if (idx >= 0 && vals.length > idx) {
									tempList.add(vals[idx]); 
								}
							}
							inList.add(c);
							map.put(c.getId(), tempList);
						}
					}
					cnt++;
				}
				if (inList.size() > 0) {
					List<NarrowDownValue> vList = vTemp.search(inList, map); // シリーズから見るとor。全ColumnIDにヒットしているシリーズのみ表示
					if (vList != null && vList.size() > 0) {
						int chkCnt = inList.size(); // 1シリーズに付き、chkCnt分なければヒットとみなさない。
						HashMap<String, List<String>> chkMap = new HashMap<>();
						for(NarrowDownValue val : vList) {
							List<String> tmplist = chkMap.get(val.getSeriesId());
							if (tmplist == null) {
								tmplist = new ArrayList<>();
							}
							tmplist.add(val.getId());
							chkMap.put(val.getSeriesId(), tmplist);
						}
						Set<String> keyset = chkMap.keySet();
						for(String key : keyset) {
							List<String> chkList = chkMap.get(key);
							if (chkList != null && chkList.size() == chkCnt) {
								if (ret == null) ret = new LinkedList<>();
								Optional<Series> s = seriesRepo.findById(key);
								if (s.isPresent()) {
									ret.add(s.get());
								}
							}
						}
					}
				} else  {
					err.setCode(ErrorCode.E10005);
					err.setMessage("Narrow down search condition is empty.");
				}
			}
		} catch (ModelExistsException e) {
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
	public ErrorObject changeStateColumnValue(String testId, String prodId) {
		ErrorObject ret = new ErrorObject();
		try {
			List<NarrowDownColumn> list = repo.findAllByCategoryId(testId);
			if (list != null) {
				for(NarrowDownColumn col : list) {
					NarrowDownColumn prod = null;
					Optional<NarrowDownColumn> oC = repo.findByStateRefId(col.getId());
					if (oC.isPresent()) {
						prod = oC.get();
						prod.setUpdateParam(col);
						prod.setState(ModelState.PROD);
					} else {
						prod = new NarrowDownColumn();
						prod.setId(null);
						prod.setCategoryId(prodId);
						prod.setUpdateParam(col);
						prod.setState(ModelState.PROD);
						prod.setStateRefId(col.getId());
					}
					NarrowDownColumn saved = repo.save(prod);
					// Valueの更新
					List<NarrowDownValue> vList = vRepo.findAllByColumnId(col.getId());
					for(NarrowDownValue v : vList) {
						NarrowDownValue vProd = null;
						Optional<NarrowDownValue> oPV = vRepo.findByStateRefId(v.getId());
						if (oPV.isPresent()) {
							vProd = oPV.get();
							vProd.setUpdateParam(v);
							vProd.setCategoryId(prodId);
							vProd.setColumnId(saved.getId());
							vProd.setStateRefId(v.getId());
							List<Series> sList = seriesTemp.findByStateRefId(v.getSeriesId(), ModelState.PROD);
							if (sList != null && sList.size() > 0) {
								vProd.setSeriesId(sList.get(0).getId());
							}
						} else {
							vProd = new NarrowDownValue();
							vProd.setId(null);
							vProd.setUpdateParam(v);
							vProd.setCategoryId(prodId);
							vProd.setColumnId(saved.getId());
							vProd.setStateRefId(v.getId());
							vProd.setState(ModelState.PROD);
							List<Series> sList = seriesTemp.findByStateRefId(v.getSeriesId(), ModelState.PROD);
							if (sList != null && sList.size() > 0) {
								vProd.setSeriesId(sList.get(0).getId());
							}
						}
						vRepo.save(vProd);
					}
					
				}
				refreshNarrowDownColumns(); // Bean更新
			}
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


	// ===== 以下、List<NarrowDownColumn>の処理 =====

	@Override
	public void refreshNarrowDownColumns() {
		narrowDownColumns = repo.findAll();
		
	}

	@Override
	public void addNarrowDownColumn(NarrowDownColumn col) {
		if (narrowDownColumns != null) {
			narrowDownColumns.add(col);
		}
	}

	@Override
	public void removeNarrowDownColumn(NarrowDownColumn col) {
		if (narrowDownColumns != null) {
			for (NarrowDownColumn c : narrowDownColumns) {
				if (c.getId().equals(col.getId()) ) {
					narrowDownColumns.remove(c);
					break;
				}
			}
		}
	}

	@Override
	public List<NarrowDownColumn> findByCategoryIdFromBean(String lang, String id) {
		List<NarrowDownColumn> ret = null;
		if (narrowDownColumns != null) {
			for (NarrowDownColumn t : narrowDownColumns) {
				if (t.getLang().equals(lang) && t.getCategoryId().equals(id)) {
					if (ret == null) ret = new LinkedList<>();
					ret.add( t);
				}
			}
		}
		return ret;
	}

}

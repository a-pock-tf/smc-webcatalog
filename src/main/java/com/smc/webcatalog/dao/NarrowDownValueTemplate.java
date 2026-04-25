package com.smc.webcatalog.dao;

import java.util.HashMap;
import java.util.List;

import com.smc.webcatalog.model.NarrowDownColumn;
import com.smc.webcatalog.model.NarrowDownValue;

/***
 * MongoRepositoryで足りないものはこちらで
 * @author miyasit
 *
 */
public interface NarrowDownValueTemplate {

	List<NarrowDownValue> findAllByRange(String columnId, String val);
	
	List<NarrowDownValue> findAllByValue(String columnId, String val);

	List<NarrowDownValue> findAllByValues(String columnId, String[] vals);
	
	List<NarrowDownValue> search(List<NarrowDownColumn> columns, HashMap<String, List<String>> map);
}

package com.smc.webcatalog.dao;

import java.util.ArrayList;
import java.util.List;

import com.smc.exception.DataAccessException;
import com.smc.util.ImpLog;
import com.smc.webcatalog.model.ImpCategory;
import com.smc.webcatalog.model.ImpItemType;

public class ImpCategoryDao extends ImpIBatisBaseDao {

	public ImpCategoryDao(ImpDaoStatus status){
		super.initDao(status);
	}

	public void insert(ImpCategory ca){

		ImpLog.logStart(this,"insert");

		try{
			startTrans();

			/*
			Integer maxorder = getMaxOrder();
			ca.setOrder(maxorder+1);
			*/

			mapclient.insert("insertCategory",ca);

			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}

	private void updateOrder(ImpCategory ca){

		ImpLog.logStart(this,"updateOrder");

		//notransaction
		try{
			//startTrans();

			mapclient.update("updateCategoryOrder",ca);

			//commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			//endTrans();
		}

	}

	public int getMaxOrder(int parent_ca_id){
		Integer maxorder = 0;

		try{
			maxorder = (Integer)mapclient.queryForObject("getCategoryMaxOrder",parent_ca_id);
			if(maxorder==null) maxorder = 0;

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}
		return maxorder;
	}


	public int getMaxId(){
		Integer max = 0;

		try{
			max = (Integer)mapclient.queryForObject("getCategoryMaxId");
			if(max==null) max = 0;

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}
		return max;
	}

	public void update(ImpCategory ca){

		ImpLog.logStart(this,"update");

		try{
			startTrans();
			mapclient.update("updateCategory",ca);

			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}

	public void delete(int id){

		ImpLog.logStart(this,"delete");

		try{
			startTrans();
			mapclient.delete("deleteCategory",id);
			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}

	/*�Y������̃J�e�S��(WEB�J�^���O�̂�)���폜*/
	public void deleteAll(String lang){

		ImpLog.logStart(this,"deleteAll");

		try{
			startTrans();
			mapclient.delete("deleteAllCategory",lang);
			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}



	//���ʂ̊K�w�����擾
	/*
	public int getBottomSize(int ca_id,ItemType type,int c){

		List<ImpCategory> list = list(ca_id,type);
		if(c!=0){
			if(list.size()>0){
			getBottomSize(list.get(0).getId(), type, c);
			c++;
			}
		}

		return c;
	}*/



	public ImpCategory get(int id){
		//Log.logStart(this,"get");
		ImpCategory ca = null;
		try{

			ca = (ImpCategory)mapclient.queryForObject("getCategory",id);
			setParent(id, ca);

		}catch(Exception ex){
			ImpLog.logEx(ex);
			ImpLog.log(ca.getName());
			throw new DataAccessException();
		}
		return ca;
	}

	public List<ImpCategory> list(int parent_id,ImpItemType type){

		ImpCategory pca = new ImpCategory(type);
		pca.setParent_id(parent_id);

		List<ImpCategory> list = new ArrayList<ImpCategory>();

		try{

			list = (List<ImpCategory>)mapclient.queryForList("listCategory",pca);

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}
		return list;
	}

	public List<ImpCategory> listByName(String name,ImpItemType type){

		ImpCategory pca = new ImpCategory(type);
		pca.setName(name);

		List<ImpCategory> list = new ArrayList<ImpCategory>();

		try{

			ImpCategory ca = (ImpCategory)mapclient.queryForObject("getCategoryByName",pca);
			if(ca!=null){
			pca = new ImpCategory(type);
			pca.setParent_id(ca.getId());
			list = (List<ImpCategory>)mapclient.queryForList("listCategory",pca);
			}

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}
		return list;
	}

	public void setParent(int id,ImpCategory ca){


		try{

			ImpCategory parent = (ImpCategory)mapclient.queryForObject("getCategory",id);
			if(parent!=null){
				ca.getParents().addFirst(parent);
			}

			if(parent!=null&&parent.getParent_id()!=0){
				setParent(parent.getParent_id(), ca);
			}

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}
	}


	public void moveOrder(int src_id,int dest_order,ImpItemType type){

		ImpLog.logStart(this,"moveOrder");

		try{
			//int maxorder = getMaxOrder();

			//order�̌�Ɉړ�
        	if(dest_order>=0){

        		//Series src = get(src_it_id);
        		ImpCategory src = get(src_id);

        		List<ImpCategory> list = list(src_id,type);
        		list.remove(src.getOrder()-1);

        		if(dest_order>=list.size()){
        			dest_order = list.size();
        		}

        		list.add(dest_order,src);

        		renumberOrder(list);
        	}




		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}

	}


	public void renumberOrder(List<ImpCategory> list){

		ImpLog.logStart(this,"renumberOrder");

		try{
			startTrans();
	    		int o = 0;
	    		for(ImpCategory _ca:list){
	    			o++;
	    			_ca.setOrder(o);
	    			updateOrder(_ca);
	    		}

	    	commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}


}

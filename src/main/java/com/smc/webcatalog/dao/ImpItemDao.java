package com.smc.webcatalog.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.smc.exception.DataAccessException;
import com.smc.util.ImpItemUtil;
import com.smc.util.ImpLog;
import com.smc.util.ImpResult;
import com.smc.util.ImpSearchResult;
import com.smc.webcatalog.model.ImpDbItem;
import com.smc.webcatalog.model.ImpItem;
import com.smc.webcatalog.model.ImpSearchCondition;
import com.smc.webcatalog.model.ImpSeries;

public class ImpItemDao extends ImpIBatisBaseDao {

	public ImpItemDao(ImpDaoStatus status){
		super.initDao(status);
	}

	public void insert(ImpDbItem dbitem){


		try{
			startTrans();

			Integer maxorder = getMaxOrder(dbitem.getSe_id());
			dbitem.setOrder(maxorder+1);

			mapclient.insert("insertItem",dbitem);

			commitTrans();
		}catch(Exception ex){
		}finally{
			endTrans();
		}

	}

	private void updateOrder(ImpDbItem dbitem){

		//notransaction
		try{
			//startTrans();

			mapclient.update("updateItemOrder",dbitem);

			//commitTrans();
		}catch(Exception ex){
		}finally{
			//endTrans();
		}

	}


	public long getItemMax(){

		long c = 0;

		try{
			c = (Long)mapclient.queryForObject("getItemMax");

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}
		return c;
	}

	public int getMaxOrder(long se_id){
		Integer maxorder = 0;

		try{
			maxorder = (Integer)mapclient.queryForObject("getItemMaxOrder",se_id);
			ImpLog.log("maxorder======="+maxorder);
			if(maxorder==null) maxorder = 0;

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}
		return maxorder;
	}

	public void update(ImpDbItem dbitem){

		ImpLog.logStart(this,"update");

		try{
			startTrans();
			mapclient.update("updateItem",dbitem);

			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}

	public void delete(long id){

		ImpLog.logStart(this,"delete");

		try{
			startTrans();
			mapclient.delete("deleteItem",id);
			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}

	/**
	 * �Y���J�����̃f�[�^���ׂĂ��폜
	 * @param name
	 */
	public void clearItemCol(String colname,Long se_id){

		ImpLog.logStart(this,"clearItemCol");
		Map<String,Object> params = new HashMap<String,Object>();
		params.put("colname", colname);
		params.put("se_id", se_id);


		try{
			startTrans();
			mapclient.update("clearItemCol",params);
			commitTrans();

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}



	public void deleteBySeId(long se_id){

		ImpLog.logStart(this,"deleteBySeId");

		try{
			startTrans();
			mapclient.delete("deleteItemBySeId",se_id);

			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}

	public List<ImpDbItem> listBySeries_id(long id){
		return listBySeries_id(id,null);
	}

	public List<ImpDbItem> listBySeries_id(long id,Boolean active){

		ImpSearchCondition cond = new ImpSearchCondition();
		cond.setId(id);
		cond.setActive(active);



		List<ImpDbItem> dbitemlist = new ArrayList<ImpDbItem>();

		try{

			dbitemlist = (List<ImpDbItem>)mapclient.queryForList("listItemBySeries_id",cond);

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}


		return dbitemlist;

	}


	//ImpSeries->Items�̌������ʂƂ��Č������� �V���[�YID�Ń��X�g
	public List<ImpSeries> listBySeids(List<Long> se_ids){

		List<ImpSeries> series = new ArrayList<ImpSeries>();

		try{

			ImpSeriesDao sedao = new ImpSeriesDao(this.status);

			if(se_ids.size()>0){

				series = sedao.listSeriesBySeids(se_ids);

				for(ImpSeries se:series){
					//Log.log(se.getId()+"/"+se.getName());
					List<ImpDbItem> list = listBySeries_id(se.getId());
					List<ImpItem> itemlist = new LinkedList<ImpItem>();
					for(ImpDbItem dbitem:list){
						ImpItem i = ImpItemUtil.createItem(se, dbitem);
						//Log.logReflec(i.getMapByCname("c1"));
						itemlist.add(i);
					}
					se.setItems(itemlist);
				}
			}

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}

		return series;
	}


    public ImpSearchResult<ImpSeries> searchBySeries(Boolean se_active,int ca_id,int parent_ca_id[],List<String> ids,String cname,List<String> keywords,String lang,String order,int limit,int offset,int show){
        return  searchBySeries(se_active, ca_id, parent_ca_id, null, ids, cname, keywords, lang, order, limit, offset, show);
    }
    public ImpSearchResult<ImpSeries> searchBySeries(Boolean se_active,int ca_id,int parent_ca_id[],long[] seids,String cname,List<String> keywords,String lang,String order,int limit,int offset,int show){
        return  searchBySeries(se_active, ca_id, parent_ca_id, seids, null, cname, keywords, lang, order, limit, offset, show);
    }

	//ImpSeries->Items�̌������ʂƂ��Č�������
	public ImpSearchResult<ImpSeries> searchBySeries(Boolean se_active,int ca_id,int parent_ca_id[],long[] seids,List<String> ids,String cname,List<String> keywords,String lang,String order,int limit,int offset,int show){

		ImpLog.logStart(this,"searchBySeries");
		/*
		Map<String,Object> params = new HashMap<String, Object>();
		params.put("se_id", se_id);
		params.put("cname", cname);
		params.put("keywords", keywords);
		*/

		ImpSearchResult<ImpSeries> s = null;

		if(cname!=null&&!cname.matches("^c\\d{1,2}$")){
				throw new DataAccessException();
		}

		if(order!=null&&!order.matches("^(c\\d{1,2}$)|(it_id)|(se_id)")){
			throw new DataAccessException();
		}


		ImpResult<ImpSeries> result = new ImpResult<ImpSeries>();
		try{

			ImpSeriesDao sedao = new ImpSeriesDao(this.status);
			int max = 0;

			//�V���[�Y(se_id)����������Ă���
			if(seids!=null&&seids.length>0){
				for(Long se_id:seids){
					ImpSeries se = sedao.get(se_id);

					if(se!=null){
					    if(se_active!=null&&se_active){
					        if(!se.getActive()) se=null;
					    }
					}
					if(se!=null){
						max++;
						ImpSearchResult<ImpItem> list = search(se_active, se,null,keywords,null,Integer.MAX_VALUE,0);
						se.setItems(list.getResult());
						result.add(se);
					}
				}

			//id(sid)����������Ă���
			}else if(ids!=null&&ids.size()>0){
                for(String _sid:ids){
                    ImpSeries se = sedao.getBySid(_sid);

                    if(se!=null){
                        if(se_active!=null&&se_active){
                            if(!se.getActive()) se=null;
                        }
                    }

                    if(se!=null){
                        max++;
                        ImpSearchResult<ImpItem> list = search(se_active, se,null,keywords,null,Integer.MAX_VALUE,0);
                        se.setItems(list.getResult());
                        result.add(se);
                    }
                }

			//�J�e�S������������Ă���
			}else if(ca_id!=0){


				//�������ʂ��܂ރV���[�Y���擾
				ImpSearchResult<ImpSeries> selist = sedao.searchSeriesByCategory(se_active,ca_id,limit,offset);
				max = selist.getMax();
				//item���擾
				for(ImpSeries se:selist.getResult()){
					List<ImpDbItem> list = listBySeries_id(se.getId(),se_active);
					List<ImpItem> itemlist = new LinkedList<ImpItem>();
					for(ImpDbItem dbitem:list){
						ImpItem i = ImpItemUtil.createItem(se, dbitem);
						//Log.logReflec(i.getMapByCname("c1"));
						itemlist.add(i);
					}
					se.setItems(itemlist);

					result.add(se);
				}



			}else{

				//�������ʂ��܂ރV���[�Y���擾
				if(parent_ca_id!=null){
				for(int p_ca_id:parent_ca_id){
					ImpSearchResult<ImpSeries> selist = sedao.searchSeries(se_active ,p_ca_id,cname, keywords, lang, limit, offset);
					max+= selist.getMax();
					//item���擾
					for(ImpSeries se:selist.getResult()){
						List<ImpDbItem> list = listBySeries_id(se.getId(),se_active);
						List<ImpItem> itemlist = new LinkedList<ImpItem>();
						for(ImpDbItem dbitem:list){
							ImpItem i = ImpItemUtil.createItem(se, dbitem);
							//Log.logReflec(i.getMapByCname("c1"));
							itemlist.add(i);
						}
						se.setItems(itemlist);

						result.add(se);
					}
				}

				//�L�[���[�h�̂�
				}else{

					ImpSearchResult<ImpSeries> selist = sedao.searchSeries(se_active , 0,cname, keywords, lang, limit, offset);
					//item���擾
					for(ImpSeries se:selist.getResult()){
						List<ImpDbItem> list = listBySeries_id(se.getId(),se_active);
						List<ImpItem> itemlist = new LinkedList<ImpItem>();
						for(ImpDbItem dbitem:list){
							ImpItem i = ImpItemUtil.createItem(se, dbitem);
							//Log.logReflec(i.getMapByCname("c1"));
							itemlist.add(i);
						}
						se.setItems(itemlist);

						result.add(se);
					}
				}

			}

			s = new ImpSearchResult<ImpSeries>(max, limit, offset,show);
			s.setResult(result);


		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}


		return s;



	}


	public ImpSearchResult<ImpItem> search(Boolean active,ImpSeries se,String cname,List<String> keywords,String order,int limit,int offset){

		ImpLog.logStart(this,"search");
		/*
		Map<String,Object> params = new HashMap<String, Object>();
		params.put("se_id", se_id);
		params.put("cname", cname);
		params.put("keywords", keywords);
		*/

		ImpSearchResult<ImpItem> s ;

		if(cname!=null&&!cname.matches("^c\\d{1,2}$")){
				throw new DataAccessException();
		}

		if(order!=null&&!order.matches("^(c\\d{1,2}$)|(it_id)|(se_id)|(it_sid)")){
			throw new DataAccessException();
		}



		ImpSearchCondition cond = new ImpSearchCondition();
		cond.setSe_id(se.getId());
		cond.setKeywords(keywords);
		cond.setCname(cname);
		cond.setOrder(order);
		cond.setActive(active);


		List<ImpDbItem> dbitemlist = new ArrayList<ImpDbItem>();
		List<ImpItem> list = new ArrayList<ImpItem>();
		try{

			//�����擾
			//dbitemlist = (List<DbItem>)mapclient.queryForList("searchItem",cond);
			int max = dbitemlist.size();

			cond.setLimit(limit);
			cond.setOffset(offset);

			dbitemlist = (List<ImpDbItem>)mapclient.queryForList("searchItem",cond);


			for(ImpDbItem item:dbitemlist){
            	ImpItem _item = ImpItemUtil.createItem(se, item);
            	list.add(_item);
            }

			s = new ImpSearchResult<ImpItem>(max, limit, offset);
			s.setResult(list);



		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}


		return s;

	}

	public ImpSearchResult<ImpItem> search_global(List<String> keywords,String order,int limit,int offset){

		ImpLog.logStart(this,"search_global");
		/*
		Map<String,Object> params = new HashMap<String, Object>();
		params.put("se_id", se_id);
		params.put("cname", cname);
		params.put("keywords", keywords);
		*/

		ImpSearchResult<ImpItem> s ;

		if(order!=null&&!order.matches("^(c\\d{1,2}$)|(it_id)|(se_id)")){
			throw new DataAccessException();
		}


		ImpSearchCondition cond = new ImpSearchCondition();
		cond.setKeywords(keywords);
		cond.setOrder(order);


		ImpSeriesDao sedao = new ImpSeriesDao(this.status);

		List<ImpDbItem> dbitemlist = new ArrayList<ImpDbItem>();
		List<ImpItem> list = new ArrayList<ImpItem>();
		try{

			//�����擾
			dbitemlist = (List<ImpDbItem>)mapclient.queryForList("searchItemGlobal",cond);
			int max = dbitemlist.size();

			cond.setLimit(limit);
			cond.setOffset(offset);

			dbitemlist = (List<ImpDbItem>)mapclient.queryForList("searchItemGlobal",cond);

			for(ImpDbItem item:dbitemlist){
            	ImpItem _item = ImpItemUtil.createItem(sedao.get(item.getSe_id()), item);
            	list.add(_item);
            }

			s = new ImpSearchResult<ImpItem>(max, limit, offset);
			s.setResult(list);



		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}


		return s;

	}

	public ImpDbItem get(long id){
		ImpDbItem dbitem = null;
		try{

			dbitem = (ImpDbItem)mapclient.queryForObject("getItem",id);

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}
		return dbitem;
	}

	//SID�̏d���`�F�b�N�p
	public ImpDbItem getOtherBySid(String sid , long id){

		ImpDbItem param_dbitem = new ImpDbItem();
		param_dbitem.setSid(sid);
		param_dbitem.setId(id);


		ImpDbItem dbitem = null;
		try{

			dbitem = (ImpDbItem)mapclient.queryForObject("getItemOtherBySid",param_dbitem);

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}
		return dbitem;
	}

	public void moveOrder(long se_id,long src_it_id,int dest_order){

		ImpLog.logStart(this,"moveOrder");

		try{
			int maxorder = getMaxOrder(se_id);

			//order�̌�Ɉړ�
        	if(dest_order>=0){

        		//ImpSeries src = get(src_it_id);
        		ImpDbItem src = get(src_it_id);

        		List<ImpDbItem> list = listBySeries_id(se_id);

        		if(dest_order>=maxorder){
        			dest_order = list.size();
        		}
        		list.remove(src.getOrder()-1);

        		if(dest_order>list.size()){
        			list.add(src);
        		}else{
        			list.add(dest_order,src);
        		}
        		renumberOrder(list);
        	}




		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}

	}


	public void renumberOrder(List<ImpDbItem> list){

		ImpLog.logStart(this,"renumberOrder");

		try{
			startTrans();
	    		int o = 0;
	    		for(ImpDbItem _it:list){
	    			o++;
	    			_it.setOrder(o);
	    			updateOrder(_it);
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

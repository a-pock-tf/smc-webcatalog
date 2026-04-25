package com.smc.webcatalog.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.smc.exception.DataAccessException;
import com.smc.util.ImpItemUtil;
import com.smc.util.ImpLog;
import com.smc.util.ImpSearchResult;
import com.smc.webcatalog.config.ImpSearchMode;
import com.smc.webcatalog.model.ImpCategory;
import com.smc.webcatalog.model.ImpDbItem;
import com.smc.webcatalog.model.ImpItem;
import com.smc.webcatalog.model.ImpSearchCondition;
import com.smc.webcatalog.model.ImpSeries;
import com.smc.webcatalog.model.ImpSeriesMap;

public class ImpSeriesDao extends ImpIBatisBaseDao {


	public ImpSeriesDao(ImpDaoStatus status){
		super.initDao(status);
	}

	/**
	 * seriesmap�̒P�i�ǉ��p
	 * @param map
	 */
	public void insertSeriesMap(ImpSeriesMap map){

		ImpLog.logStart(this,"insertSeriesMap");

		try{
			startTrans();

			mapclient.insert("insertSeriesMap",map);

			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}


	public List<ImpSeries> list(){
		return list(false);
	}

	public List<ImpSeries> list(boolean getItems){

		List<ImpSeries> list = new LinkedList<ImpSeries>();

		ImpItemDao itdao = new ImpItemDao(this.status);

		try{

			list = (List<ImpSeries>)mapclient.queryForList("listSeries");

			if(getItems){
				for(ImpSeries se:list){


					//get seriesmap
					List<ImpSeriesMap> maplist = (List<ImpSeriesMap>)mapclient.queryForList("listSeriesMap",se.getId());
					for(ImpSeriesMap map:maplist){
						se.addSeriesMap(map);
					}
					//set item
					List<ImpDbItem> _list = itdao.listBySeries_id(se.getId(),true);
					List<ImpItem> itemlist = new LinkedList<ImpItem>();
					for(ImpDbItem dbitem:_list){
						ImpItem i = ImpItemUtil.createItem(se, dbitem);
						itemlist.add(i);
					}
					se.setItems(itemlist);

				}
			}

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}

		return list;

	}

	public long getSeriesMax(){

		long c = 0;
		try{

			c = (Long)mapclient.queryForObject("getSeriesMax");

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}

		return c;

	}


	private void setSeries(ImpSeries se,List<ImpSeries> selist){

		try{

		if(se!=null){
			//�J�e�S���Z�b�g
			ImpCategoryDao cadao = new ImpCategoryDao(this.status);

			List<ImpCategory> calist = new LinkedList<ImpCategory>();
			for(ImpSeries _se:selist){

				if (_se.getCs_ca_id() != null) {
					ImpCategory ca = cadao.get(_se.getCs_ca_id());
					ca.setCs_id(_se.getCs_id());
					ca.setCs_order(_se.getCs_order());
					calist.add(ca);
				}

				//Log.log(ca.getName());
			}

			se.setCategories(calist);

			//set seriesmap
			List<ImpSeriesMap> list = (List<ImpSeriesMap>)mapclient.queryForList("listSeriesMap",se.getId());

			for(ImpSeriesMap map:list){
				se.addSeriesMap(map);
			}
		}


		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}


	}

	public ImpSeries getBySid(String sid){
		ImpSeries se = null;

		try{
			//�J�e�S������������̂�2�ȏ㌋�ʂ��A���Ă���ꍇ������
			List<ImpSeries> selist = (List<ImpSeries>)mapclient.queryForList("getSeriesBySid",sid);


			if(selist!=null&&selist.size()>0){
				se = selist.get(0);
			}

			//Log.logReflec(se);
			setSeries(se, selist);

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}

		return se;

	}

	public ImpSeries get(long id){
		return get(id,false);
	}

	public ImpSeries get(long id,boolean getItems){

		ImpSeries se = null;
		ImpItemDao itdao = new ImpItemDao(this.status);

		try{
			//�J�e�S������������̂�2�ȏ㌋�ʂ��A���Ă���ꍇ������
			List<ImpSeries> selist = (List<ImpSeries>)mapclient.queryForList("getSeries",id);

			if(selist!=null&&selist.size()>0){
				se = selist.get(0);
				setSeries(se, selist);

				if(getItems){

					//set item
					List<ImpDbItem> _list = itdao.listBySeries_id(se.getId(),true);
					List<ImpItem> itemlist = new LinkedList<ImpItem>();
					for(ImpDbItem dbitem:_list){
						ImpItem i = ImpItemUtil.createItem(se, dbitem);
						itemlist.add(i);
					}
					se.setItems(itemlist);
				}
			}

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}

		return se;

	}

	//�d���`�F�b�N�p
	public ImpSeries getOtherBySid(String sid,long id){

		ImpSeries se_param = new ImpSeries();
		se_param.setSid(sid);
		se_param.setId(id);

		ImpLog.logStart(this, "getSeriesOtherBySid");

		ImpSeries se = null;

		try{

			se = (ImpSeries)mapclient.queryForObject("getSeriesOtherBySid",se_param);

			/*
			List<ImpSeriesMap> list = (List<ImpSeriesMap>)mapclient.queryForList("listSeriesMap",se.getId());

			for(ImpSeriesMap map:list){
				se.addSeriesMap(map);
			}*/


		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}

		return se;

	}


	public ImpSeriesMap getSeriesMap(long id){

		ImpSeriesMap map = null;

		try{

			map = (ImpSeriesMap)mapclient.queryForObject("getSeriesMap",id);


		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}

		return map;

	}

	public void updateSeriesMap(ImpSeriesMap map){

		try{
			startTrans();

			mapclient.update("updateSeriesMap",map);


			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}


	public Integer getMaxOrder(int parent_ca_id){

		Integer maxorder = 0;

		try{

			maxorder = (Integer)mapclient.queryForObject("getSeriesMaxOrder",parent_ca_id);

			if(maxorder==null) maxorder=0;

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}

		return maxorder;

	}

	public ImpSeries getByOrder(int order){

		ImpSeries se = null;

		try{

			se = (ImpSeries)mapclient.queryForObject("getSeriesByOrder",order);

			List<ImpSeriesMap> list = (List<ImpSeriesMap>)mapclient.queryForList("listSeriesMap",se.getId());

			for(ImpSeriesMap map:list){
				se.addSeriesMap(map);
			}


		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}

		return se;

	}

	public void insert(ImpSeries series){

		ImpLog.logStart(this,"insert");

		try{
			startTrans();

			mapclient.insert("insertSeries",series);

			for(ImpSeriesMap map:series.getSeriesmap().values()){
				map.setSe_id(series.getId());
				mapclient.insert("insertSeriesMap",map);
			}

			//inset category_series
			for(ImpCategory ca:series.getCategories()){
				Integer maxorder = listSeriesByCategory(ca.getId(),null).size();
				insertCategorySeriesPriv(ca.getId(), series.getId(), maxorder+1);
			}

			commitTrans();


		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}


	public void insertForProdServer(ImpSeries series,int ca_id){

		ImpLog.logStart(this,"insertForProdServer");

		try{
			startTrans();

			mapclient.insert("insertSeries",series);

			for(ImpSeriesMap map:series.getSeriesmap().values()){
				map.setSe_id(series.getId());
				mapclient.insert("insertSeriesMap",map);
			}

			//inset category_series
			//�w�肳�ꂽ�J�e�S���ɂ̂ݒǉ�����
			Integer maxorder = listSeriesByCategory(ca_id,null).size();
			insertCategorySeriesPriv(ca_id, series.getId(), maxorder+1);


			commitTrans();


		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}



	public void update(ImpSeries series){

		ImpLog.logStart(this,"update");

		try{

			//�܂�DB����get
			ImpSeries s_db = get(series.getId());

			startTrans();

			//update category_series
			for(ImpCategory ca:series.getCategories()){

				boolean find = false;

				for(ImpCategory ca_db:s_db.getCategories()){
					if(ca_db.getId()==ca.getId()){
						find = true;
					}
				}
				//�Ȃ����̂�insert
				if(!find){
					ImpLog.log("insert ca_id="+ca.getId()+ " se_id="+series.getId());
					Integer maxorder = listSeriesByCategory(ca.getId(),null).size();
					insertCategorySeriesPriv(ca.getId(), series.getId(), maxorder+1);
				}

			}

			//�폜�������̂��폜
			for(ImpCategory ca_db:s_db.getCategories()){
				boolean find = false;
				for(ImpCategory ca:series.getCategories()){
					if(ca_db.getId()==ca.getId()){
						find = true;
					}
				}
				if(!find){
					deleteCategorySeries(ca_db.getId(), series.getId());
				}
			}

			//update
			mapclient.update("updateSeries",series);

			//update series map
			int order = 0;
			for(ImpSeriesMap map:series.getSeriesmap().values()){
				order++;
				map.setOrder(order);
				mapclient.update("updateSeriesMap",map);
			}



			commitTrans();

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}

	/**
	 * ImpSeries�e�[�u���̂�update
	 * @param series
	 */
	public void updateSimple(ImpSeries series){

		ImpLog.logStart(this,"updateSimple");

		try{

			startTrans();

			//update
			mapclient.update("updateSeries",series);

			commitTrans();

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}


	public void updateKeyword(ImpSeries se){

		ImpLog.logStart(this,"updateKeyword");

		try{

			String sepa = ",";
			String k = "";

    		k += se.getName()+sepa+se.getName2()+sepa;
			ImpLog.log("��"+se.getName()+"/"+se.getName2());
			for(ImpItem _it:se.getItems()){
				for(ImpSeriesMap _map:_it.getSeriesmap().values()){
					if(_map.getName().contains("�V���[�Y")){
						k += _map.getValue()+sepa;
					}
				}
			}
			k = k.replaceAll("( |�@)+", "");
			ImpLog.log(k);

			startTrans();
			se.setKeyword(k);

			//update
			mapclient.update("updateSeriesKeyword",se);


			commitTrans();

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}

	public void updateCatpans_txt(ImpCategory old_ca,ImpCategory new_ca){

		ImpLog.logStart(this,"updateCatpans_txt");

		Map<String,String> map = new HashMap<String, String>();
		map.put("regexp", "%"+old_ca.getId()+"%[^#]+#");

		String name = new_ca.getName();
		if(name!=null){
			name = name.replaceAll("�y����[^�z]*�z", "");
		}

		map.put("rep", "%"+old_ca.getId()+"%"+name+"#");
		map.put("where", "%"+old_ca.getId()+"%");


		try{

			startTrans();

			//update
			mapclient.update("updateSeriesCatpans_txt",map);


			commitTrans();

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}



	/**�{�ԃT�[�o�[UP�p update**/
	public void updateForProdServer(ImpSeries series){

		ImpLog.logStart(this,"updateForProdServer");

		try{

			startTrans();

			//update
			mapclient.update("updateSeries",series);

			//delete seriesmap
			mapclient.delete("deleteSeriesMapBySeriesId",series.getId());

			//insert seriesmap
			for(ImpSeriesMap map:series.getSeriesmap().values()){
				map.setSe_id(series.getId());
				mapclient.insert("insertSeriesMap",map);
			}

			commitTrans();

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}

	public void updateSeriesCa_id(int src_ca_id,int dst_ca_id){

		ImpSearchCondition cond = new ImpSearchCondition();
		cond.setSrc_ca_id(src_ca_id);
		cond.setDst_ca_id(dst_ca_id);

		ImpLog.logStart(this,"updateSeriesCa_id");

		try{
			startTrans();

			mapclient.update("updateSeriesCa_id",cond);

			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}

	@Deprecated
	private void updateOrder(ImpSeries se){

		ImpLog.logStart(this,"updateOrder");

		try{
			//startTrans();
			mapclient.insert("updateSeriesOrder",se);

			//commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			//endTrans();
		}

	}



	public void renumberOrder(List<ImpSeries> list,int ca_id){

		ImpLog.logStart(this,"renumberOrder");

		try{
			startTrans();

			int o = 0;
			for(ImpSeries se:list){
				o++;
				updateCategorySeriesOrder(ca_id, se.getId(), o);
			}

			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}


	private void updateCategorySeriesOrder(int cs_ca_id,long se_id,int cs_order){

		ImpSeries param = new ImpSeries();
		param.setId(se_id);
		param.setCs_ca_id(cs_ca_id);
		param.setCs_order(cs_order);

		try{
			//startTrans();

			mapclient.update("update_category_series_order",param);


			//commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			//endTrans();
		}

	}

	public void updateCad3d(long se_id,boolean hascad3d){


		try{
			startTrans();

			updateCad3dNoTrans(se_id, hascad3d);

			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}

	//�܂�������ł��ׂ�false��
    public void updateHasXAll(String colname, String lang, Boolean bool){

        ImpLog.log("updateHasXAll");

        ImpSearchCondition cond = new ImpSearchCondition();
        cond.setLang(lang);
        cond.setColname(colname);
        cond.setBool(bool);
        try{
            startTrans();

            mapclient.update("updateSeriesHasXAll",cond);


            commitTrans();
        }catch(Exception ex){
            ImpLog.logEx(ex);
            throw new DataAccessException();
        }finally{
            endTrans();
        }

    }


    public void updateHasXNoTrans(long se_id,String colname,Boolean bool){

        //Log.log("updateHasXNoTrans");
        ImpSearchCondition cond = new ImpSearchCondition();
        cond.setId(se_id);
        cond.setColname(colname);
        cond.setBool(bool);

        try{
            //startTrans();

            mapclient.update("updateSeriesHasX",cond);


            //commitTrans();
        }catch(Exception ex){
            ImpLog.logEx(ex);
            throw new DataAccessException();
        }finally{
            //endTrans();
        }

    }

    @Deprecated
	public void updateCad3dNoTrans(long se_id,boolean hascad3d){

		//Log.log("updateSeriesCad3dNoTrans");

		ImpSeries param = new ImpSeries();
		param.setId(se_id);
		param.setHascad3d(hascad3d);

		try{
			//startTrans();

			mapclient.update("updateSeriesCad3d",param);


			//commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			//endTrans();
		}

	}

	public void renumberSeriesMapOrder(Collection<ImpSeriesMap> list){

		ImpLog.logStart(this,"renumberSeriesMapOrder");

		try{
			startTrans();

			int o = 0;
			for(ImpSeriesMap map:list){
				o++;
				map.setOrder(o);
				mapclient.update("updateSeriesMap",map);
			}

			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}



	public void moveOrder(int ca_id,long src_id,int dst_order){

		ImpLog.logStart(this,"moveOrder");

		try{
			ImpSeries src = get(src_id);

			List<ImpSeries> list = listSeriesByCategory(ca_id,null);
			List<ImpSeries> newlist = new LinkedList<ImpSeries>();

			//�܂��Y���̂��̂������Ă��Ȃ����X�g���쐬
			for(ImpSeries se:list){
				if(se.getId()!=src_id){
					newlist.add(se);
				}
			}

			if(dst_order>newlist.size()) dst_order=newlist.size();
			//�ʒu���w�肵��add
			newlist.add(dst_order,src);

			renumberOrder(newlist,ca_id);


		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}

	}

	public void delete(long id){

		ImpLog.logStart(this,"delete");

		try{
			startTrans();

			ImpSeries se = get(id);

			mapclient.delete("deleteSeries",id);
			mapclient.delete("deleteSeriesMapBySeriesId",id);
			mapclient.delete("delete_category_series",se);

			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}

	public void deleteAllCategorySeries(){

		ImpLog.logStart(this,"delete_all_category_series");

		try{
			startTrans();

			mapclient.delete("delete_all_category_series");

			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}

	public void deleteCategorySeriesByCa_id(int ca_id){

		ImpLog.logStart(this,"deleteCategorySereiesByCa_id");

		try{
			startTrans();

			mapclient.delete("delete_category_series_by_ca_id",ca_id);

			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}

	private void deleteCategorySeries(int ca_id,long se_id){

		ImpLog.logStart(this,"deleteCategorySereies");

		ImpSeries param = new ImpSeries();
		param.setCs_ca_id(ca_id);
		param.setId(se_id);

		try{
			//startTrans();

			mapclient.delete("delete_category_series",param);

			//commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			//endTrans();
		}

	}


	public void deleteCategorySeriesBySe_id(long se_id){

		ImpLog.logStart(this,"deleteCategorySereiesBySe_id");

		try{
			startTrans();

			mapclient.delete("delete_category_series_by_se_id",se_id);

			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}


	private void insertCategorySeriesPriv(int cs_ca_id,long se_id,int order){

		ImpSeries param = new ImpSeries();
		param.setCs_ca_id(cs_ca_id);
		param.setId(se_id);
		param.setOrder(order);
		ImpLog.logStart(this,"insertCategorySeries");

		try{
			//startTrans();

			mapclient.insert("insert_category_series",param);

			//commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			//endTrans();
		}

	}

	public void insertCategorySeries(int ca_id,long se_id,int order){

		ImpSeries param = new ImpSeries();
		param.setCa_id(ca_id);
		param.setId(se_id);
		param.setOrder(order);
		ImpLog.logStart(this,"insertCategorySeries");

		try{


			startTrans();

			insertCategorySeriesPriv(ca_id, se_id, order);

			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}

	public void deleteSeriesMap(long id){

		ImpLog.logStart(this,"deleteSeriesMap");

		try{
			startTrans();

			mapclient.delete("deleteSeriesMap",id);

			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}


	/**
	 * ���łɓ����Ŋ��蓖�Ă�ꂽ�J���������擾����
	 * @return
	 */
	public String getColumnByName(String name){

		String cname = null;

		try{

			cname = (String)mapclient.queryForObject("getColumnByName",name);

		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}

		return cname;
	}


	/**
	 * item���������A���ʂ��܂�series�����Ɏ擾���܂�
	 * @param cname
	 * @param keywords
	 * @param limit
	 * @param offset
	 * @return
	 * @deprecated
	 */
	public ImpSearchResult<ImpSeries> searchSeries(Boolean se_active,int parent_ca_id,String cname,List<String> keywords,String lang,int limit,int offset){

		ImpLog.logStart(this,"searchSeries");
		/*
		Map<String,Object> params = new HashMap<String, Object>();
		params.put("se_id", se_id);
		params.put("cname", cname);
		params.put("keywords", keywords);
		*/

		ImpSearchResult<ImpSeries> s ;

		if(cname!=null&&!cname.matches("^c\\d{1,2}$")){
				throw new DataAccessException();
		}
		/*
		if(order!=null&&!order.matches("^(c\\d{1,2}$)|(it_id)|(se_id)")){
			throw new DataAccessException();
		}*/



		ImpSearchCondition cond = new ImpSearchCondition();
		cond.setKeywords(keywords);
		cond.setCname(cname);
		cond.setCa_id(parent_ca_id);
		cond.setLang(lang);
		cond.setActive(se_active);

		//cond.setOrder(order);


		List<ImpSeries> list = new ArrayList<ImpSeries>();
		try{

			//�����擾
			list = (List<ImpSeries>)mapclient.queryForList("searchSeries",cond);
			int max = list.size();

			cond.setLimit(limit);
			cond.setOffset(offset);

			list = (List<ImpSeries>)mapclient.queryForList("searchSeries",cond);

			for(ImpSeries se:list){
				List<ImpSeriesMap> maplist = (List<ImpSeriesMap>)mapclient.queryForList("listSeriesMap",se.getId());
				for(ImpSeriesMap map:maplist){
					se.addSeriesMap(map);
				}
			}

			s = new ImpSearchResult<ImpSeries>(max, limit, offset);
			s.setResult(list);


		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}


		return s;

	}

	//foradmin
	public List<ImpSeries> listSeriesByCategory(int ca_id,Boolean active){

		//Log.logStart(this,"listSeriesByCategory");
		ImpSearchCondition cond = new ImpSearchCondition();
		cond.setCa_id(ca_id);
		cond.setActive(active);

		List<ImpSeries> list = new ArrayList<ImpSeries>();
		try{


			list = (List<ImpSeries>)mapclient.queryForList("listSeriesByCategory",cond);



		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}


		return list;


	}

	//foradmin
	public List<ImpSeries> listSeriesByParentCategory(int parent_ca_id){

		ImpLog.logStart(this,"listSeriesByParentCategory");
		ImpSearchCondition cond = new ImpSearchCondition();
		cond.setCa_id(parent_ca_id);


		List<ImpSeries> list = new ArrayList<ImpSeries>();
		try{


			list = (List<ImpSeries>)mapclient.queryForList("listSeriesByParentCategory",cond);



		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}


		return list;


	}
	//�����J�^���O�p�L�[���[�h,ABC����
	public ImpSearchResult<ImpSeries> searchWebcatalog(Boolean seactive,ImpSearchMode mode,List<Integer> ca_ids,List<String> keywords,String lang,int limit,int offset,int show){

		return searchWebcatalog(seactive,mode,ca_ids,keywords,lang,limit,offset,show,true);

	}

	//�����J�^���O�p�L�[���[�h,ABC����
	public ImpSearchResult<ImpSeries> searchWebcatalog(Boolean seactive,ImpSearchMode mode,List<Integer> ca_ids,List<String> keywords,String lang,int limit,int offset,int show,boolean getItems){

		ImpLog.logStart(this,"searchWebcatalog");
		ImpSearchCondition cond = new ImpSearchCondition();
		cond.setMode(mode.toString());
		cond.setCa_ids(ca_ids);
		cond.setKeywords(keywords);
		cond.setLang(lang);
		cond.setActive(seactive);

		List<ImpSeries> result = new ArrayList<ImpSeries>();
		ImpSearchResult<ImpSeries> s = null;

		ImpItemDao itdao = new ImpItemDao(this.status);

		try{

			//����
			result = (List<ImpSeries>)mapclient.queryForList("searchWebcatalog",cond);
			int max = result.size();

			cond.setLimit(limit);
			cond.setOffset(offset);

			result = (List<ImpSeries>)mapclient.queryForList("searchWebcatalog",cond);

			if(getItems){
				for(ImpSeries se:result){


					//get seriesmap
					List<ImpSeriesMap> maplist = (List<ImpSeriesMap>)mapclient.queryForList("listSeriesMap",se.getId());
					for(ImpSeriesMap map:maplist){
						se.addSeriesMap(map);
					}
					//set item
					List<ImpDbItem> _list = itdao.listBySeries_id(se.getId(),seactive);
					List<ImpItem> itemlist = new LinkedList<ImpItem>();
					for(ImpDbItem dbitem:_list){
						ImpItem i = ImpItemUtil.createItem(se, dbitem);
						itemlist.add(i);
					}
					se.setItems(itemlist);

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

	/**
	 * ImpSeries �� ca_id�Ō���
	 * @param cname
	 * @param keywords
	 * @param limit
	 * @param offset
	 * @return
	 */
	public ImpSearchResult<ImpSeries> searchSeriesByCategory(Boolean se_active, int ca_id,int limit,int offset){

		ImpLog.logStart(this,"searchSeriesByCateogry");
		/*
		Map<String,Object> params = new HashMap<String, Object>();
		params.put("se_id", se_id);
		params.put("cname", cname);
		params.put("keywords", keywords);
		*/

		ImpSearchResult<ImpSeries> s ;


		ImpSearchCondition cond = new ImpSearchCondition();
		cond.setCa_id(ca_id);
		cond.setActive(se_active);
		//cond.setOrder(order);


		List<ImpSeries> list = new ArrayList<ImpSeries>();
		try{

			//�����擾
			list = (List<ImpSeries>)mapclient.queryForList("searchSeriesByCategory",cond);
			int max = list.size();

			cond.setLimit(limit);
			cond.setOffset(offset);

			list = (List<ImpSeries>)mapclient.queryForList("searchSeriesByCategory",cond);

			for(ImpSeries se:list){
				List<ImpSeriesMap> maplist = (List<ImpSeriesMap>)mapclient.queryForList("listSeriesMap",se.getId());
				for(ImpSeriesMap map:maplist){
					se.addSeriesMap(map);
				}
			}

			s = new ImpSearchResult<ImpSeries>(max, limit, offset);
			s.setResult(list);


		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}


		return s;

	}



	/**seid��List�Ŏ擾
	 * �Z�b�g�����J�e�S���͕s��!!!
	 * (�{�������̂��̂�1�Ɍ��肵�Ă��邽��)
	 *
	 **/
	public List<ImpSeries> listSeriesBySeids(List<Long> se_ids){
		return listSeriesBySeidsAndCategoryLabel(se_ids, null);
	}

	/**seid��List�Ŏ擾
	 * �Z�b�g�����J�e�S����category_prefix(���K�\��)�Ŏw��
	 * WEB�J�^���O�̏ꍇ="^�y����"���t���Ă���J�e�S����1�����Ȃ�
	 * (�{�������̂��̂�1�Ɍ��肵�Ă��邽��)
	 *
	 **/
	public List<ImpSeries> listSeriesBySeidsAndCategoryLabel(List<Long> se_ids,String category_prefix){

		ImpLog.logStart(this,"listSeriesBySeids");

		ImpSearchCondition cond = new ImpSearchCondition();
		cond.setSe_ids(se_ids);
		cond.setCategory_prefix(category_prefix);


		List<ImpSeries> list = new ArrayList<ImpSeries>();
		try{

			list = (List<ImpSeries>)mapclient.queryForList("listSeriesBySeids",cond);

			for(ImpSeries se:list){
				List<ImpSeriesMap> maplist = (List<ImpSeriesMap>)mapclient.queryForList("listSeriesMap",se.getId());
				for(ImpSeriesMap map:maplist){
					se.addSeriesMap(map);
				}
			}



		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}


		return list;

	}

}

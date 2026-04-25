package com.smc.util;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;

import com.smc.webcatalog.model.ImpDbItem;
import com.smc.webcatalog.model.ImpItem;
import com.smc.webcatalog.model.ImpSeries;
import com.smc.webcatalog.model.ImpSeriesMap;

public class ImpItemUtil {

	public static ImpDbItem createImpDbImpItem(ImpItem item){

		ImpDbItem dbitem= new ImpDbItem();
		try{
			PropertyUtils.copyProperties(dbitem, item);//�����v���p�e�B�[���R�s�[

			for(ImpSeriesMap map:item.getSeriesmap().values()){
				PropertyUtils.setProperty(dbitem, map.getCname(), map.getValue());
			}
		}catch(Exception ex){
			ImpLog.logEx(ex);
		}

		return dbitem;
	}


	//�V���[�Y���ړ�
	public static void moveSeries(long item_id,long dst_se_id){


/*		ImpItemDao idao = ImpDaoFactory.createItemDao();
		ImpDbItem i = idao.get(item_id);

		long src_se_id = i.getSe_id();
		i.setSe_id(dst_se_id);
		i.setOrder(Integer.MAX_VALUE);
		idao.update(i);

		//���Ƃ̃V���[�Y��order��U��Ȃ���
		List<ImpDbItem> srclist = idao.listBySeries_id(src_se_id,null);
		idao.renumberOrder(srclist);

		List<ImpDbItem> dstlist = idao.listBySeries_id(dst_se_id,null);
		idao.renumberOrder(dstlist);

*/

	}

	//dbitem�̃J������seriesmap��values�ɃZ�b�g
	public static ImpSeries setImpSeriesMapValues(ImpSeries se,ImpDbItem item){

		ImpSeries _se = new ImpSeries();

		try{

			for(ImpSeriesMap map:se.getSeriesmap().values()){
				ImpSeriesMap _map = (ImpSeriesMap)BeanUtils.cloneBean(map);
				_map.setValue((String)(PropertyUtils.getProperty(item, map.getCname())));
				_map.setItem_id(item.getId());
				_se.addSeriesMap(_map);
			}
		}catch(Exception ex){
			ImpLog.logEx(ex);
		}

		return _se;

	}

	//dbitem�̃J������seriesmap��values�ɃZ�b�g
	public static ImpItem createItem(ImpSeries se,ImpDbItem dbitem){

		ImpItem _item = new ImpItem();

		try{
			PropertyUtils.copyProperties(_item, dbitem);
			_item.setSe_id(se.getId());//se_id�͈قȂ�
			_item.setSe_name(se.getName());


			for(ImpSeriesMap map:se.getSeriesmap().values()){
				ImpSeriesMap _map = (ImpSeriesMap)BeanUtils.cloneBean(map);
				_map.setValue((String)(PropertyUtils.getProperty(dbitem, map.getCname())));
				_map.setItem_id(dbitem.getId());
				_item.addSeriesMap(_map);
			}
		}catch(Exception ex){
			ImpLog.logEx(ex);
		}

		return _item;

	}



}

package com.smc.webcatalog.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smc.webcatalog.dao.ImpDaoFactory;
import com.smc.webcatalog.dao.ImpItemDao;
import com.smc.webcatalog.dao.ImpSeriesDao;
import com.smc.webcatalog.model.ImpDbItem;
import com.smc.webcatalog.model.ImpSeries;

@Service
@Transactional
public class ImpSeriesService {

/*	private static ImpSeriesDao sedao = ImpDaoFactory.createSeriesDao();

	private static ImpItemDao itdao = ImpDaoFactory.createItemDao(); //

	public List<ImpSeries> list(int ca_id)
	{
		List<ImpSeries> list = sedao.listSeriesByCategory(ca_id,null);
		return  list;
	}

	public ImpSeries getWithSeriesMapItems(long se_id)
	{
		return sedao.get(se_id, true);
	}

	public List<ImpDbItem> listItem(long se_id)
	{
		List<ImpDbItem> list = itdao.listBySeries_id(se_id);
		return list;
	}
	*/
}

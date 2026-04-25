package com.smc.webcatalog.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ImpCategoryService {

//	private static ImpCategoryDao cadao = ImpDaoFactory.createCategoryDao();
//	private static ImpSeriesDao sedao = ImpDaoFactory.createSeriesDao();
//	private static ImpItemDao itdao = ImpDaoFactory.createItemDao();

/*	public List<ImpCategory> getCategory()
	{
		return list();
	}

	public static List<ImpCategory> list(){

		List<ImpCategory> tmp = cadao.list(0, null);
		List<ImpCategory> root = new LinkedList<ImpCategory>();

		for(ImpCategory ca:tmp){
			if(ca.getName().matches("^【総合.*$")){

				listChildCategory( ca, ImpItemType.SERIES, 0);
				root.add(ca);
			}
		}


		return root;

	}

	public static List<ImpCategory> listByLang(String lang){

		List<ImpCategory> tmp = cadao.list(0, null);
		List<ImpCategory> root = new LinkedList<ImpCategory>();

		for(ImpCategory ca:tmp){
			if(ca.getName().matches("^【総合.*$")){

				listChildCategory( ca, ImpItemType.SERIES, 0);
				root.add(ca);
			}
		}


		return root;

	}



	private static void listChildCategory(ImpCategory ca, ImpItemType type,int level){

		level ++ ;
		//Log.log(level+"--"+ca.getName()+ca.getId());



		ca.setItemsize(sedao.listSeriesByCategory(ca.getId(),null).size());
		ca.setChildren(cadao.list(ca.getId(),ca.getType()));

		for(ImpCategory _ca:ca.getChildren()){
			listChildCategory( _ca, type, level);
		}


	}*/
}

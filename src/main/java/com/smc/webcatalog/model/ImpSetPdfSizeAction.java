/*
 * Author Tsutomu Miyashita( WhiteBaseSystems,Limited )
 * Created on 2002/12
 */


package com.smc.webcatalog.model;

import java.io.File;
import java.text.DecimalFormat;




public class ImpSetPdfSizeAction  {



	//デジカタPDFパス変換
    public static String getPdfPath(String catalog_path){
    	String s = "";

    	if(catalog_path!=null){

    		if(catalog_path.indexOf("/mpv/")!=-1){

				s = catalog_path.replaceFirst("(.*)\\/mpv\\/([^/]+)\\/.*", "$1/mpv/$2/data/$2.pdf");

    		}else{

    			s = catalog_path.replaceFirst("/", "/pdf/");
    			s = s.replaceFirst(".htm.*$", ".pdf");
    		}
    	}

    	return s;
    }

    //newbest用パス変換
    public static String getPdfPath_NEWBEST(String catalog_path){
    	String s = "";

    	if(catalog_path!=null&&catalog_path.startsWith("/catalog/")){

    		if(catalog_path.indexOf(".htm?back=0")!=-1){

    			s = catalog_path.replaceFirst("/", "/pdf/");
    			s = s.replaceFirst(".htm.*$", ".pdf");

    		}else if(catalog_path.indexOf("/mpv/")!=-1){

				s = catalog_path.replaceFirst("(.*)\\/mpv\\/([^/]+)\\/.*", "$1/mpv/$2/data/$2.pdf");


    		}else if(catalog_path.indexOf("/pageview.html")!=-1){

				s = catalog_path.replaceFirst("^(.*)/([^/]+)/pageview\\.html$", "$1/$2/data/$2.pdf");

			//2018-04-17�ǉ�
    		}else if(catalog_path.indexOf("/index.html")!=-1){

    		    s = catalog_path.replaceFirst("^(.*)/([^/]+)/index\\.html$", "$1/$2/data/$2.pdf");

    		}

    	}else{

    	    s = catalog_path;

    	}




    	return s;
    }


	public static String getPdfSize(String BASEDIR,String catalog_path){

		String s  = "";

		String pdfpath = getPdfPath_NEWBEST(catalog_path);

		if(pdfpath!=null){

			File pdf = new File(BASEDIR+pdfpath);

			if(pdf.exists()){
				s = getSizeString(pdf.length());
			}else{
				s = "0";
			}

		}

		return s;
	}

	public static String getSizeString(long size){

		String suffix = "KB";

		DecimalFormat df = new DecimalFormat("#,##0");

		double s = (double)size / getRatio("kilo");

		if(s>1024){
			df = new DecimalFormat("#,##0.#");
			s = (double)size / getRatio("mega");
			suffix = "MB";
		}

		return df.format(s)+suffix;
	}

	public static int getRatio(String prefix) {
		int ratio = 0;
		if(prefix.equals("kilo")) {
			ratio = 1024;
		} else if(prefix.equals("mega")) {
			ratio = 1024 * 1024;
		} else if(prefix.equals("giga")) {
			ratio = 1024 * 1024 * 1024;
		} else if(prefix.equals("tera")) {
			ratio = 1024 * 1024 * 1024 * 1024;
		}
		return ratio;
	}

}//end Class


package com.smc.util;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;



public class ImpLog {

	public static void log(String s){

		 System.out.println(s);

	}

	public static void logEx(Throwable ex){

		 System.out.println(ex.getClass().getName()+"/"+ex.getMessage());
		 ex.printStackTrace(System.out);


	}

	public static void logStart(Object o,String s){

		 System.out.println(o.getClass().getName()+"#"+s);

	}

	public static void logReflec(Object o){

		logReflec(o,false);
	}

	public static void logReflec(Object o,boolean format){

		String s = ReflectionToStringBuilder.toString(o);
		if(format){
			s = s.replaceAll("\\[", "****start***\n");
			s = s.replaceAll("\\]", "****end***\n");
			s = s.replaceAll(",", "\n");
		}
		System.out.println(s);

	}

}
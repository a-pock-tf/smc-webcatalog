package com.smc.webcatalog.model;


public enum ImpCtype {

	TEXT("文字列"),
	TEXT_LONG("長い文字列"),
	DOUBLE("数値"),

	;
	private String name;

	public String getName(){
		return super.name();
	}

	public String getValue() {
	    return name;
    }

	private ImpCtype(String name){
		this.name = name;
	}

	public static ImpCtype getName(String s){

		for(ImpCtype type:ImpCtype.values()){
			if(s!=null&&s.equals(type.getValue())){
				return type;
			}
		}
		return ImpCtype.TEXT;
	}




}

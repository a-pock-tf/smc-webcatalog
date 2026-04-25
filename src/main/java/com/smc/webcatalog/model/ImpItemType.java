package com.smc.webcatalog.model;


public enum ImpItemType {

	ITEM("製品"),
	SERIES("シリーズ"),
	FAQ("FAQ"),
	GROSSARY("用語集"),
	WEBCAT("WEBカタログ"),
	SPEC("特設サイト")
	;
	private String name;

	public String getName(){
		return super.name();
	}

	public String getValue() {
	    return name;
    }

	private ImpItemType(String name){
		this.name = name;
	}

	public static ImpItemType getName(String s){

		for(ImpItemType type:ImpItemType.values()){
			if(s!=null&&s.equals(type.getValue())){
				return type;
			}
		}
		return ImpItemType.ITEM;
	}




}

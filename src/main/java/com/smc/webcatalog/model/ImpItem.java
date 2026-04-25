package com.smc.webcatalog.model;

public class ImpItem extends ImpSeries {

	private long se_id;
	private String se_name;




	private String name;
	/*	private String name2;
	private String txt;
	private String txt2;
*/

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/*
	public String getName2() {
		return name2;
	}

	public void setName2(String name2) {
		this.name2 = name2;
	}

	public String getTxt() {
		return txt;
	}

	public void setTxt(String txt) {
		this.txt = txt;
	}

	public String getTxt2() {
		return txt2;
	}

	public void setTxt2(String txt2) {
		this.txt2 = txt2;
	}
*/
	public long getSe_id() {
		return se_id;
	}

	public void setSe_id(long se_id) {
		this.se_id = se_id;
	}

	public String getSe_name() {
		return se_name;
	}

	public void setSe_name(String se_name) {
		this.se_name = se_name;
	}


}

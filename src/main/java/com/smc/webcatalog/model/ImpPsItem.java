package com.smc.webcatalog.model;


public class ImpPsItem extends ImpAbstractEntity {

	private int num;

	private String flg;
	private String kan;
	private String page;
	private String c1;
	private int c1id;
	private String c2;
	private int c2id;
	private String series;
	private String item;
	private String item_regexp;
	private String name;
	private String sid;
	private long se_id;
	private String query;

	public String getFlg() {
		return flg;
	}
	public void setFlg(String flg) {
		this.flg = flg;
	}
	public String getKan() {
		return kan;
	}
	public void setKan(String kan) {
		this.kan = kan;
	}
	public String getPage() {
		return page;
	}
	public void setPage(String page) {
		this.page = page;
	}
	public String getC1() {
		return c1;
	}
	public void setC1(String c1) {
		this.c1 = c1;
	}
	public int getC1id() {
		return c1id;
	}
	public void setC1id(int c1id) {
		this.c1id = c1id;
	}
	public String getC2() {
		return c2;
	}
	public void setC2(String c2) {
		this.c2 = c2;
	}
	public int getC2id() {
		return c2id;
	}
	public void setC2id(int c2id) {
		this.c2id = c2id;
	}
	public String getSeries() {
		return series;
	}
	public void setSeries(String series) {
		this.series = series;
	}
	public String getItem() {
		return item;
	}
	public void setItem(String item) {
		this.item = item;
	}
	public String getItem_regexp() {
		return item_regexp;
	}
	public void setItem_regexp(String item_regexp) {
		this.item_regexp = item_regexp;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSid() {
		return sid;
	}
	public void setSid(String sid) {
		this.sid = sid;
	}
	public int getNum() {
		return num;
	}
	public void setNum(int num) {
		this.num = num;
	}
	public long getSe_id() {
		return se_id;
	}
	public void setSe_id(long se_id) {
		this.se_id = se_id;
	}
    public String getQuery() {
        return query;
    }
    public void setQuery(String query) {
        this.query = query;
    }


}

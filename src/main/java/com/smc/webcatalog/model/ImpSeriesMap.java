package com.smc.webcatalog.model;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ImpSeriesMap extends ImpAbstractEntity implements Serializable{

	public static int COL_MAX=30;

	private long se_id;

	private int order;
	private String name;
	private String cname;
	private long item_id;
	private String value="";

	private int rowspan;
	private int colspan;

	private Pattern p_row = Pattern.compile("\\[��(\\d+)\\]");
	private Pattern p_col = Pattern.compile("\\[��(\\d+)\\]");


	private ImpCtype type = ImpCtype.TEXT;


	public String getValue_html(){

		String s = "";

		if(getValue()!=null){
			s = getValue().replace("\r\n", "<br/>");
		}

		return s;
	}


	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCname() {
		return cname;
	}
	public void setCname(String cname) {
		this.cname = cname;
	}


	public String getValue() {
		return value;
	}
	public void setValue(String value) {

		//value =detectRowspan(value);

		this.value = value;

	}
	public long getItem_id() {
		return item_id;
	}
	public void setItem_id(long item_id) {
		this.item_id = item_id;
	}
	public ImpCtype getType() {
		return type;
	}
	public void setType(ImpCtype type) {
		this.type = type;
	}
	public long getSe_id() {
		return se_id;
	}
	public void setSe_id(long se_id) {
		this.se_id = se_id;
	}

	private String detectRowspan(String s){

		int _rowspan = 0;
		if(s!=null){
			Matcher m = p_row.matcher(s);
			if(m.find()){
				int g = m.groupCount();
				if(g>0){
					_rowspan = Integer.parseInt((m.group(1)));
					//s = s.replaceAll(m.group(0),"");
					s = m.replaceAll("");
				}
			}
		}
		if(_rowspan>0) this.rowspan = _rowspan;

		return s;
	}


	public int getRowspan() {
		return rowspan;
	}


	public void setRowspan(int rowspan) {
		this.rowspan = rowspan;
	}


	public int getColspan() {
		return colspan;
	}


	public void setColspan(int colspan) {
		this.colspan = colspan;
	}
}

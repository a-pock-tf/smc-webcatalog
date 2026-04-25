package com.smc.webcatalog.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ImpSearchCondition {

	private long id;
	private long se_id;
	private List<String> keywords  = new ArrayList<String>();
	private List<String> keywords_opt  = new ArrayList<String>();
	private String cname;
	private String order;
	private int limit;
	private int offset;
	private int ca_id;
	private String s_ca_id;
	private int parent_ca_id;
	private List<Integer> parent_ca_ids  = new ArrayList<Integer>();
	private List<Long> se_ids = new ArrayList<Long>();

	private int src_ca_id;
	private int dst_ca_id;

	private String yomi;
	private String groupid;

	private Date start;
	private Date end;

	private String type;

	private String ken;

	private Boolean ontop;
	private Boolean active;
	private Boolean admin;

	private String mode;

	private String lang = "ja";

	private List<Integer> ca_ids  = new ArrayList<Integer>();

	private String ok;

	private String category_prefix;

	private String colname;

	private Boolean bool = false;


	public long getSe_id() {
		return se_id;
	}
	public void setSe_id(long se_id) {
		this.se_id = se_id;
	}

	public String getCname() {

		if(cname!=null&&!cname.matches("^c\\d{1,2}$")){
			cname = null;
		}

		return cname;
	}
	public void setCname(String cname) {
		this.cname = cname;
	}
	public List<String> getKeywords() {
		return keywords;
	}
	public void setKeywords(List<String> keywords) {
		this.keywords = keywords;
	}
	public int getLimit() {
		return limit;
	}
	public void setLimit(int limit) {
		this.limit = limit;
	}
	public int getOffset() {
		return offset;
	}
	public void setOffset(int offset) {
		this.offset = offset;
	}
	public String getOrder() {

		if(order!=null&&!order.matches("^(c\\d{1,2})|(it_id)|(se_id)$")){
			order = null;
		}

		return order;
	}
	public void setOrder(String order) {
		this.order = order;
	}
	public int getCa_id() {
		return ca_id;
	}
	public void setCa_id(int ca_id) {
		this.ca_id = ca_id;
	}
	public int getSrc_ca_id() {
		return src_ca_id;
	}
	public void setSrc_ca_id(int src_ca_id) {
		this.src_ca_id = src_ca_id;
	}
	public int getDst_ca_id() {
		return dst_ca_id;
	}
	public void setDst_ca_id(int dst_ca_id) {
		this.dst_ca_id = dst_ca_id;
	}
	public String getYomi() {
		return yomi;
	}
	public void setYomi(String yomi) {
		this.yomi = yomi;
	}
	public String getGroupid() {
		return groupid;
	}
	public void setGroupid(String groupid) {
		this.groupid = groupid;
	}

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Date getStart() {
		return start;
	}
	public void setStart(Date start) {
		this.start = start;
	}
	public Date getEnd() {
		return end;
	}
	public void setEnd(Date end) {
		this.end = end;
	}
	public String getKen() {
		return ken;
	}
	public void setKen(String ken) {
		this.ken = ken;
	}
	public Boolean getOntop() {
		return ontop;
	}
	public void setOntop(Boolean ontop) {
		this.ontop = ontop;
	}
	public Boolean getActive() {
		return active;
	}
	public void setActive(Boolean active) {
		this.active = active;
	}
	public Boolean getAdmin() {
		return admin;
	}
	public void setAdmin(Boolean admin) {
		this.admin = admin;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getLang() {
		return lang;
	}
	public void setLang(String lang) {
		this.lang = lang;
	}
	public int getParent_ca_id() {
		return parent_ca_id;
	}
	public void setParent_ca_id(int parent_ca_id) {
		this.parent_ca_id = parent_ca_id;
	}
	public List<Integer> getParent_ca_ids() {
		return parent_ca_ids;
	}
	public void setParent_ca_ids(List<Integer> parent_ca_ids) {
		this.parent_ca_ids = parent_ca_ids;
	}
	public List<Long> getSe_ids() {
		return se_ids;
	}
	public void setSe_ids(List<Long> se_ids) {
		this.se_ids = se_ids;
	}
	public List<Integer> getCa_ids() {
		return ca_ids;
	}
	public void setCa_ids(List<Integer> ca_ids) {
		this.ca_ids = ca_ids;
	}
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
	public String getS_ca_id() {
		return s_ca_id;
	}
	public void setS_ca_id(String s_ca_id) {
		this.s_ca_id = s_ca_id;
	}
	public List<String> getKeywords_opt() {
		return keywords_opt;
	}
	public void setKeywords_opt(List<String> keywords_opt) {
		this.keywords_opt = keywords_opt;
	}
	public String getOk() {
		return ok;
	}
	public void setOk(String ok) {
		this.ok = ok;
	}
	public String getCategory_prefix() {
		return category_prefix;
	}
	public void setCategory_prefix(String category_prefix) {
		this.category_prefix = category_prefix;
	}
    public String getColname() {
        return colname;
    }
    public void setColname(String colname) {
        this.colname = colname;
    }
    public Boolean getBool() {
        return bool;
    }
    public void setBool(Boolean bool) {
        this.bool = bool;
    }




}

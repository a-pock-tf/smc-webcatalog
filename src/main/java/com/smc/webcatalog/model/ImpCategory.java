package com.smc.webcatalog.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

public class ImpCategory implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 6264545050183957332L;


	private int id;
	private int parent_id;
	private int parent_order;

	private long cs_id;
	private int cs_order;

	private ImpItemType type = ImpItemType.FAQ;

	private String lang = "ja";
	private int order = 1;
	private String name;
	private boolean active=true;
	private Timestamp ctime = new Timestamp(System.currentTimeMillis());
	private Timestamp mtime = new Timestamp(System.currentTimeMillis());
	private String by;

	private LinkedList<ImpCategory> parents = new LinkedList<ImpCategory>();
	private LinkedList<ImpCategory> children = new LinkedList<ImpCategory>();


	private int itemsize=0;

	private String id_new;

	// TODO 総合Tならテーマのサブカテゴリ？種別？DisplayType?が必要
	public String getName_html(){

		String s = "";

		if(name!=null){
			s = name.replaceAll("【.*】", "");
		}

		return s;
	}

	public CategoryDisplayType getDisplayType()
	{
		CategoryDisplayType sub = CategoryDisplayType.GENERAL;
		if (name.isEmpty() == false && name.contains("【総合T】"));
		{
			sub = CategoryDisplayType.SUBJECT;
		}
		return sub;
	}

	public ImpCategory(){

	}

	public ImpCategory(ImpItemType type){
		this.type = type;
	}

	// for Import
	public Category getCategory(String parent, String langRef, String stateRef, String lang, CategoryType ct, ModelState ms, User u, int order)
	{
		Category c = new Category();
		if (parent != null && !parent.isEmpty()) c.setParentId(parent);
		if (langRef != null && !langRef.isEmpty()) c.setLangRefId(langRef);
		if (stateRef != null && !stateRef.isEmpty()) c.setStateRefId(stateRef);
		c.setOldId(String.valueOf(id));
		c.setName(getName_html());
		c.setSlug("");
		c.setLang(lang);
		c.setType(ct);
		c.setDisplayType(getDisplayType());
		c.setState(ms);
		c.setCtime(getCtime());
		c.setMtime(getCtime());
		c.setUser(u);
		c.setOrder(order);
		c.setActive(active);
		return c;
	}



	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public int getParent_id() {
		return parent_id;
	}


	public void setParent_id(int parent_id) {
		this.parent_id = parent_id;
	}


	public ImpItemType getType() {
		return type;
	}


	public void setType(ImpItemType type) {
		this.type = type;
	}


	public String getLang() {
		return lang;
	}


	public void setLang(String lang) {
		this.lang = lang;
	}


	public int getOrder() {
		return order;
	}


	public void setOrder(int order) {
		this.order = order;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public boolean getActive() {
		return active;
	}


	public void setActive(boolean active) {
		this.active = active;
	}


	public Timestamp getCtime() {
		return ctime;
	}


	public void setCtime(Timestamp ctime) {
		this.ctime = ctime;
	}


	public Timestamp getMtime() {
		return mtime;
	}


	public void setMtime(Timestamp mtime) {
		this.mtime = mtime;
	}


	public String getBy() {
		return by;
	}


	public void setBy(String by) {
		this.by = by;
	}

	public LinkedList<ImpCategory> getParents() {
		return parents;
	}

	public void setParents(LinkedList<ImpCategory> parents) {
		this.parents = parents;
	}

	public LinkedList<ImpCategory> getChildren() {
		return children;
	}

	public void setChildren(LinkedList<ImpCategory> children) {
		this.children = children;
	}

	public void setChildren(List<ImpCategory> _children){

		this.children.addAll(_children);
	}



	public int getItemsize() {
		return itemsize;
	}

	public void setItemsize(int itemsize) {
		this.itemsize = itemsize;
	}

	public long getCs_id() {
		return cs_id;
	}

	public void setCs_id(long cs_id) {
		this.cs_id = cs_id;
	}

	public int getCs_order() {
		return cs_order;
	}

	public void setCs_order(int cs_order) {
		this.cs_order = cs_order;
	}

	public int getParent_order() {
		return parent_order;
	}

	public void setParent_order(int parent_order) {
		this.parent_order = parent_order;
	}

    public String getId_new() {
        return id_new;
    }

    public void setId_new(String id_new) {
        this.id_new = id_new;
    }

}

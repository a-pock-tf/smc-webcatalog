package com.smc.webcatalog.model;

public class ImpCatpan {

	private int id;
	private String name;

	public ImpCatpan(){}

	public ImpCatpan(ImpCategory ca){

		setId(ca.getId());
		setName(ca.getName());

	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}


}

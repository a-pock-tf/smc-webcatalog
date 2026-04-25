package com.smc.webcatalog.model;

import java.util.LinkedList;
import java.util.List;

public class MyErrorsImpl implements MyErrors{


	List<ErrorObject> _list;

	public MyErrorsImpl() {
		_list = new LinkedList<ErrorObject>();
	}

	@Override
	public void addError(ErrorObject error) {
		_list.add(error);
	}

	@Override
	public void setErrrors(List<ErrorObject> errors) {
		_list.addAll(errors);
	}

	@Override
	public List<ErrorObject> getErrors() {
		return _list;
	}

	@Override
	public boolean hasError() {
		return (_list.size() > 0);
	}

	@Override
	public int getCount() {
		return _list.size();
	}

	public String getAllMessages() {
		String ret = "";
		for(ErrorObject e : _list) {
			ret += e.getCode() + ":" + e.getMessage() + "\r\n";
		}

		return ret;
	}


}

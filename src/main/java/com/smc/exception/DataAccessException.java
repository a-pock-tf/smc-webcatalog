
package com.smc.exception;


@SuppressWarnings("serial")
public class DataAccessException extends RuntimeException {

	private String msg;

	public DataAccessException(){
	}
	public DataAccessException(String msg){
		this.msg = msg;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}



}

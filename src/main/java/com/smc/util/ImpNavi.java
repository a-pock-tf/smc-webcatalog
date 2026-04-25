/*
 * Result.java
 *
 * Created on 2004/01/13, 19:31
 */

package com.smc.util;


/**
 *
 * @author  miyasit
 * @version 
 */
public class ImpNavi {
	
	private String pageno;
	private boolean isCurrent=false;
	private int off;
	private int limit;
	private boolean isLast=false;
	private boolean hide = false;
	
	public boolean getHide() {
		return hide;
	}

	public void setHide(boolean hide) {
		this.hide = hide;
	}

	/**
	 * @return
	 */
	public boolean getIsCurrent() {
		return isCurrent;
	}

	/**
	 * @return
	 */
	public int getLimit() {
		return limit;
	}

	/**
	 * @return
	 */
	public int getOff() {
		return off;
	}

	/**
	 * @return
	 */
	public String getPageno() {
		return pageno;
	}

	/**
	 * @param b
	 */
	public void setIsCurrent(boolean b) {
		isCurrent = b;
	}

	/**
	 * @param i
	 */
	public void setLimit(int i) {
		limit = i;
	}

	/**
	 * @param i
	 */
	public void setOff(int i) {
		off = i;
	}

	/**
	 * @param string
	 */
	public void setPageno(String string) {
		pageno = string;
	}

	/**
	 * @return
	 */
	public boolean getIsLast() {
		return isLast;
	}


	/**
	 * @param b
	 */
	public void setIsLast(boolean b) {
		isLast = b;
	}

}

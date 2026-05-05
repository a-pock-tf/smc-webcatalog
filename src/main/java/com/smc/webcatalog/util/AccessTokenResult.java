package com.smc.webcatalog.util;


import java.util.Calendar;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class AccessTokenResult {

	private String access_token;
	private String token_type;
	private String expires_in;

	private Calendar cal_expire;

	private boolean expired;

	public AccessTokenResult() {
		cal_expire = Calendar.getInstance();
	}

	public String getAccess_token() {
		return access_token;
	}

	public void setAccess_token(String access_token) {
		this.access_token = access_token;
	}

	public String getToken_type() {
		return token_type;
	}

	public void setToken_type(String token_type) {
		this.token_type = token_type;
	}

	public String getExpires_in() {
		return expires_in;
	}

	public void setExpires_in(String expires_in) {
		this.expires_in = expires_in;
	}

	public Calendar getCal_expire() {
		return cal_expire;
	}

	public void setCal_expire(Calendar cal_expire) {
		this.cal_expire = cal_expire;
	}

	public boolean isExpired(Calendar cal_now) {
		//Calendar cal_now = Calendar.getInstance();
		try {
			cal_expire.add(Calendar.SECOND, Integer.parseInt(getExpires_in()));

			if (cal_now.after(cal_expire)) {
				this.expired = true;
			} else {
				this.expired = false;
			}

		} catch (Exception ex) {
			log.error("isExpired"+ex.getMessage());
			return true;
		}
		return expired;
	}

	public void setExpired(boolean expired) {
		this.expired = expired;
	}

}

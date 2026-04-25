package com.smc.webcatalog.model;

import com.smc.webcatalog.config.ErrorCode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorObject {

	private ErrorCode code;

	public ErrorObject() {
		code = ErrorCode.E00000;
		count = 0;
		message = null;
	}
	/**
	 * 登録、更新が成功した数
	 */
	private int count;

	private String message;

	/**
	 * エラー確認
	 * @return trueはエラー
	 */
	public boolean isError() {
		return !code.equals(ErrorCode.E00000);
	}

	@Override
	public String toString() {
		String ret = super.toString();
		ret+=" code:"+code + " count=" + count + " message="+ message;
		return ret;
	}
}

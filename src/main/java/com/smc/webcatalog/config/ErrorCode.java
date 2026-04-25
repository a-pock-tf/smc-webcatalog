package com.smc.webcatalog.config;

public enum ErrorCode {

	/**
	 * 正常
	 */
	E00000,
	/**
	 * 該当IDなし
	 */
	E10001,
	/**
	 * すでに存在
	 */
	E10003,
	/**
	 * 条件違反
	 */
	E10005,
	/**
	 *
	 */
	E20001,
	/**
	 * DB接続エラー
	 */
	E50001,
	/**
	 * JAVA例外 null pointerなど
	 */
	E80001,
	/**
	 * 不明なエラー
	 */
	E99999

}

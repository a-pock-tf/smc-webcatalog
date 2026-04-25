package com.smc.webcatalog.model;

import lombok.Getter;
import lombok.Setter;

/**
 * ログイン用フォーム
 * @author miyasit
 *
 */
@Getter
@Setter
public class PasswordForm {

	private String id;
	private String before;
	private String after;
	private String again;
}

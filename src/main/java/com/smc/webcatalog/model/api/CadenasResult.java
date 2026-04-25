package com.smc.webcatalog.model.api;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CadenasResult {

	private String error = ""; // エラーメッセージ。正常時はカラ
	private String result = ""; // create実行後のCADENASからのメッセージ。
	private String email = "";

	// 以下各ログイン用URL
	private String create = "";
	private String update = "";
	private String login = "";
}

package com.smc.webcatalog.model;

import java.util.List;

// TODO Error表現のサンプル(List系でもいいが、わかりやすく)
public interface MyErrors {

	//単一のエラーをset
	void addError(ErrorObject error);

	//複数のエラーをset/get
	void setErrrors(List<ErrorObject> errors);
	List<ErrorObject> getErrors();

	//エラーがないか
	boolean hasError();

	//エラーの数
	int getCount();

	String getAllMessages();

}

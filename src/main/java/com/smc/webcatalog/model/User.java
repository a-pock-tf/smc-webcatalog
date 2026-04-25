package com.smc.webcatalog.model;

import org.hibernate.validator.constraints.UniqueElements;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.util.StringUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/***
 * 管理者としてのUser
 * @author miyasit
 */
@Document(collection = "user")
@Getter
@Setter
@ToString(callSuper = true, includeFieldNames = true)
public class User extends BaseModel {

	@UniqueElements
	String loginId;

	String password;
	String name;
	String category;
	String email;
	String company;
	String address;
	String country;
	boolean admin;

	/**
	 * 編集可能な言語リスト
	 */
	String[] langList;

	public User() {
		setLang("ja-jp");
		admin = false;
	}

	public boolean isEnable()
	{
		return isActive();
	}
	public void setEnable(boolean e) {
		setActive(e);
	}

	/**
	 * 言語が編集可能かどうかを判定
	 * @param lang
	 * @return
	 */
	public boolean isEditableLang(String lang) {
		boolean ret = false;
		if (isAdmin()) {
			ret = true;
		}
		else if (StringUtils.isEmpty(lang) == false && langList != null && langList.length > 0) {
			for(String la : langList) {
				if (lang.equals(la)) {
					ret = true;
					break;
				}
			}
		}
		return ret;
	}

	public boolean isEditableCategory(String cate) {
		boolean ret = false;
		if (isAdmin()) {
			ret = true;
		}
		else if (category == null || StringUtils.isEmpty(category)) {
			ret = true;
		}
		else {
			String[] arr = category.split(",");
			for(String tmp : arr) {
				if (cate != null && cate.equals(tmp)) {
					ret = true;
					break;
				}
			}
		}
		return ret;
	}

}

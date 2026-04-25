package com.smc.webcatalog.model;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

/**
 * User 編集用フォーム
 * @author miyasit
 *
 */
@Getter
@Setter
public class UserForm {

	private String id;
	private String lang;
	private String loginId;
	private String password;
	private String email;
	private String company;
	private String address;
	private String country;
	private String category;
	private boolean admin;
	private String[] langList;
	private boolean active;

	@NotEmpty
	@Size(min = 1, max = 100)
	private String name;
}

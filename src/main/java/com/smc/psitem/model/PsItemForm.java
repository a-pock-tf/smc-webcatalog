package com.smc.psitem.model;

import javax.validation.constraints.NotEmpty;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;

/**
 * Category 編集用フォーム
 * @author miyasit
 *
 */
@Getter
@Setter
public class PsItemForm {
	@NotEmpty
	private String lang;

	private MultipartFile file;

}

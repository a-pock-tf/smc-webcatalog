package com.smc.omlist.model;

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
public class OmlistForm {
	@NotEmpty
	private String lang;

	private MultipartFile file;

}

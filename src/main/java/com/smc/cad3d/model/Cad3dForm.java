package com.smc.cad3d.model;

import javax.validation.constraints.NotEmpty;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;

/**
 * Cad3d 編集用フォーム
 * @author miyasit
 *
 */
@Getter
@Setter
public class Cad3dForm {
	@NotEmpty
	private String lang;

	private MultipartFile file;

}

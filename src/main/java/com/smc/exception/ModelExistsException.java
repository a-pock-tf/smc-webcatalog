package com.smc.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModelExistsException extends RuntimeException {

	public ModelExistsException(String msg) {
		super(msg);
	}

}

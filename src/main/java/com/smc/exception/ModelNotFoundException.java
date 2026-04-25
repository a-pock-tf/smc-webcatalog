package com.smc.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModelNotFoundException extends RuntimeException {

	public ModelNotFoundException(String msg) {
		super(msg);
	}

}

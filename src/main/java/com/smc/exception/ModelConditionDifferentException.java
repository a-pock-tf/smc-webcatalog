package com.smc.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModelConditionDifferentException extends RuntimeException {

	public ModelConditionDifferentException(String msg) {
		super(msg);
	}

}

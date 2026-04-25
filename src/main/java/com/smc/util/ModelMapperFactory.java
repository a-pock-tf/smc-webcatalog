package com.smc.util;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import lombok.val;

public class ModelMapperFactory {

	/**
	 * ModelMapperを返します。
	 *
	 * @return
	 */
	public static ModelMapper create() {
		// ObjectMappingのためのマッパー
		val modelMapper = new ModelMapper();
		val configuration = modelMapper.getConfiguration();

		// 厳格にマッピングする
		configuration.setMatchingStrategy(MatchingStrategies.STRICT);
		return modelMapper;
	}

}

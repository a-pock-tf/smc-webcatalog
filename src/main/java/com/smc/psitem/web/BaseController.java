package com.smc.psitem.web;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * XXX Controllerはこれを継承すること
 * @author miyasit
 *
 */
public class BaseController {

	@Autowired
	protected ApplicationContext applicationContext;

	@Autowired
	protected ModelMapper modelMapper;

}

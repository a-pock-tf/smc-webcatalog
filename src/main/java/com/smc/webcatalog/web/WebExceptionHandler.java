package com.smc.webcatalog.web;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.smc.exception.DataAccessException;
import com.smc.exception.ModelNotFoundException;

import lombok.extern.slf4j.Slf4j;

/***
 * 独自例外処理の実装
 * @author miyasit
 *
 */
@ControllerAdvice
@Slf4j
public class WebExceptionHandler {

	//モデルが見つからなかったとき
	@ExceptionHandler(ModelNotFoundException.class)
	@ResponseStatus(HttpStatus.OK)
	public String resolveException(Exception ex, HttpServletResponse response, Model model) {
		log.error("ModelNotFoundException", ex);

		model.addAttribute("e", ex.getMessage());

		return "error/error1";// error1.htmlへ遷移
	}

	// Fileが見つからなかったとき
	@ExceptionHandler(DataAccessException.class)
	@ResponseStatus(HttpStatus.OK)
	public String resolveDataAccessException(Exception ex, HttpServletResponse response, Model model) {
		log.error("ModelNotFoundException", ex);

		model.addAttribute("e", ex.getMessage());

		return "error/404";// 404.htmlへ遷移
	}
}

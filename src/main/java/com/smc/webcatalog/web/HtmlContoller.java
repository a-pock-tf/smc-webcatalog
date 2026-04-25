package com.smc.webcatalog.web;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerMapping;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class HtmlContoller {

	/**
	 *	特に処理をせず、HTMLにだけアクセスしたいとき
	 */
//	@RequestMapping(value = "**/**.html", method = RequestMethod.GET)
	public String html(HttpServletRequest request) {

		String path = (String) request.getAttribute(
				HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

		AntPathMatcher apm = new AntPathMatcher();
		String finalPath = apm.extractPathWithinPattern(bestMatchPattern, path);

		if (finalPath.endsWith(".html")) {
			finalPath = finalPath.replace(".html", "");
		} else {
			finalPath += "/index.html";
		}
		log.info("finalPath=" + finalPath);

		return finalPath;
	}

}

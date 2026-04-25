package com.smc.webcatalog.util;

import java.util.Locale;

public class LibLocale {
	public static Locale getLocale(String lang) {
		Locale loc = Locale.JAPANESE;
		if (lang.indexOf("en") > -1) loc = Locale.ENGLISH;
		else if (lang.equals("zh-tw")) loc = Locale.TRADITIONAL_CHINESE;
		else if (lang.indexOf("zh") > -1)  loc = Locale.CHINESE;
		return loc;
	}
}

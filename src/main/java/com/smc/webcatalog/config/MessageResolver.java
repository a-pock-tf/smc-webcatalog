package com.smc.webcatalog.config;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

public class MessageResolver implements MessageSource {
	@Autowired
    private MessageSource messageSource;

    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }


    @Override
    public String getMessage(String code, Object[] args,String defaultMessage, Locale locale) {
        return this.getMessage(code,args,"",locale);
    }

    @Override
    public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
        // TODO Auto-generated method stub
        String sMess = messageSource.getMessage(code,args,locale);
        return sMess;
    }

    @Override
    public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
        // TODO Auto-generated method stub
        String sMess = messageSource.getMessage(resolvable,locale);
        return sMess;
    }
}

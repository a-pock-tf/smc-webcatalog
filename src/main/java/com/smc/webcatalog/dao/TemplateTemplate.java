package com.smc.webcatalog.dao;

import java.util.Optional;

import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Template;

/***
 * MongoRepositoryで足りないものはこちらで
 * @author miyasit
 *
 */
public interface TemplateTemplate {

	Optional<Template> findByLangAndModelState(String lang, ModelState m, Boolean active);

}

package com.smc.webcatalog.dao;

import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smc.webcatalog.model.SeriesLinkMaster;

@Repository
@Scope("session")
public interface SeriesLinkMasterRepository  extends MongoRepository<SeriesLinkMaster, String>, SeriesLinkMasterTemplate {
	Optional<SeriesLinkMaster> findByNameAndLang(String name, String lang);

}

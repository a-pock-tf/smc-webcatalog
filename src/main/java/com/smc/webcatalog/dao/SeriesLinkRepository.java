package com.smc.webcatalog.dao;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smc.webcatalog.model.SeriesLink;

@Repository
@Scope("session")
public interface SeriesLinkRepository  extends MongoRepository<SeriesLink, String>, SeriesLinkTemplate {
	List<SeriesLink> findBySeriesId(String seriesId);
}

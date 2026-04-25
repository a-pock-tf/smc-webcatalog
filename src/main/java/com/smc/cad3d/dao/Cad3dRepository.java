package com.smc.cad3d.dao;

import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smc.cad3d.model.Cad3d;


/**
 *  Repositoryでできることはこちらで
 */
@Repository
@Scope("session")
public interface Cad3dRepository extends MongoRepository<Cad3d, String>, Cad3dTemplate {

	public Optional<Cad3d> findById(String id);

	public Optional<Cad3d> findByName(String name);

	public Optional<Cad3d> findByIds(String ids);

	public void deleteAllByLang(String lang);

}

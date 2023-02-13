package com.auth.twofactor.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.auth.twofactor.entity.Properties;

public interface PropertiesRepository extends JpaRepository<Properties, Long>{
	
	Optional<Properties> findByPropName(String propName);
	
}

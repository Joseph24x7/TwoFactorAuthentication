package com.auth.twofactor.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.auth.twofactor.entity.User;

public interface UserRepository extends JpaRepository<User, Long>{
			
	Optional<User> findByUsername(String username);

}

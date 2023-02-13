package com.auth.twofactor.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.auth.twofactor.entity.User;

public interface UserRepository extends JpaRepository<User, Long>{
			
	Optional<User> findByUsername(String username);
	
	@Query("SELECT u FROM User u WHERE u.username = ?1 OR u.email = ?2")
	Optional<User> findByUsernameAndEmail(String username, String email);

}

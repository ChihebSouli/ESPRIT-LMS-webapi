package com.esprit.lms.users.repository;

import com.esprit.lms.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEspritId(String espritId);

    Optional<User> findByEmail(String email);

    boolean existsByEspritId(String espritId);
}

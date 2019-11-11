package com.vaadin.flow.connect.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.vaadin.flow.connect.backend.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmailIgnoreCase(String email);

    Page<User> findBy(Pageable pageable);
}

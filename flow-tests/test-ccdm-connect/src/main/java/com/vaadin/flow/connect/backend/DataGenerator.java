package com.vaadin.flow.connect.backend;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vaadin.flow.connect.backend.entity.Role;
import com.vaadin.flow.connect.backend.entity.User;
import com.vaadin.flow.connect.backend.repository.UserRepository;
import com.vaadin.flow.spring.annotation.SpringComponent;

@SpringComponent
public class DataGenerator {

    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;

    @Autowired
    public DataGenerator(UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void loadData() {
        if (userRepository.count() != 0L) {
            getLogger().info("Using existing database");
            return;
        }

        getLogger().info("Generating demo data");

        getLogger().info("... generating users");
        createBaker(userRepository, passwordEncoder);
        createAdmin(userRepository, passwordEncoder);

        getLogger().info("Generated demo data");
    }

    private User createBaker(UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        return userRepository.save(createUser("user@vaadin.com", "Heidi",
                "Carter", passwordEncoder.encode("baker"), Role.USER));
    }

    private User createAdmin(UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        return userRepository.save(createUser("admin@vaadin.com", "Göran",
                "Rich", passwordEncoder.encode("admin"), Role.ADMIN));
    }

    private User createUser(String email, String firstName, String lastName,
            String passwordHash, String role) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        return user;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(DataGenerator.class);
    }
}

package com.vaadin.flow.connect.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.vaadin.flow.connect.backend.entity.User;
import com.vaadin.flow.connect.backend.repository.UserRepository;

@Service
public class UserService implements CrudService<User> {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserRepository getRepository() {
        return userRepository;
    }

    public Page<User> find(Pageable pageable) {
        return getRepository().findBy(pageable);
    }

    @Override
    public User createNew(User currentUser) {
        return new User();
    }

}

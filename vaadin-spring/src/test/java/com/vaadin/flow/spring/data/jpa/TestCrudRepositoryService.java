package com.vaadin.flow.spring.data.jpa;

import org.springframework.stereotype.Service;

import com.vaadin.flow.spring.data.jpa.CrudRepositoryService;

@Service
public class TestCrudRepositoryService
        extends CrudRepositoryService<TestObject, Integer, TestRepository> {
    TestCrudRepositoryService(TestRepository repository) {
        super(repository);
    }

}

package com.vaadin.flow.spring.data.jpa;

import org.springframework.stereotype.Service;

@Service
public class TestCrudRepositoryService
        extends CrudRepositoryService<TestObject, Integer, TestRepository> {
    TestCrudRepositoryService(TestRepository repository) {
        super(repository);
    }

}

package com.vaadin.flow.spring.data.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TestRepository extends JpaRepository<TestObject, Integer>,
        JpaSpecificationExecutor<TestObject> {

}

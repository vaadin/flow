package com.vaadin.flow.spring.test;

import org.springframework.stereotype.Component;

@Component
public class DummyService {
    public String greet() {
        return "Hello!";
    }
}

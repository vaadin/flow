package com.vaadin.flow.spring.hillasecuritycontextpath;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application
        extends com.vaadin.flow.spring.hillasecurity.Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}

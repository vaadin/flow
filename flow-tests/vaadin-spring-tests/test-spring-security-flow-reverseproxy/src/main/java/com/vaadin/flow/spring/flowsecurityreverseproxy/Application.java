package com.vaadin.flow.spring.flowsecurityreverseproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import com.vaadin.flow.spring.test.Proxy;

@SpringBootApplication
@ComponentScan(basePackageClasses = Proxy.class)
public class Application
        extends com.vaadin.flow.spring.flowsecurity.Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}

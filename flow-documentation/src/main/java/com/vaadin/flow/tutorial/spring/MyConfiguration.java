package com.vaadin.flow.tutorial.spring;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.vaadin.flow.tutorial.annotations.CodeFor;

@Configuration
@ComponentScan
@PropertySource("classpath:application.properties")
@CodeFor("spring/tutorial-spring-configuration.asciidoc")
public class MyConfiguration {

}

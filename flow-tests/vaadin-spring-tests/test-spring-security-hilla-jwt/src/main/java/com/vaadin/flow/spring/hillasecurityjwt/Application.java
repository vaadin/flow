package com.vaadin.flow.spring.hillasecurityjwt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@SpringBootApplication()
@ComponentScan(basePackages = {
        "com.vaadin.flow.spring.hillasecurity" }, excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.vaadin\\.flow\\.spring\\.hillasecurity\\.endpoints\\..*") })
@Import(JwtSecurityUtils.class)
public class Application
        extends com.vaadin.flow.spring.hillasecurity.Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}

package com.vaadin.flow.spring.flowsecurityurlmapping;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class Application
        extends com.vaadin.flow.spring.flowsecurity.Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // Test views use relative path to images, that cannot be correctly resolved
    // where setting vaadin.urlMapping, because view base path differs from
    // web application context path
    // The following filter forwards request from
    // {vaadin.urlMapping}/public/images
    // to /public/images, so they are then served by spring.
    @Bean
    FilterRegistrationBean<?> publicImagesAliasFilter() {
        FilterRegistrationBean<OncePerRequestFilter> registrationBean = new FilterRegistrationBean<>(
                new OncePerRequestFilter() {

                    @Override
                    protected void doFilterInternal(HttpServletRequest request,
                            HttpServletResponse response,
                            FilterChain filterChain)
                            throws ServletException, IOException {
                        request.getRequestDispatcher(
                                request.getRequestURI().substring(7))
                                .forward(request, response);
                    }
                });
        registrationBean.addUrlPatterns("/vaadin/public/images/*",
                "/vaadin/public/profiles/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }
}

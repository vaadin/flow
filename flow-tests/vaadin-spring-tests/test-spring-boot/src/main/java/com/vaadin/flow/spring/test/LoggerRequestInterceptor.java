package com.vaadin.flow.spring.test;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinRequestInterceptor;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

@Component
public class LoggerRequestInterceptor implements VaadinRequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger("RequestInterceptor");

    @Override
    public void requestStart(VaadinRequest request, VaadinResponse response) {
        logger.info("Request start, path into = {}" , request.getPathInfo());
    }

    @Override
    public void handleException(VaadinRequest request, VaadinResponse response, VaadinSession vaadinSession, Exception t) {
    }

    @Override
    public void requestEnd(VaadinRequest request, VaadinResponse response, VaadinSession session) {
        logger.info("Request end, path into = {}" , request.getPathInfo());
    }
}

/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultErrorHandlerTest {

    MockedStatic<LoggerFactory> loggerFactory;
    Logger logger;

    @Before
    public void setUp() throws Exception {
        logger = Mockito
                .spy(LoggerFactory.getLogger(DefaultErrorHandler.class));
        loggerFactory = Mockito.mockStatic(LoggerFactory.class);
        loggerFactory
                .when(() -> LoggerFactory
                        .getLogger(DefaultErrorHandler.class.getName()))
                .thenReturn(logger);
        Mockito.when(logger.isDebugEnabled()).thenReturn(false);
    }

    @After
    public void tearDown() throws Exception {
        loggerFactory.close();
    }

    @Test
    public void error_acceptedException_errorHandled() {
        DefaultErrorHandler errorHandler = Mockito
                .spy(new DefaultErrorHandler(Set.of(IOException.class.getName(),
                        MalformedURLException.class.getName())));

        Throwable throwable = new RuntimeException();
        errorHandler.error(new ErrorEvent(throwable));
        Mockito.verify(logger).error("Unexpected error: {}", null, throwable);

        throwable = new IllegalArgumentException();
        errorHandler.error(new ErrorEvent(throwable));
        Mockito.verify(logger).error("Unexpected error: {}", null, throwable);
    }

    @Test
    public void error_ignoredException_notHandled() {
        DefaultErrorHandler errorHandler = Mockito
                .spy(new DefaultErrorHandler(Set.of(IOException.class.getName(),
                        MalformedURLException.class.getName(),
                        "com.vaadin.flow.server.DefaultErrorHandlerTest$InnerException")));

        errorHandler.error(new ErrorEvent(new IOException()));
        errorHandler.error(new ErrorEvent(new MalformedURLException()));
        errorHandler.error(new ErrorEvent(new InnerException()));

        Mockito.verify(logger, Mockito.never()).error(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(Throwable.class));
    }

    @Test
    public void error_subclassOfIgnoredException_errorHandled() {
        DefaultErrorHandler errorHandler = Mockito.spy(
                new DefaultErrorHandler(Set.of(IOException.class.getName())));

        Throwable throwable = new MalformedURLException();
        errorHandler.error(new ErrorEvent(throwable));
        Mockito.verify(logger).error("Unexpected error: {}", null, throwable);
    }

    @Test
    public void error_loggerAtDebugLevel_errorHandled() {
        Mockito.reset(logger);
        Mockito.doReturn(true).when(logger).isDebugEnabled();

        DefaultErrorHandler errorHandler = Mockito
                .spy(new DefaultErrorHandler(Set.of(IOException.class.getName(),
                        MalformedURLException.class.getName(),
                        "com.vaadin.flow.server.DefaultErrorHandlerTest$InnerException")));

        Throwable throwable = new RuntimeException();
        errorHandler.error(new ErrorEvent(throwable));
        Mockito.verify(logger).error("Unexpected error: {}", null, throwable);

        throwable = new IOException();
        errorHandler.error(new ErrorEvent(throwable));
        Mockito.verify(logger).error("Unexpected error: {}", null, throwable);

        throwable = new MalformedURLException();
        errorHandler.error(new ErrorEvent(throwable));
        Mockito.verify(logger).error("Unexpected error: {}", null, throwable);

        throwable = new InnerException();
        errorHandler.error(new ErrorEvent(throwable));
        Mockito.verify(logger).error("Unexpected error: {}", null, throwable);

        throwable = new UncheckedIOException(new IOException());
        errorHandler.error(new ErrorEvent(throwable));
        Mockito.verify(logger).error("Unexpected error: {}",
                "java.io.IOException", throwable);
    }

    public static class InnerException extends Exception {
    }
}

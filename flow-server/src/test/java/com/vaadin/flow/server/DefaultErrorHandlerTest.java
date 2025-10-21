/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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

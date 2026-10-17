/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultErrorHandlerTest {

    MockedStatic<LoggerFactory> loggerFactory;
    Logger logger;

    @BeforeEach
    void setUp() throws Exception {
        logger = Mockito
                .spy(LoggerFactory.getLogger(DefaultErrorHandler.class));
        loggerFactory = mockStatic(LoggerFactory.class);
        loggerFactory
                .when(() -> LoggerFactory
                        .getLogger(DefaultErrorHandler.class.getName()))
                .thenReturn(logger);
        when(logger.isDebugEnabled()).thenReturn(false);
    }

    @AfterEach
    void tearDown() throws Exception {
        loggerFactory.close();
    }

    @Test
    void error_acceptedException_errorHandled() {
        DefaultErrorHandler errorHandler = Mockito
                .spy(new DefaultErrorHandler(Set.of(IOException.class.getName(),
                        MalformedURLException.class.getName())));

        Throwable throwable = new RuntimeException();
        errorHandler.error(new ErrorEvent(throwable));
        verify(logger).error("Unexpected error: {}", null, throwable);

        throwable = new IllegalArgumentException();
        errorHandler.error(new ErrorEvent(throwable));
        verify(logger).error("Unexpected error: {}", null, throwable);
    }

    @Test
    void error_ignoredException_notHandled() {
        DefaultErrorHandler errorHandler = Mockito
                .spy(new DefaultErrorHandler(Set.of(IOException.class.getName(),
                        MalformedURLException.class.getName(),
                        "com.vaadin.flow.server.DefaultErrorHandlerTest$InnerException")));

        errorHandler.error(new ErrorEvent(new IOException()));
        errorHandler.error(new ErrorEvent(new MalformedURLException()));
        errorHandler.error(new ErrorEvent(new InnerException()));

        verify(logger, never()).error(anyString(), any(Throwable.class));
    }

    @Test
    void error_subclassOfIgnoredException_errorHandled() {
        DefaultErrorHandler errorHandler = spy(
                new DefaultErrorHandler(Set.of(IOException.class.getName())));

        Throwable throwable = new MalformedURLException();
        errorHandler.error(new ErrorEvent(throwable));
        verify(logger).error("Unexpected error: {}", null, throwable);
    }

    @Test
    void error_loggerAtDebugLevel_errorHandled() {
        reset(logger);
        doReturn(true).when(logger).isDebugEnabled();

        DefaultErrorHandler errorHandler = Mockito
                .spy(new DefaultErrorHandler(Set.of(IOException.class.getName(),
                        MalformedURLException.class.getName(),
                        "com.vaadin.flow.server.DefaultErrorHandlerTest$InnerException")));

        Throwable throwable = new RuntimeException();
        errorHandler.error(new ErrorEvent(throwable));
        verify(logger).error("Unexpected error: {}", null, throwable);

        throwable = new IOException();
        errorHandler.error(new ErrorEvent(throwable));
        verify(logger).error("Unexpected error: {}", null, throwable);

        throwable = new MalformedURLException();
        errorHandler.error(new ErrorEvent(throwable));
        verify(logger).error("Unexpected error: {}", null, throwable);

        throwable = new InnerException();
        errorHandler.error(new ErrorEvent(throwable));
        verify(logger).error("Unexpected error: {}", null, throwable);

        throwable = new UncheckedIOException(new IOException());
        errorHandler.error(new ErrorEvent(throwable));
        verify(logger).error("Unexpected error: {}", "java.io.IOException",
                throwable);
    }

    public static class InnerException extends Exception {
    }
}

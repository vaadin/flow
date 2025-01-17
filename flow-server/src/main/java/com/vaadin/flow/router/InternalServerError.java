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
package com.vaadin.flow.router;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.internal.DefaultErrorHandler;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * This is a basic default error view shown on exceptions during navigation.
 *
 * @since 1.0
 */
@Tag(Tag.DIV)
@AnonymousAllowed
@DefaultErrorHandler
public class InternalServerError extends Component
        implements HasErrorParameter<Exception> {

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<Exception> parameter) {
        String exceptionText;
        String errorTextStem = "There was an exception while trying to navigate to '%s'";
        String rootCause = getRootCause(parameter);
        boolean isRootCauseAvailable = rootCause != null
                && !rootCause.isEmpty();
        if (isRootCauseAvailable) {
            exceptionText = String.format(
                    errorTextStem + " with the root cause '%s'",
                    event.getLocation().getPath(), rootCause);
        } else if (parameter != null && parameter.hasCustomMessage()) {
            exceptionText = String.format(
                    errorTextStem + " with the exception message '%s'",
                    event.getLocation().getPath(),
                    parameter.getCustomMessage());
        } else {
            exceptionText = String.format(errorTextStem,
                    event.getLocation().getPath());
        }

        Exception exception = parameter.getException();
        if (exception != null) {
            reportException(exception, event.getLocation().getPath(),
                    exceptionText, isRootCauseAvailable);
        } else {
            getElement().setText(exceptionText);
        }
        return HttpStatusCode.INTERNAL_SERVER_ERROR.getCode();
    }

    /**
     * Returns {@code true} if there is a logging binding available for SLF4J.
     *
     * @return {@code true} if there is a SLF4J logging binding
     */
    protected boolean hasLogBinding() {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        return loggerFactory != null
                && !NOPLoggerFactory.class.equals(loggerFactory.getClass());
    }

    private void reportException(Exception exception, String path,
            String exceptionText, boolean isRootCauseAvailable) {
        if (isRootCauseAvailable) {
            getElement()
                    .appendChild(ElementFactory.createHeading3(exceptionText));
        } else {
            getElement().appendChild(Element.createText(exceptionText));
        }

        VaadinService vaadinService = VaadinService.getCurrent();
        // Check that we have a vaadinService as else we will fail on a NPE and
        // the stacktrace we actually got will disappear and getting a NPE is
        // confusing.
        boolean productionMode = vaadinService != null && vaadinService
                .getDeploymentConfiguration().isProductionMode();

        if (!productionMode) {
            checkLogBinding();
            printStacktrace(exception);
        }

        getLogger().error(
                "There was an exception while trying to navigate to '{}'", path,
                exception);
    }

    private void printStacktrace(Exception exception) {
        StringWriter writer = new StringWriter();
        try {
            exception.printStackTrace(new PrintWriter(writer));
            getElement().appendChild(
                    ElementFactory.createPreformatted(writer.toString()));
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                // StringWriter doesn't throw an exception
                assert false;
            }
        }

    }

    private void checkLogBinding() {
        if (!hasLogBinding()) {
            Element logInfo = ElementFactory
                    .createDiv("Your application doesn't have SLF4J binding. "
                            + "As a result the logger doesn't do any real logging. "
                            + "Add some binding as a dependency to your project. "
                            + "See details ");
            logInfo.getStyle().setMargin("10px 0");
            logInfo.getStyle().setFontWeight("bold");
            logInfo.getStyle().setColor("#6495ED");
            logInfo.appendChild(ElementFactory.createAnchor(
                    "https://www.slf4j.org/manual.html#swapping", "here"));
            getElement().appendChild(logInfo);
        }
    }

    private String getRootCause(ErrorParameter<Exception> parameter) {
        if (parameter == null || parameter.getException() == null
                || parameter.getException().getCause() == null) {
            return null;
        }
        Throwable rootCause = null;
        Throwable cause = parameter.getException().getCause();
        while (cause != null && cause != rootCause) {
            rootCause = cause;
            cause = cause.getCause();
        }
        return rootCause.toString();
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(InternalServerError.class.getName());
    }
}

/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import javax.servlet.http.HttpServletResponse;

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
import com.vaadin.flow.server.VaadinService;

/**
 * This is a basic default error view shown on exceptions during navigation.
 *
 * @since 1.0
 */
@Tag(Tag.DIV)
public class InternalServerError extends Component
        implements HasErrorParameter<Exception> {

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<Exception> parameter) {
        String exceptionText;
        if (parameter.hasCustomMessage()) {
            exceptionText = String.format(
                    "There was an exception while trying to navigate to '%s'"
                            + " with the exception message '%s'",
                    event.getLocation().getPath(),
                    parameter.getCustomMessage());
        } else {
            exceptionText = String.format(
                    "There was an exception while trying to navigate to '%s'",
                    event.getLocation().getPath());
        }

        Exception exception = parameter.getException();
        if (exception != null) {
            reportException(exception, event.getLocation().getPath(),
                    exceptionText);
        } else {
            getElement().setText(exceptionText);
        }
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
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
            String exceptionText) {
        getElement().appendChild(Element.createText(exceptionText));

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
            logInfo.getStyle().set("marginTop", "10px");
            logInfo.getStyle().set("marginBottom", "10px");
            logInfo.getStyle().set("fontWeight", "bold");
            logInfo.getStyle().set("color", "#6495ED");
            logInfo.appendChild(ElementFactory.createAnchor(
                    "https://www.slf4j.org/manual.html#swapping", "here"));
            getElement().appendChild(logInfo);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(InternalServerError.class.getName());
    }
}

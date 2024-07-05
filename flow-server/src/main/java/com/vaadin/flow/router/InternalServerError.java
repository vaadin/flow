/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
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
import com.vaadin.flow.router.internal.DefaultErrorHandler;
import com.vaadin.flow.server.VaadinService;

/**
 * This is a basic default error view shown on exceptions during navigation.
 *
 * @since 1.0
 */
@Tag(Tag.DIV)
@DefaultErrorHandler
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

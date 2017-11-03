/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.router;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;

/**
 * This is a basic default error view shown on exceptions during navigation.
 */
@Tag(Tag.DIV)
public class InternalServerError extends Component
        implements HasErrorParameter<Exception> {

    private static final String EXCEPTION_TRYING_NAVIGATE_S = "There was an exception while trying to navigate to '%s'";

    @Override
    public int setErrorParameter(BeforeNavigationEvent event,
            ErrorParameter<Exception> parameter) {
        String exceptionText;
        if (parameter.hasCustomMessage()) {
            exceptionText = String.format(
                    EXCEPTION_TRYING_NAVIGATE_S
                            + " with the exception message '%s'",
                    event.getLocation().getPath(),
                    parameter.getCustomMessage());
        } else {
            exceptionText = String.format(EXCEPTION_TRYING_NAVIGATE_S,
                    event.getLocation().getPath());
        }

        Exception exception = parameter.getException();
        if (exception != null) {
            getLogger().log(Level.SEVERE, exception,
                    () -> String.format(EXCEPTION_TRYING_NAVIGATE_S,
                            event.getLocation().getPath()));
        }

        getElement().setText(exceptionText);
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    private static Logger getLogger() {
        return Logger.getLogger(InternalServerError.class.getName());
    }
}

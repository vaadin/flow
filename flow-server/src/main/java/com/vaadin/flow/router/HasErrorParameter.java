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

import java.io.Serializable;

/**
 * Defines a view that handles the exceptions for the set Exception type T.
 *
 * @param <T>
 *            type Exception type handled
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface HasErrorParameter<T extends Exception> extends Serializable {

    /**
     * Callback executed before rendering the exception view.
     * <p>
     * Note! returned int should be a valid
     * {@link javax.servlet.http.HttpServletResponse} code
     *
     * @param event
     *            the before navigation event for this request
     * @param parameter
     *            error parameter containing custom exception and caught
     *            exception
     * @return a valid {@link javax.servlet.http.HttpServletResponse} code
     */
    int setErrorParameter(BeforeEnterEvent event, ErrorParameter<T> parameter);
}

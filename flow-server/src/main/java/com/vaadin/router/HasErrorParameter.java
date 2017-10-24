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

import com.vaadin.router.event.BeforeNavigationEvent;

/**
 * Event sent to the error handler that handlers given Exception type T. This is
 * done for caught exceptions during navigation.
 *
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface HasErrorParameter<T extends Exception> {

    /**
     * Method called for the implementing class when a corresponding exception
     * has been caught during navigation.
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
    int setErrorParameter(BeforeNavigationEvent event,
            ErrorParameter<T> parameter);
}

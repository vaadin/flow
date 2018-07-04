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

package com.vaadin.flow.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of {@link ErrorHandler}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DefaultErrorHandler implements ErrorHandler {
    @Override
    public void error(ErrorEvent event) {
        Throwable t = findRelevantThrowable(event.getThrowable());

        // print the error on console
        getLogger().error("", t);
    }

    /**
     * Vaadin wraps exceptions in its own and due to reflection usage there
     * might be also other irrelevant exceptions that make no sense for Vaadin
     * users (~developers using Vaadin). This method tries to choose the
     * relevant one to be reported.
     *
     * @param t
     *            a throwable passed to ErrorHandler
     * @return the throwable that is relevant for Vaadin users
     */
    public static Throwable findRelevantThrowable(Throwable t) {
        return t;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(DefaultErrorHandler.class.getName());
    }

}

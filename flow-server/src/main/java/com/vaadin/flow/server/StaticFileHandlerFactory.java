/*
 * Copyright 2000-2023 Vaadin Ltd.
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

/**
 * A factory to create a {@link StaticFileHandler}.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
public interface StaticFileHandlerFactory {

    /**
     * Creates a new instance of {@link StaticFileHandler} for the given
     * {@code service}.
     *
     * @param service
     *            a {@link VaadinServletService} instance
     * @return a new {@link StaticFileHandler} instance for the {@code service}
     */
    StaticFileHandler createHandler(VaadinService service);
}

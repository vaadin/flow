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
package com.vaadin.flow.function;

import java.io.Serializable;
import java.util.function.BiFunction;

import javax.servlet.ServletContext;

import com.vaadin.flow.server.StreamResource;

/**
 * Content type resolver.
 * <p>
 * Allows to get content type for the given {@link StreamResource} instance
 * using the current {@link ServletContext}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public interface ContentTypeResolver extends
        BiFunction<StreamResource, ServletContext, String>, Serializable {

}

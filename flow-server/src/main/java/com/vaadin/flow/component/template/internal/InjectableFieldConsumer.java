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
package com.vaadin.flow.component.template.internal;

import java.lang.reflect.Field;

/**
 * Three argument consumer.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
@FunctionalInterface
public interface InjectableFieldConsumer {

    /**
     * Performs this operation on the given arguments.
     * <p>
     * The arguments are: the field declared in a template class, the identifier
     * of the element inside the HTML template file, the element tag.
     *
     * @param field
     *            the field declared in a template class
     * @param id
     *            the element id
     * @param tag
     *            the element tag
     */
    void apply(Field field, String id, String tag);
}

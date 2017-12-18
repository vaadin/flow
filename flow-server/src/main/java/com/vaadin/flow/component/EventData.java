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
package com.vaadin.flow.component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.dom.Element;

/**
 * Maps data from a DOM event to a {@link ComponentEvent}.
 * <p>
 * This annotation should be added to the DOM event constructor in a
 * {@link ComponentEvent}, mapped using @{@link DomEvent}. See the @
 * {@link DomEvent} documentation for more information.
 *
 * @see DomEvent
 * @see Element#addEventListener(String,
 *      com.vaadin.flow.dom.DomEventListener, String...)
 * @author Vaadin Ltd
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
@Documented
public @interface EventData {
    /**
     * The identifier used in
     * {@link Element#addEventListener(String, com.vaadin.flow.dom.DomEventListener, String...)}
     * to identify the event data.
     *
     * @return the identifier to use for fetching event data
     */
    String value();
}

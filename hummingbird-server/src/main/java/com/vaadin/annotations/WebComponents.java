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
package com.vaadin.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.UI;

/**
 * Allows to specify polyfill (webcomponentsjs) version to use. Version 0 is
 * used by default.
 * <p>
 * Add the annotation to your {@link UI} or {@link VaadinServlet} class and
 * specify version number using {@link #value()}.
 *
 * @since
 *
 * @author Vaadin Ltd
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
public @interface WebComponents {

    /**
     * Gets polyfill version to use.
     * <p>
     * Version 0 is used by default.
     * <p>
     * Currently supported versions are {@code 0} and {@code 1}.
     * 
     * @return which polyfill version to use
     */
    int value() default 0;
}

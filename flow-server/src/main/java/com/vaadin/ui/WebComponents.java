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
package com.vaadin.ui;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.ui.UI;

/**
 * Allows to configure polyfill (webcomponents.js) for Flow application.
 * <p>
 * In order to set the settings, add the annotation to your {@link UI} or
 * {@link VaadinServlet} class.
 *
 * @author Vaadin Ltd
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
public @interface WebComponents {
    /**
     * Gets whether the application should load the ES5 adapter polyfill. This
     * polyfill is only needed when serving ES5 files on ES6 browsers.
     * <p>
     * The default is <code>false</code>.
     *
     * @return <code>true</code> if the es5 adapter should be loaded,
     *         <code>false</code> otherwise
     */
    boolean loadEs5Adapter() default false;

    /**
     * Gets whether the application should force the usage of the ShadyDOM
     * polyfill by webcomponents.js.
     * <p>
     * The default is <code>false</code>.
     *
     * @return <code>true</code> if the webcomponents polyfill should use
     *         ShadyDOM, <code>false</code> otherwise
     */
    boolean forceShadyDom() default false;
}

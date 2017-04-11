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
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.ui.UI;

/**
 * Allows to specify polyfill (webcomponentsjs) version to use, and other
 * configurations related to web components in general.
 * {@link PolyfillVersion#V0} is used by default.
 * <p>
 * Add the annotation to your {@link UI} or {@link VaadinServlet} class and
 * specify the polyfill version using {@link #value()}.
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
     * Enumeration of all versions of the webcomponents.js polyfill supported by
     * the framework.
     * 
     * @author Vaadin Ltd
     */
    enum PolyfillVersion {
        V0, V1;
    }

    /**
     * Gets polyfill version to use.
     * <p>
     * {@link PolyfillVersion#V0} is used by default.
     * 
     * @return which polyfill version to use
     */
    PolyfillVersion value() default PolyfillVersion.V0;

    /**
     * Gets whether the application should load the ES5 adapter polyfill. This
     * polyfill is only needed when serving ES5 files on ES6 browsers.
     * <p>
     * By default it returns <code>false</code>.
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

    /**
     * Gets the base URL of ES6 components. This property is read when the
     * protocol {@literal webcomponent://} is used at {@link HtmlImport}.
     * <p>
     * The default value is {@literal context://build/es6/}
     * 
     * @return The URL where ES6 components can be found.
     */
    String es6BuildUrl() default ApplicationConstants.ES6_BUILD_URL_DEFAULT_VALUE;

    /**
     * Gets the base URL of ES5 components. This property is read when the
     * protocol {@literal webcomponent://} is used at {@link HtmlImport}.
     * <p>
     * The default value is {@literal context://build/es5/}
     * 
     * @return The URL where ES5 components can be found.
     */
    String es5BuildUrl() default ApplicationConstants.ES5_BUILD_URL_DEFAULT_VALUE;
}

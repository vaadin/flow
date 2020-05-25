/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.internal.RouteUtil;

/**
 * Defines the route template suffix for components that function as
 * navigation targets in routing.
 * <p>
 * The route template of the navigation target is composed of the values of
 * all {@link RoutePrefix} annotated on the {@link #layout()} and
 * {@link ParentLayout} class values, starting from the root parent and joined
 * together using slash delimiter to form a path form string.
 * <p>
 * There is also {@link RouteAlias} annotation which may be declared in addition
 * to this annotation and may be used multiple times.
 *
 * @see RouteAlias
 * @see RoutePrefix
 * @see RouterLayout
 * @see UI
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface Route {

    String NAMING_CONVENTION = "___NAMING_CONVENTION___";

    /**
     * Sets the route template suffix value of the annotated class.
     *
     * <p>
     * If no value is provided, the path will be derived from the class name of
     * the component. The derived name will be in lower case and trailing "View"
     * will be removed. Also, MainView or Main names will be mapped to root
     * (value will be "").
     * </p>
     *
     * <p>
     * This value accepts also parameter template segments which can be defined
     * using following format: <code>:parameterName[?|*][(regex)]</code>.</p>
     *
     * <p>
     * Note for framework developers: do not use the value directly, but use the
     * helper method {@link RouteUtil#resolve(Class, Route)}, so that naming
     * convention based values are dealt correctly.
     * </p>
     *
     * @return the explicit path value of this route
     */
    String value() default NAMING_CONVENTION;

    /**
     * Sets the parent component for the route target component.
     * <p>
     * When navigating between components that use the same layout, the same
     * component instance is reused. Default layout target is the {@link UI},
     * but the layout should not be a custom {@code UI} as {@code UI} is a special
     * class used to know where the route stack ends and no parent layouts should
     * be involved.
     *
     * <p>
     * All layout stacks will be appended to the {@code UI} as it represents
     * the Body element.
     *
     * @return the layout component class used by the route target component.
     * @see RouterLayout
     */
    Class<? extends RouterLayout> layout() default UI.class;

    /**
     * Ignore route prefixes from parent layouts and only use the path defined
     * in this annotation.
     *
     * @return <code>true</code> to ignore parent layout prefixes,
     *         <code>false</code> otherwise
     */
    boolean absolute() default false;

    /**
     * Marks if this Route should be registered during the initial route
     * registration on servlet startup.
     * <p>
     * Default is to register route at startup.
     *
     * @return setting to false skips automatic registration
     */
    boolean registerAtStartup() default true;

}

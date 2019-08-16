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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;

/**
 * Configures subclasses of {@link VaadinServlet}. For a {@link VaadinServlet}
 * class that has this annotation, the defined values are read during
 * initialization and will be available using
 * {@link DeploymentConfiguration#getApplicationOrSystemProperty(String, Object, Function)}
 * as well as from specific methods in {@link DeploymentConfiguration}. Init
 * params defined in <code>web.xml</code> or the <code>@WebServlet</code>
 * annotation take precedence over values defined in this annotation.
 *
 * @deprecated Leftover from Vaadin Framework 8 where the developer typically
 *             defines their own servlet class. Starting from Flow 1.0, a
 *             servlet is automatically registered and this annotation serves
 *             very little purpose.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Deprecated
public @interface VaadinServletConfiguration {
    /**
     * Defines the init parameter name for methods in
     * {@link VaadinServletConfiguration}.
     *
     * @author Vaadin Ltd
     * @since 1.0
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Documented
    @interface InitParameterName {
        /**
         * The name of the init parameter that the annotated method controls.
         *
         * @return the parameter name
         */
        String value();
    }

    /**
     * Whether Vaadin is in production mode.
     *
     * @return true if in production mode, false otherwise.
     *
     * @see DeploymentConfiguration#isProductionMode()
     */
    @InitParameterName(Constants.SERVLET_PARAMETER_PRODUCTION_MODE)
    boolean productionMode();

    /**
     * Gets the UI class to use for the servlet.
     *
     * @return the UI class
     */
    @InitParameterName(VaadinSession.UI_PARAMETER)
    Class<? extends UI> ui() default UI.class;

    /**
     * The number of seconds between heartbeat requests of a UI, or a
     * non-positive number if heartbeat is disabled. The default value is 300
     * seconds, i.e. 5 minutes.
     *
     * @return the time between heartbeats
     *
     * @see DeploymentConfiguration#getHeartbeatInterval()
     */
    @InitParameterName(Constants.SERVLET_PARAMETER_HEARTBEAT_INTERVAL)
    int heartbeatInterval() default DefaultDeploymentConfiguration.DEFAULT_HEARTBEAT_INTERVAL;

    /**
     * Whether a session should be closed when all its open UIs have been idle
     * for longer than its configured maximum inactivity time. The default value
     * is <code>false</code>.
     *
     * @return true if UIs and sessions receiving only heartbeat requests are
     *         eventually closed; false if heartbeat requests extend UI and
     *         session lifetime indefinitely
     *
     * @see DeploymentConfiguration#isCloseIdleSessions()
     */
    @InitParameterName(Constants.SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS)
    boolean closeIdleSessions() default DefaultDeploymentConfiguration.DEFAULT_CLOSE_IDLE_SESSIONS;

    /**
     * Whether the framework should register automatic servlets for the
     * application or not. The default value is <code>false</code>.
     *
     * @return true if no automatic servlet registration should be done
     *
     * @see DeploymentConfiguration#disableAutomaticServletRegistration()
     */
    @InitParameterName(Constants.DISABLE_AUTOMATIC_SERVLET_REGISTRATION)
    boolean disableAutomaticServletRegistration() default false;

}

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

package com.vaadin.flow.component.page;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

/**
 * Configures automatic server push for a root {@link com.vaadin.flow.router.Route navigation target}
 * (or custom {@link UI}). If some other push mode is desired, it can be passed as a parameter, e.g.
 * <code>@Push(PushMode.MANUAL)</code>.
 *
 * @see PushMode
 * @see com.vaadin.flow.router.Route
 * @see com.vaadin.flow.router.RoutePrefix
 * @see com.vaadin.flow.router.RouterLayout
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Push {
    /**
     * The {@link PushMode} to use for the annotated root navigation target (or custom UI). The default
     * push mode when this annotation is present is {@link PushMode#AUTOMATIC}.
     *
     * @return the push mode to use
     */
    PushMode value() default PushMode.AUTOMATIC;

    /**
     * Transport type used for the push for the annotated root navigation target (or custom UI). The
     * default transport type when this annotation is present is
     * {@link Transport#WEBSOCKET_XHR}.
     *
     * @return the transport type to use
     */
    Transport transport() default Transport.WEBSOCKET_XHR;

}

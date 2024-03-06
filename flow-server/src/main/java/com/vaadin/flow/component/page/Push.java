/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.page;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

/**
 * Configures automatic server push. The annotation should be placed on your
 * AppShellConfigurator class. If some other push mode is desired, it can be
 * passed as a parameter, e.g. <code>@Push(PushMode.MANUAL)</code>.
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
     * The {@link PushMode} to use for the annotated root navigation target (or
     * custom UI). The default push mode when this annotation is present is
     * {@link PushMode#AUTOMATIC}.
     *
     * @return the push mode to use
     */
    PushMode value() default PushMode.AUTOMATIC;

    /**
     * Transport type used for the push for the annotated root navigation target
     * (or custom UI). The default transport type when this annotation is
     * present is {@link Transport#WEBSOCKET_XHR}.
     *
     * @return the transport type to use
     */
    Transport transport() default Transport.WEBSOCKET_XHR;

}

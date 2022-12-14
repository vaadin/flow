package com.vaadin.flow.uitest.ui.push;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CustomPush {
    PushMode value() default PushMode.AUTOMATIC;

    Transport transport() default Transport.WEBSOCKET_XHR;

}

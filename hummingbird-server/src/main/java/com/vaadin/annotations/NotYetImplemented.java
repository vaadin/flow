package com.vaadin.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface NotYetImplemented {
    String value() default ""; // Just a free form info string
}

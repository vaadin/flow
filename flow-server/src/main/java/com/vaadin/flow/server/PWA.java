package com.vaadin.flow.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PWA {

    String version() default PwaConfiguration.DEFAULT_VERSION;

    /**
     *
     * @return
     */
    String startUrl() default PwaConfiguration.DEFAULT_START_URL;

    String offlinePath() default PwaConfiguration.DEFAULT_OFFLINE_PATH;

    /**
     * Manifest.json url
     *
     * @return
     */
    String manifestPath() default PwaConfiguration.DEFAULT_PATH;

    /**
     * Path of logo.
     *
     * If the logo -file is not found, falls back to default logo
     *
     * @return
     */
    String logoPath() default PwaConfiguration.DEFAULT_LOGO;

    /**
     * Name of the application.
     *
     * @return
     */
    String name();

    /**
     * Short name for application.
     *
     * Maximum of 12 characters.
     *
     * @return
     */
    String shortName();

    String themeColor() default PwaConfiguration.DEFAULT_THEME_COLOR;

    String backgroundColor() default PwaConfiguration.DEFAULT_BACKGROUND_COLOR;

    /**
     * Defines the developers’ preferred display mode for the website.
     *
     * Possible values:
     * fullscreen, standalone, minimal-ui, browser
     *
     * @return
     */
    String display() default PwaConfiguration.DEFAULT_DISPLAY;

    boolean enabled() default true;
}

package com.vaadin.flow.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Manifest {

    /**
     * I guess this should be just context-path, no editing whatsoever?
     * Or maybe fall back to context-path if empty?
     * @return
     */
    String startUrl() default PwaConfiguration.DEFAULT_START_URL;

    String offlinePath() default PwaConfiguration.DEFAULT_OFFLINE_PATH;

    /**
     * Sets the manifest.json url
     *
     * @return
     */
    String manifestPath() default PwaConfiguration.DEFAULT_PATH;

    /**
     * Sets the path of logo.
     *
     * If the logo -file is not found, falls back to default logo
     *
     * @return
     */
    String logoPath() default PwaConfiguration.DEFAULT_LOGO;

    /**
     * Sets name of the project.
     *
     * @return
     */
    String name() default PwaConfiguration.DEFAULT_NAME;

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

    boolean disableServiceWorker() default false;

    boolean disableManifest() default false;

}

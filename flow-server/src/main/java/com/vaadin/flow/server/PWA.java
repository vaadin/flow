package com.vaadin.flow.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PWA {

    /**
     * Path to static offline html file.
     *
     * Defaults to (relative) "offline.html"
     * with default configuration that is webapp/offline.html
     *
     * If offline file is not found, falls back to default offline page
     *
     * @return
     */
    String offlinePath() default PwaConfiguration.DEFAULT_OFFLINE_PATH;

    /**
     * path to Manifest.json.
     *
     * Defaults to (relative) "manifest.json"
     * with default configuration that is webapp/manifest.json
     *
     * @return
     */
    String manifestPath() default PwaConfiguration.DEFAULT_PATH;

    /**
     * Path of logo.
     *
     * Defaults to (relative) "icons/logo.png"
     *
     * with default configuration that is webapp/manifest.json
     *
     * If the logo -file is not found, falls back to default logo.
     *
     * Logo is also used to create different sizes of logo.
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

    /**
     * Description of application.
     *
     * @return
     */
    String description() default "";

    /**
     * Theme color of application.
     *
     * The theme color sets the color of the tool bar, and in the task switcher.
     *
     * @return
     */
    String themeColor() default PwaConfiguration.DEFAULT_THEME_COLOR;

    /**
     * Background color of application.
     *
     * The background_color property is used on the splash screen when the
     * application is first launched.
     *
     * @return
     */
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

    /**
     * Offline resources to be cached with service worker.
     *
     * @return
     */
    String[] offlineResources() default {};

    boolean enabled() default true;
}

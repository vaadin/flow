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
package com.vaadin.flow.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines application PWA properties.
 *
 * Annotation required in order activate automatic pwa injecting.
 *
 * Only 1 annotation for application is supported.
 *
 * Annotation must be placed to master layout.
 *
 * Application annotated with enabled PWA will add following capabilities
 * to flow application:
 *
 * - handle manifest.json
 * - handle sw.js (service worker),
 *     which will handle simple offline fallback and file caching
 * - handle default (static) offline html page
 * - handle different versions (sizes) of given logo
 * - inject needed tags to header
 *
 * Any of the handled resources can be explicitly overridden with static
 * file in public resources.
 *
 * For example, if "manifest.json" is available in webapp root folder it
 * will be served instead of generated manifest.json. Same applies for
 * service worker and generated icons.
 */
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
     * @return Path to static offline html file
     */
    String offlinePath() default PwaConfiguration.DEFAULT_OFFLINE_PATH;

    /**
     * Path to manifest file.
     *
     * Defaults to (relative) "manifest.json"
     * with default configuration that is webapp/manifest.json
     *
     * @return Path to manifest file
     */
    String manifestPath() default PwaConfiguration.DEFAULT_PATH;

    /**
     * Path to logo.
     *
     * Defaults to (relative) "icons/logo.png"
     *
     * with default configuration that is webapp/manifest.json
     *
     * If the logo -file is not found, falls back to default logo.
     *
     * Logo is also used to create different sizes of logo.
     *
     * @return path to logo
     */
    String logoPath() default PwaConfiguration.DEFAULT_LOGO;

    /**
     * Name of the application.
     *
     * @return Name of the application
     */
    String name();

    /**
     * Short name for application.
     *
     * Maximum of 12 characters.
     *
     * @return Short name for application
     */
    String shortName();

    /**
     * Description of application.
     *
     * @return Description of application
     */
    String description() default "";

    /**
     * Theme color of application.
     *
     * The theme color sets the color of the tool bar, and in the task switcher.
     *
     * @return Theme color of application
     */
    String themeColor() default PwaConfiguration.DEFAULT_THEME_COLOR;

    /**
     * Background color of application.
     *
     * The background_color property is used on the splash screen when the
     * application is first launched.
     *
     * @return Background color of application
     */
    String backgroundColor() default PwaConfiguration.DEFAULT_BACKGROUND_COLOR;

    /**
     * Defines the developers’ preferred display mode for the website.
     *
     * Possible values:
     * fullscreen, standalone, minimal-ui, browser
     *
     * @return display mode of application
     */
    String display() default PwaConfiguration.DEFAULT_DISPLAY;

    /**
     * Offline resources to be cached with service worker.
     *
     * @return Offline resources to be cached
     */
    String[] offlineResources() default {};

    /**
     * Is PWA injecting enabled.
     *
     * @return Is PWA injecting enabled
     */
    boolean enabled() default true;
}

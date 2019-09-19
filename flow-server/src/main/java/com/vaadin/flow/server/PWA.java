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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines application PWA properties.
 *
 * Annotation activates automatic
 * <a href="https://developer.mozilla.org/en-US/Apps/Progressive">PWA</a>
 * injecting.
 * <p>
 * Only one annotation for application is supported. Annotation must be placed
 * to master layout.
 *
 * Application annotated the annotation will add following capabilities to Flow
 * application:
 *
 * <ul>
 * <li>handle manifest.webmanifest
 * <li>handle sw.js (service worker), which will enable simple offline fallback
 * and file caching
 * <li>handle default (static) offline html page
 * <li>handle different versions (sizes) of the given logo
 * <li>inject needed tags to the app's page header
 * </ul>
 *
 * Any of the handled resources can be explicitly overridden with static file in
 * public resources. For example, if {@literal manifest.webmanifest} is available in
 * webapp root folder it will be served instead of generated
 * {@literal manifest.webmanifest}. Same applies for service worker and generated
 * icons.
 *
 * @since 1.2
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/Apps/Progressive">https://developer.mozilla.org/en-US/Apps/Progressive</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PWA {

    /**
     * Path to the static offline html file.
     *
     * Defaults to (relative) {@literal offline.html} with default configuration
     * that is {@literal webapp/offline.html}
     *
     * If offline file is not found, falls back to default offline page.
     *
     * @return path to the static offline html file
     */
    String offlinePath() default PwaConfiguration.DEFAULT_OFFLINE_PATH;

    /**
     * Path to the manifest file.
     *
     * Defaults to (relative) {@literal manifest.webmanifest} with default
     * configuration that is {@literal webapp/manifest.webmanifest}
     *
     * @return path to the manifest file
     */
    String manifestPath() default PwaConfiguration.DEFAULT_PATH;

    /**
     * Path to the application icon file.
     * <p>
     * Defaults to (relative) {@literal icons/icon.png} with default
     * configuration that is {@literal webapp/manifest.webmanifest}
     * <p>
     * If the specified icon file is not found, the default one will be used.
     * The file is also used to create different sizes of icon.
     *
     * @return path to the application icon file
     */
    String iconPath() default PwaConfiguration.DEFAULT_ICON;

    /**
     * (Root relative) start url, used in web-manifest, property start_url
     * <p>
     * Used in manifest as start_url of application. Must be relative to root
     * context. ie. If install address of application would be
     * https://foo.bar.org/sub/  and wanted start url would be
     * https://foo.bar.org/sub/pwa-start then value of startPath would be
     * "pwa-start" (without leading "/").
     * <p>
     * Defaults to root of application.
     *
     * @return application start url
     */
    String startPath() default "";

    /**
     * Name of the application.
     *
     * @return name of the application
     */
    String name();

    /**
     * Short name for the application. Maximum of 12 characters.
     *
     * @return short name for the application
     */
    String shortName();

    /**
     * Description of the application.
     *
     * @return description of the application
     */
    String description() default "";

    /**
     * Theme color of the application.
     *
     * The theme color sets the color of the application's tool bar and
     * application's color in the task switcher.
     *
     * @return theme color of the application
     */
    String themeColor() default PwaConfiguration.DEFAULT_THEME_COLOR;

    /**
     * Background color of the application.
     *
     * The background color property is used on the splash screen when the
     * application is first launched.
     *
     * @return Background color of the application
     */
    String backgroundColor() default PwaConfiguration.DEFAULT_BACKGROUND_COLOR;

    /**
     * Defines the developers’ preferred display mode for the website.
     *
     * Possible values: fullscreen, standalone, minimal-ui, browser
     *
     * @return display mode of application
     */
    String display() default PwaConfiguration.DEFAULT_DISPLAY;

    /**
     * Offline resources to be cached using the service worker.
     *
     * @return offline resources to be cached
     */
    String[] offlineResources() default {};

    /**
     * If enabled, server will inject default pwa -install prompt html and js
     * in bootstrap page.
     * <p>
     * Will capture beforeinstallprompt -event and show install prompt as
     * required from Chrome 68 upwards.
     *
     * @return are pwa -install prompt resources injected.
     */
    boolean enableInstallPrompt() default true;

}

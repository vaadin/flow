/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.dependency;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.component.Component;

/**
 * Annotation for defining an npm package dependency on a {@link Component}
 * class which includes one or more JS modules. The JS Modules can be defined
 * using {@link JsModule} annotation on the same {@link Component}. For adding
 * multiple npm packages files for a single component, you can use this
 * annotation multiple times.
 * <p>
 * Declared npm packages will be bundled by flow-maven-plugin in a package.json
 * file, making sure that only one dependency is created.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
@Repeatable(NpmPackage.Container.class)
public @interface NpmPackage {

    /**
     * npm package to install before loading any JS modules declared using
     * {@link JsModule}.
     *
     * @return an npm JavaScript package
     */
    String value();

    /**
     * Defines the npm package version. It should meet the 'd.d.d' or the
     * 'd.d.d-suffix' pattern.
     * <p>
     * Troubleshooting: when two or more annotations with the same package value
     * are found in the class-path, and their versions do not match, the build
     * process will print a warning message informing about found versions and
     * which one will be used.
     *
     * @return npm package version
     */
    String version();

    /**
     * Internal annotation to enable use of multiple {@link NpmPackage}
     * annotations.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Documented
    @Inherited
    @interface Container {

        /**
         * Internally used to enable use of multiple {@link NpmPackage}
         * annotations.
         *
         * @return an array of the JavaScript annotations
         */
        NpmPackage[] value();
    }
}

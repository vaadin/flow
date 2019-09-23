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
 * Annotation for defining an NPM package dependency on a {@link Component}
 * class which includes one or more JS modules. The JS Modules can be defined
 * using {@link JsModule} annotation on the same {@link Component}. For adding
 * multiple NPM packages files for a single component, you can use this
 * annotation multiple times.
 * <p>
 * Declared NPM packages will be bundled by flow-maven-plugin in a package.json
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
     * NPM package to install before loading any JS modules declared using
     * {@link JsModule}.
     *
     * @return an NPM JavaScript package
     */
    String value();

    /**
     * Defines the NPM package version. It should meet the 'd.d.d' or the
     * 'd.d.d-suffix' pattern.
     * <p>
     * Troubleshooting: when two or more annotations with the same package value
     * are found in the class-path, and their versions do not match the build
     * process will fail.
     *
     * @return NPM package version
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

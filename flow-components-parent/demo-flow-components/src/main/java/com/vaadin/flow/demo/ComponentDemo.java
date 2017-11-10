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
package com.vaadin.flow.demo;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a view as being a component demo view
 *
 * @author Vaadin Ltd
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface ComponentDemo {

    /**
     * Enum for describing the category a DemoView belongs in.
     */
    enum DemoCategory {
        VAADIN, PAPER
    }

    /**
     * Name of the component demo
     * 
     * @return component demo name
     */
    String name();

    /**
     * Which category this demo belongs in, default is
     * {@code DemoCategory.VAADIN}.
     * 
     * @return the demo category
     */
    DemoCategory category() default DemoCategory.VAADIN;

    /**
     * Which subcategory this demo belongs in, default is empty (no
     * subcategory).
     * 
     * @return the demo subcategory
     */
    String subcategory() default "";
}

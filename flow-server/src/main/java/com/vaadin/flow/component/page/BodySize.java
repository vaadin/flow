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
package com.vaadin.flow.component.page;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the body size that will be added to the HTML of the host pages.
 * <p>
 * If no {@code @BodySize} has been applied, the default values
 * {@code height:100vh} and {@code width:100vw} will be used, so the body will
 * fill the entire viewport. If you don't want to set any size for the body, you
 * must apply an empty {@code @BodySize} annotation to disable the default
 * values.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface BodySize {

    /**
     * Definition for body height.
     *
     * @return the body height to set
     */
    String height() default "";

    /**
     * Definition for body width.
     *
     * @return the body width to set
     */
    String width() default "";
}

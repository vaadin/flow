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
package com.vaadin.flow.router;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

 /**
 * This annotation sets the priority of execution on {@link BeforeEnterListener},
 * {@link BeforeLeaveListener} and {@link AfterNavigationListener}. The higher the
 * value of priority, the higher the likelihood of the listener to be the
 * first listener to be executed. Priority-values must be non-negative, with
 * zero being the default-priority, so that any listener with a priority-value of
 * more than 0 will be executed before listeners that are not annotated with
 * {@literal @}ListenerPriority
 * <p>
 *    <pre>
 *      //will be executed first
 *      &#064;ListenerPriority(5)
 *      class HighPriorityListener implements BeforeEnterListener {
 *      }
 *
 *      //will be executed second, default priority is 0
 *      class YetAnotherListener implements BeforeEnterListener {
 *      }
 *
 *      //will be executed third
 *      &#064;ListenerPriority(-5)
 *      class LowPriorityListener implements BeforeEnterListener {
 *      }
 *   </pre>
 * </p>
 *
 * @author Bernd Hopp
 */
 @Retention(RetentionPolicy.RUNTIME)
 @Target(ElementType.TYPE)
 public @interface ListenerPriority {
    /**
     * The priority of the annotated listener, can be any integer. Larger numbers
     * indicate higher priority.
     *
     * @return the priority-value.
     */
    int value();
}

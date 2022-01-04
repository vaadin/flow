/*
 * Copyright 2000-2022 Vaadin Ltd.
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
package com.vaadin.flow.di;

import javax.servlet.ServletContainerInitializer;

/**
 * The presence of the service implementing this interface with
 * {@link #runOnce()} returning {@code true} means that
 * {@link ServletContainerInitializer}s are executed only once and the
 * implementation doesn't have to care about cleaning up data collected based on
 * previous call.
 * <p>
 * In some cases (e.g. OSGi) the
 * {@link ServletContainerInitializer#onStartup(java.util.Set, javax.servlet.ServletContext)}
 * method may be called several times for the application (with different
 * classes provided). In this case the initializer logic should reset the data
 * passed on the previous call and set the new data. To be able to reset the
 * data correctly the {@link ServletContainerInitializer} implementation may
 * need to store additional data between calls which is excessive if the
 * {@link ServletContainerInitializer#onStartup(java.util.Set, javax.servlet.ServletContext)}
 * is executed only once.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
@FunctionalInterface
public interface OneTimeInitializerPredicate {

    /**
     * Checks whether the {@link ServletContainerInitializer}s requires reset to
     * the previous state on
     * {@link ServletContainerInitializer#onStartup(java.util.Set, javax.servlet.ServletContext)}
     * call.
     * 
     * @return {@code true} if {@link ServletContainerInitializer}s are executed
     *         only once, {@code false} otherwise
     */
    boolean runOnce();
}

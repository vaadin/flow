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
package com.vaadin.router.event;

/**
 * Event sent to the active and de-active navigation chain instances
 * implementing this interface before navigation happens.
 *
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface BeforeLeaveListener {

    /**
     * Method called before navigation is executed.
     * 
     * @param event
     *            before navigation event with event details
     */
    void beforeNavigation(BeforeNavigationEvent event);
}

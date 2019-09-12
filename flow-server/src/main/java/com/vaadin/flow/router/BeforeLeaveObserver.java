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
package com.vaadin.flow.router;

import com.vaadin.flow.router.internal.BeforeLeaveHandler;

/**
 * Any attached component implementing this interface will receive an event
 * before leaving the current navigation state.
 * <p>
 * During this event phase there is the possibility to reroute to another
 * navigation target or to postpone the navigation (to for instance get user
 * input).
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface BeforeLeaveObserver extends BeforeLeaveHandler {
}

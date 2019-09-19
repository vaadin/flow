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
 * A listener that may be added to the {@link com.vaadin.flow.component.UI}
 * using
 * {@link com.vaadin.flow.component.UI#addBeforeLeaveListener(BeforeLeaveListener)}.
 * <p>
 * All listeners added this way will be informed when old components are
 * detached from the {@link com.vaadin.flow.component.UI}.
 * <p>
 * During this phase there is the possibility to reroute to another navigation
 * target or to postpone the navigation (to for instance get user input).
 *
 * All BeforeLeaveListeners will be executed before the BeforeLeaveObservers.
 * To control the order of execution of BeforeLeaveListeners, see {@link ListenerPriority}
 *
 * @since 1.0
 */
@FunctionalInterface
public interface BeforeLeaveListener extends BeforeLeaveHandler {
}

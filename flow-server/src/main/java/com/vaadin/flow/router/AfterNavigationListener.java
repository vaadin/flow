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

import com.vaadin.flow.router.internal.AfterNavigationHandler;

/**
 * A listener that may be added to the {@link com.vaadin.flow.component.UI}
 * using
 * {@link com.vaadin.flow.component.UI#addAfterNavigationListener(AfterNavigationListener)}.
 * <p>
 * All listeners added this way will be informed when new components have been
 * attached to the {@link com.vaadin.flow.component.UI} and all navigation tasks
 * have resolved.
 *
 * All AfterNavigationListeners will be executed before the AfterNavigationObservers.
 * To control the order of execution of AfterNavigationListeners, see {@link ListenerPriority}
 *
 * @since 1.0
 */
@FunctionalInterface
public interface AfterNavigationListener extends AfterNavigationHandler {
}

/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.quarkus.it.pushdispatch;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.ui.Transport;

/**
 * Use pure WEBSOCKET transport (not the default WEBSOCKET_XHR) so the
 * navigation request itself flows through the Push websocket. With this
 * transport, the websocket has to be open before any navigation message can be
 * handled, so the deadlock scenario (Push on the event loop holding the lock
 * that afterNavigation's REST call needs to release) becomes deterministic.
 */
@Push(transport = Transport.WEBSOCKET)
public class AppShellConfig implements AppShellConfigurator {
}

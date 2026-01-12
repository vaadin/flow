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
package com.vaadin.flow.server.dau;

import java.io.Serializable;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;

/**
 * A context for {@link UserIdentitySupplier} configurable function. Holds the
 * current instances of Vaadin request and Vaadin session.
 *
 * @param request
 *            current Vaadin request, never {@literal null}.
 * @param session
 *            current Vaadin session, can be {@literal null}
 *
 * @since 24.5
 */
public record UserIdentityContext(VaadinRequest request,
        VaadinSession session) implements Serializable {
}

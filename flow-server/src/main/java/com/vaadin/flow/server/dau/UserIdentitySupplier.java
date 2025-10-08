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

import java.util.Optional;

import com.vaadin.flow.function.SerializableFunction;

/**
 * Provides a custom identifier of end-users that, if configured, is used the by
 * Vaadin's license server to count unique daily users for a given subscription.
 * <p>
 * !Important note: Vaadin doesn't collect, neither it stores or anyhow analyses
 * the possible personal information returned by this function. The returned
 * string value is hashed with subscription key as a salt and only then handled
 * further.
 * <p>
 * Returns non-empty string, wrapped into an Optional, if the identity can be
 * given, e.g. when user is logged in, otherwise returns an empty Optional if no
 * user information is available. The function MUST never return
 * {@literal null}.
 *
 * @since 24.5
 */
public interface UserIdentitySupplier
        extends SerializableFunction<UserIdentityContext, Optional<String>> {
}

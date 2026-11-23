/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import com.vaadin.experimental.CoreFeatureFlagProvider;
import com.vaadin.experimental.DisabledFeatureException;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.shared.ui.Transport;

/**
 * Utility methods for server push.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class PushUtil {

    private PushUtil() {
    }

    /**
     * Checks that the given push transport can be used, i.e. that the feature
     * flag guarding an experimental transport is enabled.
     *
     * @param transport
     *            the transport to check
     * @param context
     *            the context to read the feature flags from, or
     *            <code>null</code> if there is none, in which case an
     *            experimental transport is rejected
     * @throws DisabledFeatureException
     *             if the transport is behind a feature flag that is not enabled
     */
    public static void checkTransportEnabled(Transport transport,
            VaadinContext context) {
        if (transport != Transport.SERVER_SENT_EVENTS) {
            return;
        }
        if (context == null || !FeatureFlags.get(context)
                .isEnabled(CoreFeatureFlagProvider.SSE_PUSH_TRANSPORT)) {
            throw new DisabledFeatureException(
                    CoreFeatureFlagProvider.SSE_PUSH_TRANSPORT);
        }
    }
}

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

package com.vaadin.flow.server.webpush;

import java.io.Serializable;

/**
 * Callback for receiving web push subscription details
 *
 * @since 24.2
 */
@FunctionalInterface
public interface WebPushSubscriptionResponse extends Serializable {

    /**
     * Invoked when the client-side details are available.
     *
     * @param subscription
     *            web push subscription object
     */
    void subscription(WebPushSubscription subscription);
}

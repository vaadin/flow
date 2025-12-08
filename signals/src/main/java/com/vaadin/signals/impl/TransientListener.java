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
package com.vaadin.signals.impl;

/**
 * A listener that is expected to only be invoked the next time some event
 * occurs but not for subsequent events. The listener can optionally request
 * that it retained also for the following event.
 */
@FunctionalInterface
public interface TransientListener {
    /**
     * Invoked when the next event occurs. The return value indicates whether
     * the listener should be retained.
     *
     * @param immdediate
     *            <code>true</code> if the listener is invoked immediately when
     *            it is added, <code>false</code> if the event occurred after
     *            the listener was added
     *
     * @return <code>true</code> to invoke the listener also for the next event,
     *         <code>false</code> to stop invoking the listener
     */
    boolean invoke(boolean immdediate);
}

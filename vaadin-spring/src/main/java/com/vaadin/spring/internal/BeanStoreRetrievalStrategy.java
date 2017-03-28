/*
 * Copyright 2015-2017 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.spring.internal;

/**
 * Strategy interface for getting the
 * {@link com.vaadin.spring.internal.BeanStore}. The strategy pattern is used to
 * make it easier to mock the scope while doing testing. For internal use only.
 *
 * @author Petter Holmström (petter@vaadin.com)
 */
public interface BeanStoreRetrievalStrategy {

    /**
     * Returns the current bean store (never {@code null}).
     */
    BeanStore getBeanStore();

    /**
     * Returns the current conversation ID (never {@code null}).
     */
    String getConversationId();
}

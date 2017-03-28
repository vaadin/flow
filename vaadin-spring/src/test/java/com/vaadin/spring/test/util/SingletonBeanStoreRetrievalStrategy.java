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
package com.vaadin.spring.test.util;

import com.vaadin.spring.internal.BeanStore;
import com.vaadin.spring.internal.BeanStoreRetrievalStrategy;

/**
 * Singleton bean store retrieval strategy that always returns the same bean
 * store and conversation id. This strategy is primarily a helper for testing.
 */
public class SingletonBeanStoreRetrievalStrategy
        implements BeanStoreRetrievalStrategy {

    public static final String CONVERSATION_ID = "testConversation";
    private BeanStore beanStore = new BeanStore("testBeanStore");

    @Override
    public BeanStore getBeanStore() {
        return beanStore;
    }

    @Override
    public String getConversationId() {
        return CONVERSATION_ID;
    }

}

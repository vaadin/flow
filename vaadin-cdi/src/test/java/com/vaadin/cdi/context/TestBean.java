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
package com.vaadin.cdi.context;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.concurrent.atomic.AtomicInteger;

public class TestBean {
    private static AtomicInteger beanCount = new AtomicInteger();
    private String state = "";

    @PostConstruct
    private void construct() {
        beanCount.incrementAndGet();
    }

    @PreDestroy
    private void destruct() {
        beanCount.decrementAndGet();
    }

    public static int getBeanCount() {
        return beanCount.get();
    }

    public static void resetCount() {
        beanCount.set(0);
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}

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
package com.vaadin.client;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.client.UILifecycle.UIState;

public class UILifecycleTest {

    UILifecycle lifecycle = new UILifecycle();

    @Test
    public void initialState() {
        Assert.assertEquals(UIState.INITIALIZING, lifecycle.getState());
    }

    @Test
    public void initialToRunningToTerminated() {
        lifecycle.setState(UIState.RUNNING);
        Assert.assertEquals(UIState.RUNNING, lifecycle.getState());
        lifecycle.setState(UIState.TERMINATED);
        Assert.assertEquals(UIState.TERMINATED, lifecycle.getState());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidStateChangeInitToTerminated() {
        lifecycle.setState(UIState.INITIALIZING);
        lifecycle.setState(UIState.TERMINATED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidStateChangeRunningToInit() {
        lifecycle.setState(UIState.INITIALIZING);
        lifecycle.setState(UIState.RUNNING);
        lifecycle.setState(UIState.INITIALIZING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidStateChangeTerminatedToInit() {
        lifecycle.setState(UIState.INITIALIZING);
        lifecycle.setState(UIState.RUNNING);
        lifecycle.setState(UIState.TERMINATED);
        lifecycle.setState(UIState.INITIALIZING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidStateChangeTerminatedToRunning() {
        lifecycle.setState(UIState.INITIALIZING);
        lifecycle.setState(UIState.RUNNING);
        lifecycle.setState(UIState.TERMINATED);
        lifecycle.setState(UIState.RUNNING);
    }

    @Test
    public void stateChangeEvents() {
        AtomicInteger events = new AtomicInteger(0);

        lifecycle.addHandler(e -> {
            events.incrementAndGet();
        });

        Assert.assertEquals(0, events.get());
        lifecycle.setState(UIState.RUNNING);
        Assert.assertEquals(1, events.get());
        lifecycle.setState(UIState.TERMINATED);
        Assert.assertEquals(2, events.get());
    }

}

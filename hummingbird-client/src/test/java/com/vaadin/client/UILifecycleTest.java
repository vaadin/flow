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

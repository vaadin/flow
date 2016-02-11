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

    @Test
    public void invalidStateChanges() {
        lifecycle.setState(UIState.INITIALIZING);
        Assert.assertEquals(UIState.INITIALIZING, lifecycle.getState());
        lifecycle.setState(UIState.TERMINATED);
        Assert.assertEquals(UIState.INITIALIZING, lifecycle.getState());

        lifecycle.setState(UIState.RUNNING);
        Assert.assertEquals(UIState.RUNNING, lifecycle.getState());
        lifecycle.setState(UIState.INITIALIZING);
        Assert.assertEquals(UIState.RUNNING, lifecycle.getState());

        lifecycle.setState(UIState.TERMINATED);
        Assert.assertEquals(UIState.TERMINATED, lifecycle.getState());
        lifecycle.setState(UIState.INITIALIZING);
        Assert.assertEquals(UIState.TERMINATED, lifecycle.getState());
        lifecycle.setState(UIState.RUNNING);
        Assert.assertEquals(UIState.TERMINATED, lifecycle.getState());
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

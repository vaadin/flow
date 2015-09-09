package com.vaadin.client.communication.tree;

import java.util.ArrayList;
import java.util.List;

public class CallbackQueue {

    private List<Runnable> callbacks = new ArrayList<>();

    public void enqueue(Runnable runnable) {
        callbacks.add(runnable);
    }

    public void flush() {
        List<Runnable> callbacks = this.callbacks;
        this.callbacks = new ArrayList<>();
        for (Runnable runnable : callbacks) {
            runnable.run();
        }
    }
}

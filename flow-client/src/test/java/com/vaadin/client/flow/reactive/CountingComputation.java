/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow.reactive;

import com.vaadin.client.Command;

public class CountingComputation extends Computation {

    private Command reader;

    public CountingComputation(ReactiveEventRouter<?, ?> eventRouter) {
        this(eventRouter::registerRead);
    }

    public CountingComputation(Command reader) {
        this.reader = reader;
    }

    private int count = 0;

    @Override
    protected void doRecompute() {
        count++;
        reader.execute();
    }

    public int getCount() {
        return count;
    }
}

/*
 * Copyright 2000-2020 Vaadin Ltd.
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

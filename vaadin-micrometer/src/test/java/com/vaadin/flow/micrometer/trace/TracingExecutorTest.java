/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.micrometer.trace;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

public class TracingExecutorTest {

    @Test
    public void delegatesToWrappedExecutor() {
        AtomicBoolean ran = new AtomicBoolean();
        Executor inline = Runnable::run;
        new TracingExecutor(inline).execute(() -> ran.set(true));
        Assert.assertTrue("wrapped runnable should have run", ran.get());
    }

    @Test
    public void capturesAndForwardsCommandToDelegate() {
        AtomicReference<Runnable> captured = new AtomicReference<>();
        Executor capturing = captured::set;

        Runnable original = () -> {
        };
        new TracingExecutor(capturing).execute(original);

        // The delegate should have received SOMETHING (the snapshot-wrapped
        // command), not necessarily the original instance: a context-aware
        // wrapper is expected.
        Assert.assertNotNull(captured.get());
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullDelegate() {
        new TracingExecutor(null);
    }
}

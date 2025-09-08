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

package com.vaadin.flow.dom;

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;

import com.vaadin.flow.component.ComponentTest;

public class ExecuteJsPerformanceTest extends ComponentTest.TestDiv {

    private static final int SAMPLE_SIZE = 10;

    @Test
    public void test() {
        var div = new Element("div");

        runTest("Test after (no params)", this::getDurationAfter);
        runTest("Test after (with params)", this::getDurationAfter, div);

        runTest("Test before (no params)", this::getDurationBefore);
        runTest("Test before (with params)", this::getDurationBefore, div);

    }

    private void runTest(String message,
            Function<Serializable[], Long> consumer,
            Serializable... parameters) {
        var median = IntStream.range(0, SAMPLE_SIZE).boxed()
                .map(item -> consumer.apply(parameters)).sorted().toList()
                .get(SAMPLE_SIZE / 2);
        System.err.println(message + ":" + median);
    }

    private long getDurationBefore(Serializable... parameters) {
        var stopwatch = StopWatch.create();
        stopwatch.start();
        // Stream<Serializable> jsParameters = Stream.concat(Stream.of(this),
        // Stream.of(parameters));
        Stream<Serializable> wrappedParameters = Stream
                .concat(Stream.of(parameters), Stream.of(this));
        stopwatch.stop();
        return stopwatch.getNanoTime();
    }

    private long getDurationAfter(Serializable... parameters) {
        var stopwatch = StopWatch.create();
        stopwatch.start();
        // Serializable[] jsParameters = new Serializable[parameters.length +
        // 1];
        // jsParameters[0] = this;
        // System.arraycopy(parameters, 0, jsParameters, 1, parameters.length);
        Serializable[] wrappedParameters = Arrays.copyOf(parameters,
                parameters.length + 1);
        wrappedParameters[parameters.length] = this;
        // Stream<Serializable> wrappedParameters;
        // if (parameters.length == 0) {
        // wrappedParameters = Stream.of(this);
        // } else {
        // wrappedParameters = Stream.concat(Stream.of(parameters),
        // Stream.of(this));
        // }
        stopwatch.stop();
        return stopwatch.getNanoTime();
    }

}
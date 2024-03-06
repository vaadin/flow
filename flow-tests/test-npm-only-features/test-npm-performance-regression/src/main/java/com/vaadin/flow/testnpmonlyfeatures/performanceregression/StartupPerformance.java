/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testnpmonlyfeatures.performanceregression;

import akka.actor.Actor;
import akka.actor.ActorSelection;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.google.common.collect.Multimap;
import com.google.inject.Injector;
import org.tensorflow.Tensor;
import org.tensorflow.op.data.TensorSliceDataset;
import org.tensorflow.op.image.DecodeJpeg;
import org.tensorflow.op.math.SegmentSum;
import org.tensorflow.op.summary.WriteAudioSummary;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("")
public class StartupPerformance extends Div {
    // Reference classes in various libraries. If bytecode scanning is in use,
    // visiting these classes is expected to be quite slow.
    Tensor<Integer> tensor;
    TensorSliceDataset dataSet;
    WriteAudioSummary writeAudioSummary;
    SegmentSum<Integer> segmentSum;
    DecodeJpeg decodeJpeg;
    Multimap<String, String> mmap;
    Injector injector;
    MetricRegistry registry;
    MetricSet metricSet;
    ConsoleReporter reporter;
    Gauge gauge;
    Actor actor;
    ActorSelection actorSelection;

    public StartupPerformance() {
        setId("performance-component");
    }
}

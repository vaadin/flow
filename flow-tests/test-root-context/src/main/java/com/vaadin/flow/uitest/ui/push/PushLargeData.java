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
package com.vaadin.flow.uitest.ui.push;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.uitest.ui.util.LoremIpsum;

public abstract class PushLargeData extends AbstractTestViewWithLog {

    // 200KB
    static final int DEFAULT_SIZE_BYTES = 200 * 1000;

    // Every other second
    static final int DEFAULT_DELAY_MS = 2000;

    // 3 MB is enough for streaming to reconnect
    static final int DEFAULT_DATA_TO_PUSH = 3 * 1000 * 1000;

    static final int DEFAULT_DURATION_MS = DEFAULT_DATA_TO_PUSH
            / DEFAULT_SIZE_BYTES * DEFAULT_DELAY_MS;

    private Div dataLabel = new Div();

    private final ExecutorService executor = Executors
            .newSingleThreadExecutor();

    protected Input dataSize;

    protected Input interval;

    protected Input duration;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        dataLabel.setSizeUndefined();
        dataLabel.setId("data");

        dataSize = new Input();
        dataSize.setPlaceholder("Data size");
        dataSize.setId("data-size");

        interval = new Input();
        interval.setPlaceholder("Interval (ms)");
        interval.setId("interval");

        duration = new Input();
        duration.setPlaceholder("Duration (ms)");
        duration.setId("duration");

        dataSize.setValue(DEFAULT_SIZE_BYTES + "");
        interval.setValue(DEFAULT_DELAY_MS + "");
        duration.setValue(DEFAULT_DURATION_MS + "");

        add(dataSize, interval, duration);

        NativeButton button = new NativeButton("Start pushing");
        button.setId("startButton");
        button.addClickListener(event -> {
            Integer pushSize = Integer.parseInt(dataSize.getValue());
            Integer pushInterval = Integer.parseInt(interval.getValue());
            Integer pushDuration = Integer.parseInt(duration.getValue());
            PushRunnable runnable = new PushRunnable(this, pushSize,
                    pushInterval, pushDuration);
            executor.execute(runnable);
            log("Starting push, size: " + pushSize + ", interval: "
                    + pushInterval + "ms, duration: " + pushDuration + "ms");
        });
        add(button, dataLabel);
    }

    public Div getDataLabel() {
        return dataLabel;
    }

    public static class PushRunnable implements Runnable {

        private Integer size;
        private Integer interval;
        private Integer duration;
        private final PushLargeData pushLargeData;

        public PushRunnable(PushLargeData pushLargeData, Integer size,
                Integer interval, Integer duration) {
            this.size = size;
            this.interval = interval;
            this.duration = duration;
            this.pushLargeData = pushLargeData;
        }

        @Override
        public void run() {
            final long endTime = System.currentTimeMillis() + duration;
            final String data = LoremIpsum.get(size);
            int packageIndex = 1;
            while (System.currentTimeMillis() < endTime) {
                final int idx = packageIndex++;
                pushLargeData.getUI().get().access(() -> {
                    // Using description as it is not rendered to the DOM
                    // immediately
                    pushLargeData.getDataLabel()
                            .setText(System.currentTimeMillis() + ": " + data);
                    pushLargeData.log("Package " + idx + " pushed");
                });
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    return;
                }
            }
            pushLargeData.getUI().get().access(() -> {
                pushLargeData.log("Push complete");
            });

        }
    }
}

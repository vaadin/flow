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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.Transport;

/**
 * Resynchronization when a message pushed through SSE_PURE is lost, also while
 * more messages are pushed.
 */
@Route("com.vaadin.flow.uitest.ui.ResynchronizationSsePureView")
@Push(transport = Transport.SSE_PURE)
public class ResynchronizationSsePureView extends AbstractDivView {
    static final String LOSE_PUSH_BUTTON = "lose-push";
    static final String LOSE_PUSH_WHILE_PUSHING_BUTTON = "lose-push-while-pushing";
    static final String PUSHED_CLASS = "pushed";
    static final String COUNTER_ID = "counter";
    static final int PUSHES = 20;

    private final Span counter = new Span("0");

    public ResynchronizationSsePureView() {
        counter.setId(COUNTER_ID);
        add(counter);
        add(createButton("Lose a pushed message", LOSE_PUSH_BUTTON,
                e -> push(1, 0)));
        add(createButton("Lose a pushed message while pushing",
                LOSE_PUSH_WHILE_PUSHING_BUTTON, e -> push(PUSHES, PUSHES / 2)));
    }

    /**
     * Pushes the given number of messages from a background thread, the given
     * one of which the client sees as lost and resynchronizes.
     */
    private void push(int count, int lost) {
        UI ui = getUI().orElseThrow();
        Thread.ofVirtual().start(() -> {
            for (int i = 0; i < count; i++) {
                int number = i + 1;
                ui.access(() -> {
                    counter.setText(String.valueOf(number));
                    if (number - 1 == lost) {
                        Span pushed = new Span("pushed");
                        pushed.addClassName(PUSHED_CLASS);
                        add(pushed);
                        // The message skips an id, as if one was lost
                        ui.getInternals().incrementServerId();
                    }
                });
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
    }
}

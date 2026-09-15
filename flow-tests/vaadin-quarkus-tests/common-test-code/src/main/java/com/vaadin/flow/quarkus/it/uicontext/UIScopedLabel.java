/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.quarkus.it.uicontext;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.quarkus.it.Counter;
import com.vaadin.quarkus.annotation.UIScoped;

@UIScoped
public class UIScopedLabel extends Span {

    public static final String DESTROY_COUNT = "UIScopedLabelDestroy";

    @Inject
    private Counter counter;

    private int uiId;

    public static final String ID = "UISCOPED_LABEL";

    public UIScopedLabel() {
        setId(ID);
        uiId = UI.getCurrent().getUIId();
    }

    @PreDestroy
    private void destroy() {
        counter.increment(DESTROY_COUNT + uiId);
    }

    void onSetText(@Observes SetTextEvent event) {
        setText(event.getText());
    }

    public static class SetTextEvent {
        private final String text;

        public SetTextEvent(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }
}

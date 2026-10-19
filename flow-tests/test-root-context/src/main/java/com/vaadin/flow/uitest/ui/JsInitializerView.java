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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.JsInitializerView", layout = ViewTestLayout.class)
public class JsInitializerView extends AbstractDivView {

    private final Div target = new Div();
    private final Span initCounter = new Span("0");
    private final Span cleanupCounter = new Span("0");

    private Registration registration;

    public JsInitializerView() {
        target.setId("target");
        initCounter.setId("initCounter");
        cleanupCounter.setId("cleanupCounter");

        NativeButton register = new NativeButton("Register", e -> register());
        register.setId("register");

        NativeButton sameRequestReattach = new NativeButton(
                "Same request reattach", e -> {
                    remove(target);
                    add(target);
                });
        sameRequestReattach.setId("sameRequestReattach");

        NativeButton detach = new NativeButton("Detach", e -> remove(target));
        detach.setId("detach");

        NativeButton reattach = new NativeButton("Reattach", e -> add(target));
        reattach.setId("reattach");

        NativeButton removeRegistration = new NativeButton("Remove", e -> {
            if (registration != null) {
                registration.remove();
                registration = null;
            }
        });
        removeRegistration.setId("removeRegistration");

        add(register, sameRequestReattach, detach, reattach, removeRegistration,
                target, initCounter, cleanupCounter);
    }

    private void register() {
        if (registration != null) {
            return;
        }
        // The initializer bumps the init counter on each install and returns
        // a cleanup that bumps the cleanup counter when invoked.
        registration = target.getElement().addJsInitializer(
                """
                        const initEl = $0;
                        const cleanupEl = $1;
                        initEl.textContent = String(parseInt(initEl.textContent, 10) + 1);
                        return () => {
                            cleanupEl.textContent = String(parseInt(cleanupEl.textContent, 10) + 1);
                        };
                        """,
                initCounter.getElement(), cleanupCounter.getElement());
    }
}

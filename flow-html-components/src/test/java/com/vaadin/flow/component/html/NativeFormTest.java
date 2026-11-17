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
package com.vaadin.flow.component.html;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.NativeForm.Enctype;
import com.vaadin.flow.component.html.NativeForm.Method;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NativeFormTest extends ComponentTest {

    @Override
    protected void addProperties() {
        // nullable getters, covered by the tests below
        whitelistProperty("action");
        whitelistProperty("method");
        whitelistProperty("enctype");
        whitelistProperty("target");
    }

    @AfterEach
    void clearCurrentUi() {
        UI.setCurrent(null);
    }

    @Test
    void setAction_writesAttribute() {
        NativeForm form = new NativeForm();
        assertNull(form.getAction());

        form.setAction("login");
        assertEquals("login", form.getAction());
        assertEquals("login", form.getElement().getAttribute("action"));

        form.setAction(null);
        assertNull(form.getAction());
        assertNull(form.getElement().getAttribute("action"));
    }

    @Test
    void newForm_usesPost() {
        assertEquals(Method.POST, new NativeForm().getMethod());
        assertEquals(Method.POST, new NativeForm(new Input()).getMethod());
        assertEquals("post",
                new NativeForm().getElement().getAttribute("method"));
    }

    @Test
    void setMethodAndEnctype_writeAttributeValues() {
        NativeForm form = new NativeForm();
        assertNull(form.getEnctype());

        form.setMethod(Method.POST);
        form.setEnctype(Enctype.MULTIPART);
        assertEquals("post", form.getElement().getAttribute("method"));
        assertEquals("multipart/form-data",
                form.getElement().getAttribute("enctype"));
        assertEquals(Method.POST, form.getMethod());
        assertEquals(Enctype.MULTIPART, form.getEnctype());

        form.setMethod(null);
        form.setEnctype(null);
        assertNull(form.getElement().getAttribute("method"));
        assertNull(form.getElement().getAttribute("enctype"));
    }

    @Test
    void getMethodAndEnctype_ignoreCaseOfAttribute() {
        NativeForm form = new NativeForm();
        form.getElement().setAttribute("method", "POST");
        assertEquals(Method.POST, form.getMethod());

        form.getElement().setAttribute("enctype", "MULTIPART/FORM-DATA");
        assertEquals(Enctype.MULTIPART, form.getEnctype());

        form.getElement().setAttribute("method", "put");
        form.getElement().setAttribute("enctype", "application/json");
        assertNull(form.getMethod());
        assertNull(form.getEnctype());
    }

    @Test
    void setAction_attached_unsafeScheme_throws() {
        NativeForm form = new NativeForm();
        createUi().add(form);

        assertThrows(IllegalArgumentException.class,
                () -> form.setAction("javascript:alert(1)"));
    }

    @Test
    void setAction_detached_unsafeScheme_throwsOnAttachAndClearsAction() {
        NativeForm form = new NativeForm();
        form.setAction("javascript:alert(1)");

        UI ui = createUi();
        assertThrows(IllegalArgumentException.class, () -> ui.add(form));
        assertFalse(form.getElement().hasAttribute("action"));
    }

    @Test
    void setAction_null_cancelsValidationOnAttach() {
        NativeForm form = new NativeForm();
        form.setAction("javascript:alert(1)");
        form.setAction(null);

        createUi().add(form);

        assertNull(form.getAction());
    }

    @Test
    void setUnsafeAction_skipsValidation() {
        NativeForm form = new NativeForm();
        form.setAction("javascript:alert(1)");
        form.setUnsafeAction("javascript:void(0)");

        createUi().add(form);

        assertEquals("javascript:void(0)", form.getAction());
    }

    @Test
    void setTarget_writesAttribute() {
        NativeForm form = new NativeForm();
        assertNull(form.getTarget());

        form.setTarget(AnchorTarget.BLANK);
        assertEquals("_blank", form.getElement().getAttribute("target"));
        assertEquals(AnchorTarget.BLANK, form.getTarget());

        form.setTarget(AnchorTargetValue.forString("result-frame"));
        assertEquals("result-frame", form.getTarget().getValue());

        form.setTarget(AnchorTarget.DEFAULT);
        assertNull(form.getElement().getAttribute("target"));

        form.setTarget(AnchorTarget.SELF);
        form.setTarget(null);
        assertNull(form.getTarget());
    }

    @Test
    void addNestedForm_throws() {
        Input input = new Input();
        NativeForm form = new NativeForm(input);
        NativeForm nested = new NativeForm();
        Div div = new Div();

        assertThrows(IllegalArgumentException.class,
                () -> form.add(div, nested));
        assertThrows(IllegalArgumentException.class,
                () -> form.addComponentAsFirst(nested));
        assertThrows(IllegalArgumentException.class,
                () -> form.replace(input, nested));
        assertThrows(IllegalArgumentException.class,
                () -> new NativeForm(div, nested));
        assertEquals(1, form.getComponentCount());
        assertNull(nested.getParent().orElse(null));
        assertNull(div.getParent().orElse(null));
    }

    @Test
    void addFormInsideOtherChild_allowed() {
        Div wrapper = new Div(new NativeForm());
        NativeForm form = new NativeForm(wrapper);

        assertEquals(wrapper, form.getComponentAt(0));
    }

    @Test
    void bindChildren_factoryCreatesForm_reportsErrorAndSkipsIt() {
        AlwaysLockedVaadinSession session = new AlwaysLockedVaadinSession(
                new MockVaadinServletService());
        List<ErrorEvent> errors = new ArrayList<>();
        session.setErrorHandler(errors::add);
        UI ui = new MockUI(session);
        try {
            NativeForm form = new NativeForm();
            ui.add(form);
            ListSignal<String> values = new ListSignal<>();
            form.bindChildren(values,
                    value -> "form".equals(value.peek()) ? new NativeForm()
                            : new Span(value.peek()));

            values.insertLast("field");
            assertEquals(1, form.getComponentCount());

            values.insertLast("form");

            // the binding runs in an effect, which reports the failure to the
            // session error handler instead of throwing to the caller
            assertEquals(1, errors.size());
            assertInstanceOf(IllegalArgumentException.class,
                    errors.get(0).getThrowable());
            assertEquals(1, form.getComponentCount());
        } finally {
            CurrentInstance.clearAll();
        }
    }

    private static UI createUi() {
        VaadinSession session = new AlwaysLockedVaadinSession(
                new MockVaadinServletService(
                        new MockDeploymentConfiguration()));
        UI ui = new MockUI(session);
        // Make the configuration be found through the UI only
        CurrentInstance.clearAll();
        return ui;
    }
}

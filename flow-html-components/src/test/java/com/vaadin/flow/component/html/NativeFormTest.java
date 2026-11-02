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

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.html.NativeForm.Enctype;
import com.vaadin.flow.component.html.NativeForm.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NativeFormTest extends ComponentTest {

    @Override
    protected void addProperties() {
        // nullable getters, covered by the tests below
        whitelistProperty("action");
        whitelistProperty("method");
        whitelistProperty("enctype");
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
    void setMethodAndEnctype_writeAttributeValues() {
        NativeForm form = new NativeForm();
        assertNull(form.getMethod());
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
    void addNestedForm_throws() {
        Input input = new Input();
        NativeForm form = new NativeForm(input);
        NativeForm nested = new NativeForm();
        Div div = new Div();

        assertThrows(IllegalArgumentException.class, () -> form.add(nested));
        assertThrows(IllegalArgumentException.class,
                () -> form.addComponentAsFirst(nested));
        assertThrows(IllegalArgumentException.class,
                () -> form.replace(input, nested));
        assertThrows(IllegalArgumentException.class,
                () -> new NativeForm(div, nested));
        assertEquals(1, form.getComponentCount());
        assertNull(nested.getParent().orElse(null));
    }

    @Test
    void addFormInsideOtherChild_allowed() {
        Div wrapper = new Div(new NativeForm());
        NativeForm form = new NativeForm(wrapper);

        assertEquals(wrapper, form.getComponentAt(0));
    }
}

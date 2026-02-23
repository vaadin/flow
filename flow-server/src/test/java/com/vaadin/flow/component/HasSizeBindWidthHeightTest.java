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
package com.vaadin.flow.component;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HasSizeBindWidthHeightTest extends SignalsUnitTest {

    @Tag("div")
    public static class HasSizeComponent extends Component implements HasSize {
    }

    @Test
    public void bindWidth_elementAttachedBefore_bindingActive() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        assertNull(component.getWidth());

        ValueSignal<String> signal = new ValueSignal<>("200px");
        component.bindWidth(signal);

        assertEquals("200px", component.getWidth());
    }

    @Test
    public void bindWidth_elementAttachedAfter_bindingActive() {
        HasSizeComponent component = new HasSizeComponent();
        assertNull(component.getWidth());

        ValueSignal<String> signal = new ValueSignal<>("200px");
        component.bindWidth(signal);
        UI.getCurrent().add(component);

        assertEquals("200px", component.getWidth());
    }

    @Test
    public void bindWidth_elementAttached_bindingActive() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("200px");
        component.bindWidth(signal);

        // initially "200px"
        assertEquals("200px", component.getWidth());

        // "200px" -> "300px"
        signal.set("300px");
        assertEquals("300px", component.getWidth());

        signal.set(null);
        assertNull(component.getWidth());
    }

    @Test
    public void bindWidth_elementNotAttached_bindingInactive() {
        HasSizeComponent component = new HasSizeComponent();
        ValueSignal<String> signal = new ValueSignal<>("200px");
        component.bindWidth(signal);
        signal.set("300px");

        assertNull(component.getWidth());
    }

    @Test
    public void bindWidth_elementDetached_bindingInactive() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("200px");
        component.bindWidth(signal);
        component.removeFromParent();
        signal.set("300px"); // ignored

        assertEquals("200px", component.getWidth());
    }

    @Test
    public void bindWidth_elementReAttached_bindingActivate() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("200px");
        component.bindWidth(signal);
        component.removeFromParent();
        signal.set("300px");
        UI.getCurrent().add(component);

        assertEquals("300px", component.getWidth());
    }

    @Test
    public void bindWidth_setWidthWhileBindingIsActive_throwException() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        component.bindWidth(new ValueSignal<>("200px"));

        assertThrows(BindingActiveException.class,
                () -> component.setWidth("300px"));
        assertThrows(BindingActiveException.class, component::setWidthFull);
        assertThrows(BindingActiveException.class,
                () -> component.setWidth(300, Unit.PIXELS));
        assertThrows(BindingActiveException.class, component::setSizeFull);
        assertThrows(BindingActiveException.class,
                () -> component.getElement().getStyle().setWidth("300px"));
        assertThrows(BindingActiveException.class, () -> component.getElement()
                .setAttribute(Constants.ATTRIBUTE_WIDTH_FULL, true));
        assertEquals("200px", component.getWidth());
    }

    @Test
    public void bindWidth_bindWidthWhileBindingIsActive_throwException() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        component.bindWidth(new ValueSignal<>("200px"));

        assertThrows(BindingActiveException.class,
                () -> component.bindWidth(new ValueSignal<>("300px")));
        assertThrows(BindingActiveException.class,
                () -> component.setWidth("300px"));
        assertEquals("200px", component.getWidth());
    }

    @Test
    public void bindWidth_nullSignal_throwsNPE() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);

        assertThrows(NullPointerException.class,
                () -> component.bindWidth(null));
    }

    @Test
    public void bindWidth_fullWidth_widthFullAttributeSet() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("100%");
        component.bindWidth(signal);

        assertEquals("100%", component.getWidth());
        assertEquals("", component.getElement()
                .getAttribute(Constants.ATTRIBUTE_WIDTH_FULL));
    }

    @Test
    public void bindWidth_notFullWidth_widthFullAttributeNotSet() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("200px");
        component.bindWidth(signal);

        assertEquals("200px", component.getWidth());
        assertNull(component.getElement()
                .getAttribute(Constants.ATTRIBUTE_WIDTH_FULL));
    }

    @Test
    public void bindWidth_changeFromFullWidthToOther_widthFullAttributeRemoved() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("100%");
        component.bindWidth(signal);

        assertEquals("", component.getElement()
                .getAttribute(Constants.ATTRIBUTE_WIDTH_FULL));

        signal.set("200px");
        assertEquals("200px", component.getWidth());
        assertNull(component.getElement()
                .getAttribute(Constants.ATTRIBUTE_WIDTH_FULL));
    }

    @Test
    public void bindWidth_changeFromOtherToFullWidth_widthFullAttributeSet() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("200px");
        component.bindWidth(signal);

        assertNull(component.getElement()
                .getAttribute(Constants.ATTRIBUTE_WIDTH_FULL));

        signal.set("100%");
        assertEquals("100%", component.getWidth());
        assertEquals("", component.getElement()
                .getAttribute(Constants.ATTRIBUTE_WIDTH_FULL));
    }

    @Test
    public void bindHeight_elementAttachedBefore_bindingActive() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        assertNull(component.getHeight());

        ValueSignal<String> signal = new ValueSignal<>("200px");
        component.bindHeight(signal);

        assertEquals("200px", component.getHeight());
    }

    @Test
    public void bindHeight_elementAttachedAfter_bindingActive() {
        HasSizeComponent component = new HasSizeComponent();
        assertNull(component.getHeight());

        ValueSignal<String> signal = new ValueSignal<>("200px");
        component.bindHeight(signal);
        UI.getCurrent().add(component);

        assertEquals("200px", component.getHeight());
    }

    @Test
    public void bindHeight_elementAttached_bindingActive() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("200px");
        component.bindHeight(signal);

        // initially "200px"
        assertEquals("200px", component.getHeight());

        // "200px" -> "300px"
        signal.set("300px");
        assertEquals("300px", component.getHeight());

        signal.set(null);
        assertNull(component.getHeight());
    }

    @Test
    public void bindHeight_elementNotAttached_bindingInactive() {
        HasSizeComponent component = new HasSizeComponent();
        ValueSignal<String> signal = new ValueSignal<>("200px");
        component.bindHeight(signal);
        signal.set("300px");

        assertNull(component.getHeight());
    }

    @Test
    public void bindHeight_elementDetached_bindingInactive() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("200px");
        component.bindHeight(signal);
        component.removeFromParent();
        signal.set("300px"); // ignored

        assertEquals("200px", component.getHeight());
    }

    @Test
    public void bindHeight_elementReAttached_bindingActivate() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("200px");
        component.bindHeight(signal);
        component.removeFromParent();
        signal.set("300px");
        UI.getCurrent().add(component);

        assertEquals("300px", component.getHeight());
    }

    @Test
    public void bindHeight_setHeightWhileBindingIsActive_throwException() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        component.bindHeight(new ValueSignal<>("200px"));

        assertThrows(BindingActiveException.class,
                () -> component.setHeight("300px"));
        assertThrows(BindingActiveException.class, component::setHeightFull);
        assertThrows(BindingActiveException.class,
                () -> component.setHeight(300, Unit.PIXELS));
        assertThrows(BindingActiveException.class, component::setSizeFull);
        assertThrows(BindingActiveException.class,
                () -> component.getElement().getStyle().setHeight("300px"));
        assertThrows(BindingActiveException.class, () -> component.getElement()
                .setAttribute(Constants.ATTRIBUTE_HEIGHT_FULL, true));
        assertEquals("200px", component.getHeight());
    }

    @Test
    public void bindHeight_bindHeightWhileBindingIsActive_throwException() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        component.bindHeight(new ValueSignal<>("200px"));

        assertThrows(BindingActiveException.class,
                () -> component.bindHeight(new ValueSignal<>("300px")));
        assertEquals("200px", component.getHeight());
    }

    @Test
    public void bindHeight_nullSignal_throwsNPE() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);

        assertThrows(NullPointerException.class,
                () -> component.bindHeight(null));
    }

    @Test
    public void bindHeight_fullHeight_heightFullAttributeSet() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("100%");
        component.bindHeight(signal);

        assertEquals("100%", component.getHeight());
        assertEquals("", component.getElement()
                .getAttribute(Constants.ATTRIBUTE_HEIGHT_FULL));
    }

    @Test
    public void bindHeight_notFullHeight_heightFullAttributeNotSet() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("200px");
        component.bindHeight(signal);

        assertEquals("200px", component.getHeight());
        assertNull(component.getElement()
                .getAttribute(Constants.ATTRIBUTE_HEIGHT_FULL));
    }

    @Test
    public void bindHeight_changeFromFullHeightToOther_heightFullAttributeRemoved() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("100%");
        component.bindHeight(signal);

        assertEquals("", component.getElement()
                .getAttribute(Constants.ATTRIBUTE_HEIGHT_FULL));

        signal.set("200px");
        assertEquals("200px", component.getHeight());
        assertNull(component.getElement()
                .getAttribute(Constants.ATTRIBUTE_HEIGHT_FULL));
    }

    @Test
    public void bindHeight_changeFromOtherToFullHeight_heightFullAttributeSet() {
        HasSizeComponent component = new HasSizeComponent();
        UI.getCurrent().add(component);
        ValueSignal<String> signal = new ValueSignal<>("200px");
        component.bindHeight(signal);

        assertNull(component.getElement()
                .getAttribute(Constants.ATTRIBUTE_HEIGHT_FULL));

        signal.set("100%");
        assertEquals("100%", component.getHeight());
        assertEquals("", component.getElement()
                .getAttribute(Constants.ATTRIBUTE_HEIGHT_FULL));
    }
}

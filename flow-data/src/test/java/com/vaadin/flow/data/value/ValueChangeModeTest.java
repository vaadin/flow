package com.vaadin.flow.data.value;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;

public class ValueChangeModeTest {

    @Tag("tag")
    private static class ValueChangeModeField
            extends AbstractSinglePropertyField<ValueChangeModeField, String>
            implements HasValueChangeMode {
        private ValueChangeMode valueChangeMode;

        public ValueChangeModeField() {
            super("value", "", false);
        }

        @Override
        public void setValueChangeMode(ValueChangeMode valueChangeMode) {
            this.valueChangeMode = valueChangeMode;

            setSynchronizedEvent(ValueChangeMode.eventForMode(valueChangeMode,
                    "value-changed"));
        }

        @Override
        public ValueChangeMode getValueChangeMode() {
            return valueChangeMode;
        }
    }

    @Test
    public void field_setMode() {
        ValueChangeModeField field = new ValueChangeModeField();

        field.setValueChangeMode(ValueChangeMode.ON_BLUR);
        assertValueSynchronizedWithEvent(field, "blur");

        field.setValueChangeMode(null);
        Assert.assertEquals(0,
                field.getElement().getSynchronizedPropertyEvents().count());

        field.setValueChangeMode(ValueChangeMode.EAGER);
        assertValueSynchronizedWithEvent(field, "value-changed");
    }

    private void assertValueSynchronizedWithEvent(Component component,
            String eventName) {

        Assert.assertArrayEquals(
                "value should be the only synchronized property",
                new String[] { "value" }, component.getElement()
                        .getSynchronizedProperties().toArray(String[]::new));

        String[] syncedPropertyEvents = component.getElement()
                .getSynchronizedPropertyEvents().toArray(String[]::new);
        String[] expectedPropertyEvents = { eventName };

        Assert.assertArrayEquals(
                eventName + " should be the only synchronized property-event",
                expectedPropertyEvents, syncedPropertyEvents);
    }

}

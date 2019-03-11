package com.vaadin.flow.data.value;

import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.Tag;

import java.util.EnumSet;

public class ValueChangeModeTest {

    @Tag("tag")
    private static class ValueChangeModeField
            extends AbstractSinglePropertyField<ValueChangeModeField, String>
            implements HasValueChangeMode {
        private ValueChangeMode valueChangeMode;
        private int valueChangeTimeout;

        public ValueChangeModeField() {
            super("value", "", false);
        }

        @Override
        public void setValueChangeMode(ValueChangeMode valueChangeMode) {
            this.valueChangeMode = valueChangeMode;

            setSynchronizedEvent(ValueChangeMode.eventForMode(valueChangeMode,
                    "value-changed"));
            setValueChangeTimeout(valueChangeTimeout);
        }

        @Override
        public ValueChangeMode getValueChangeMode() {
            return valueChangeMode;
        }

        @Override
        public void setValueChangeTimeout(int valueChangeTimeout) {
            this.valueChangeTimeout = valueChangeTimeout;
            ValueChangeMode.applyChangeTimeout(valueChangeMode, this.valueChangeTimeout,
                    getSynchronizationRegistration());
        }

        @Override
        public int getValueChangeTimeout() {
            return valueChangeTimeout;
        }

        // Exposed as public for testing purposes
        @Override
        public DomListenerRegistration getSynchronizationRegistration() {
            return super.getSynchronizationRegistration();
        }
    }

    @Test
    public void field_setMode() {
        ValueChangeModeField field = new ValueChangeModeField();

        field.setValueChangeMode(ValueChangeMode.ON_BLUR);
        assertValueSynchronizedWithEvent(field, "blur");

        field.setValueChangeMode(null);
        Assert.assertNull(
                "value should not be a synchronized property",
                getDisabledUpdateMode(field));
        Assert.assertNull(field.getSynchronizationRegistration());

        field.setValueChangeMode(ValueChangeMode.EAGER);
        assertValueSynchronizedWithEvent(field, "value-changed");

        field.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        assertValueSynchronizedWithEvent(field, "change");

        field.setValueChangeMode(ValueChangeMode.LAZY);
        assertValueSynchronizedWithEvent(field, "value-changed");

        field.setValueChangeMode(ValueChangeMode.TIMEOUT);
        assertValueSynchronizedWithEvent(field, "value-changed");
    }

    @Test
    public void field_setValueChangeTimeout_applied() {
        ValueChangeModeField field = new ValueChangeModeField();
        assertDebounceDisabled(field);

        field.setValueChangeMode(ValueChangeMode.LAZY);
        assertDebounceDisabled(field);

        field.setValueChangeTimeout(123);
        assertDebounceEquals(field, EnumSet.of(DebouncePhase.TRAILING), 123);

        field.setValueChangeMode(ValueChangeMode.TIMEOUT);
        assertDebounceEquals(field,
                EnumSet.of(DebouncePhase.LEADING, DebouncePhase.INTERMEDIATE), 123);

        field.setValueChangeTimeout(456);
        assertDebounceEquals(field,
                EnumSet.of(DebouncePhase.LEADING, DebouncePhase.INTERMEDIATE), 456);
    }

    @Test
    public void field_setValueChangeTimeout_ignored() {
        assertNoTimeoutApplied(ValueChangeMode.EAGER);
        assertNoTimeoutApplied(ValueChangeMode.ON_CHANGE);
        assertNoTimeoutApplied(ValueChangeMode.ON_BLUR);
    }

    private void assertNoTimeoutApplied(ValueChangeMode valueChangeMode) {
        ValueChangeModeField field = new ValueChangeModeField();
        field.setValueChangeMode(valueChangeMode);
        assertDebounceDisabled(field);
    }

    private void assertDebounceEquals(ValueChangeModeField field,
                                      EnumSet<DebouncePhase> phases, int timeout) {
        DomListenerRegistration reg = field.getSynchronizationRegistration();
        Assert.assertEquals(timeout, reg.getDebounceTimeout());
        Assert.assertEquals(phases, reg.getDebouncePhases());
    }

    private void assertDebounceDisabled(ValueChangeModeField field) {
        Assert.assertEquals(0,
                field.getSynchronizationRegistration().getDebounceTimeout());
    }

    private void assertValueSynchronizedWithEvent(ValueChangeModeField field,
            String eventName) {

        Assert.assertNotNull(
                "value should be a synchronized property",
                getDisabledUpdateMode(field));

        DomListenerRegistration reg = field.getSynchronizationRegistration();

        Assert.assertEquals(
                eventName + " should be the synchronized property-event",
                eventName, reg.getEventType());
    }

    private DisabledUpdateMode getDisabledUpdateMode(ValueChangeModeField field) {
        return field.getElement().getNode()
                .getFeature(ElementListenerMap.class)
                .getPropertySynchronizationMode("value");
    }

}

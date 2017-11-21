package com.vaadin.generator.registry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the {@link ExclusionRegistry}.
 *
 */
public class ExclusionRegistryTest {

    @Test
    public void excludeEvents() {
        ExclusionRegistry.excludeEvent("some-tag", "someEvent");
        ExclusionRegistry.excludeEvent(null, "someOtherEvent");

        assertTrue(
                ExclusionRegistry.isEventExcluded("some-tag", "someEvent"));
        assertTrue(ExclusionRegistry.isEventExcluded("some-tag",
                "someOtherEvent"));
        assertTrue(ExclusionRegistry.isEventExcluded("some-other-tag",
                "someOtherEvent"));

        assertFalse(
                ExclusionRegistry.isPropertyExcluded("some-tag", "someEvent"));
        assertFalse(ExclusionRegistry.isPropertyExcluded("some-tag",
                "someOtherEvent"));
        assertFalse(
                ExclusionRegistry.isMethodExcluded("some-tag", "someEvent"));
        assertFalse(ExclusionRegistry.isMethodExcluded("some-tag",
                "someOtherEvent"));
    }

    @Test
    public void excludeProperties() {
        ExclusionRegistry.excludeProperty("some-tag", "someProperty");
        ExclusionRegistry.excludeProperty(null, "someOtherProperty");

        assertTrue(ExclusionRegistry.isPropertyExcluded("some-tag",
                "someProperty"));
        assertTrue(ExclusionRegistry.isPropertyExcluded("some-tag",
                "someOtherProperty"));
        assertTrue(ExclusionRegistry.isPropertyExcluded("some-other-tag",
                "someOtherProperty"));

        assertFalse(
                ExclusionRegistry.isEventExcluded("some-tag", "someProperty"));
        assertFalse(ExclusionRegistry.isEventExcluded("some-tag",
                "someOtherProperty"));
        assertFalse(
                ExclusionRegistry.isMethodExcluded("some-tag", "someProperty"));
        assertFalse(ExclusionRegistry.isMethodExcluded("some-tag",
                "someOtherProperty"));
    }

    @Test
    public void excludeMethods() {
        ExclusionRegistry.excludeMethod("some-tag", "someMethod");
        ExclusionRegistry.excludeMethod(null, "someOtherMethod");

        assertTrue(
                ExclusionRegistry.isMethodExcluded("some-tag", "someMethod"));
        assertTrue(ExclusionRegistry.isMethodExcluded("some-tag",
                "someOtherMethod"));
        assertTrue(ExclusionRegistry.isMethodExcluded("some-other-tag",
                "someOtherMethod"));

        assertFalse(
                ExclusionRegistry.isEventExcluded("some-tag", "someMethod"));
        assertFalse(
                ExclusionRegistry.isEventExcluded("some-tag", "someMethod"));
        assertFalse(ExclusionRegistry.isPropertyExcluded("some-tag",
                "someOtherMethod"));
        assertFalse(ExclusionRegistry.isPropertyExcluded("some-tag",
                "someOtherMethod"));
    }

}

package com.vaadin.generator.registry;

import org.junit.Assert;
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

        Assert.assertTrue(
                ExclusionRegistry.isEventExcluded("some-tag", "someEvent"));
        Assert.assertTrue(ExclusionRegistry.isEventExcluded("some-tag",
                "someOtherEvent"));
        Assert.assertTrue(ExclusionRegistry.isEventExcluded("some-other-tag",
                "someOtherEvent"));

        Assert.assertFalse(
                ExclusionRegistry.isPropertyExcluded("some-tag", "someEvent"));
        Assert.assertFalse(ExclusionRegistry.isPropertyExcluded("some-tag",
                "someOtherEvent"));
        Assert.assertFalse(
                ExclusionRegistry.isMethodExcluded("some-tag", "someEvent"));
        Assert.assertFalse(ExclusionRegistry.isMethodExcluded("some-tag",
                "someOtherEvent"));
    }

    @Test
    public void excludeProperties() {
        ExclusionRegistry.excludeProperty("some-tag", "someProperty");
        ExclusionRegistry.excludeProperty(null, "someOtherProperty");

        Assert.assertTrue(ExclusionRegistry.isPropertyExcluded("some-tag",
                "someProperty"));
        Assert.assertTrue(ExclusionRegistry.isPropertyExcluded("some-tag",
                "someOtherProperty"));
        Assert.assertTrue(ExclusionRegistry.isPropertyExcluded("some-other-tag",
                "someOtherProperty"));

        Assert.assertFalse(
                ExclusionRegistry.isEventExcluded("some-tag", "someProperty"));
        Assert.assertFalse(ExclusionRegistry.isEventExcluded("some-tag",
                "someOtherProperty"));
        Assert.assertFalse(
                ExclusionRegistry.isMethodExcluded("some-tag", "someProperty"));
        Assert.assertFalse(ExclusionRegistry.isMethodExcluded("some-tag",
                "someOtherProperty"));
    }

    @Test
    public void excludeMethods() {
        ExclusionRegistry.excludeMethod("some-tag", "someMethod");
        ExclusionRegistry.excludeMethod(null, "someOtherMethod");

        Assert.assertTrue(
                ExclusionRegistry.isMethodExcluded("some-tag", "someMethod"));
        Assert.assertTrue(ExclusionRegistry.isMethodExcluded("some-tag",
                "someOtherMethod"));
        Assert.assertTrue(ExclusionRegistry.isMethodExcluded("some-other-tag",
                "someOtherMethod"));

        Assert.assertFalse(
                ExclusionRegistry.isEventExcluded("some-tag", "someMethod"));
        Assert.assertFalse(
                ExclusionRegistry.isEventExcluded("some-tag", "someMethod"));
        Assert.assertFalse(ExclusionRegistry.isPropertyExcluded("some-tag",
                "someOtherMethod"));
        Assert.assertFalse(ExclusionRegistry.isPropertyExcluded("some-tag",
                "someOtherMethod"));
    }

}

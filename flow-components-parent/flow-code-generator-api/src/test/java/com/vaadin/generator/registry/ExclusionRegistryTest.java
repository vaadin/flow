package com.vaadin.generator.registry;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.HasStyle;

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
        Assert.assertFalse(ExclusionRegistry
                .isBehaviorOrMixinExcluded("some-tag", "someEvent"));
        Assert.assertFalse(ExclusionRegistry
                .isBehaviorOrMixinExcluded("some-tag", "someOtherEvent"));
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
        Assert.assertFalse(ExclusionRegistry
                .isBehaviorOrMixinExcluded("some-tag", "someProperty"));
        Assert.assertFalse(ExclusionRegistry
                .isBehaviorOrMixinExcluded("some-tag", "someOtherProperty"));
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
        Assert.assertFalse(ExclusionRegistry.isEventExcluded("some-tag",
                "someOtherMethod"));
        Assert.assertFalse(
                ExclusionRegistry.isPropertyExcluded("some-tag", "someMethod"));
        Assert.assertFalse(ExclusionRegistry.isPropertyExcluded("some-tag",
                "someOtherMethod"));
        Assert.assertFalse(ExclusionRegistry
                .isBehaviorOrMixinExcluded("some-tag", "someMethod"));
        Assert.assertFalse(ExclusionRegistry
                .isBehaviorOrMixinExcluded("some-tag", "someOtherMethod"));
    }

    @Test
    public void excludeBehaviors() {
        ExclusionRegistry.excludeBehaviorOrMixin("some-tag", "someBehavior");
        ExclusionRegistry.excludeBehaviorOrMixin(null, "someOtherBehavior");

        Assert.assertTrue(ExclusionRegistry
                .isBehaviorOrMixinExcluded("some-tag", "someBehavior"));
        Assert.assertTrue(ExclusionRegistry
                .isBehaviorOrMixinExcluded("some-tag", "someOtherBehavior"));
        Assert.assertTrue(ExclusionRegistry.isBehaviorOrMixinExcluded(
                "some-other-tag", "someOtherBehavior"));

        Assert.assertFalse(
                ExclusionRegistry.isEventExcluded("some-tag", "someBehavior"));
        Assert.assertFalse(ExclusionRegistry.isEventExcluded("some-tag",
                "someOtherBehavior"));
        Assert.assertFalse(
                ExclusionRegistry.isMethodExcluded("some-tag", "someBehavior"));
        Assert.assertFalse(ExclusionRegistry.isMethodExcluded("some-tag",
                "someOtherBehavior"));
        Assert.assertFalse(ExclusionRegistry.isPropertyExcluded("some-tag",
                "someBehavior"));
        Assert.assertFalse(ExclusionRegistry.isPropertyExcluded("some-tag",
                "someOtherBehavior"));

    }

    @Test
    public void excludeInterface() {
        ExclusionRegistry.excludeInterface("some-tag", HasStyle.class);
        ExclusionRegistry.excludeInterface(null, HasEnabled.class);

        Assert.assertTrue(ExclusionRegistry.isInterfaceExcluded("some-tag",
                HasStyle.class));
        Assert.assertTrue(ExclusionRegistry.isInterfaceExcluded("some-tag",
                HasEnabled.class));
        Assert.assertTrue(ExclusionRegistry
                .isInterfaceExcluded("some-other-tag", HasEnabled.class));
        Assert.assertFalse(ExclusionRegistry
                .isInterfaceExcluded("some-other-tag", HasStyle.class));
    }

    @Test
    public void excludeTag() {
        ExclusionRegistry.excludeTag("some-tag");
        Assert.assertTrue(ExclusionRegistry.isTagExcluded("some-tag"));
        Assert.assertFalse(ExclusionRegistry.isTagExcluded("some-other-tag"));
    }

    @Test(expected = NullPointerException.class)
    public void excludeTagWithNullValue() {
        ExclusionRegistry.excludeTag(null);
    }

}

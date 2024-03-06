/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public class PrefixTreeTest {

    @Test
    public void hasPrefix_containsPrefix_returnsTrue() {
        PrefixTree prefixTree = new PrefixTree(
                Arrays.asList("com/sun", "antlr", "ch/quos/logback"));
        Assert.assertTrue(prefixTree.hasPrefix("antlr"));
        Assert.assertTrue(prefixTree.hasPrefix("com/sun/test"));
        Assert.assertTrue(prefixTree.hasPrefix("com/sun"));
    }

    @Test
    public void hasPrefix_doesNotContainPrefix_returnsFalse() {
        PrefixTree prefixTree = new PrefixTree(
                Arrays.asList("com/sun", "antlr", "ch/quos/logback"));
        Assert.assertFalse(prefixTree.hasPrefix(""));
        Assert.assertFalse(prefixTree.hasPrefix("a"));
        Assert.assertFalse(prefixTree.hasPrefix("test"));
        Assert.assertFalse(prefixTree.hasPrefix("com/su"));
    }

    @Test
    public void hasPrefix_emptyTree_returnsFalse() {
        PrefixTree prefixTree = new PrefixTree(Collections.emptyList());
        Assert.assertFalse(prefixTree.hasPrefix("a"));
    }

}

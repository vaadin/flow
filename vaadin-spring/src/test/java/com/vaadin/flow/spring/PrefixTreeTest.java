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
package com.vaadin.flow.spring;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PrefixTreeTest {

    @Test
    public void hasPrefix_containsPrefix_returnsTrue() {
        PrefixTree prefixTree = new PrefixTree(
                Arrays.asList("com/sun", "antlr", "ch/quos/logback"));
        Assertions.assertTrue(prefixTree.hasPrefix("antlr"));
        Assertions.assertTrue(prefixTree.hasPrefix("com/sun/test"));
        Assertions.assertTrue(prefixTree.hasPrefix("com/sun"));
    }

    @Test
    public void hasPrefix_doesNotContainPrefix_returnsFalse() {
        PrefixTree prefixTree = new PrefixTree(
                Arrays.asList("com/sun", "antlr", "ch/quos/logback"));
        Assertions.assertFalse(prefixTree.hasPrefix(""));
        Assertions.assertFalse(prefixTree.hasPrefix("a"));
        Assertions.assertFalse(prefixTree.hasPrefix("test"));
        Assertions.assertFalse(prefixTree.hasPrefix("com/su"));
    }

    @Test
    public void hasPrefix_emptyTree_returnsFalse() {
        PrefixTree prefixTree = new PrefixTree(Collections.emptyList());
        Assertions.assertFalse(prefixTree.hasPrefix("a"));
    }

}

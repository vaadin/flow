/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.namespace;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.change.NodeChange;

public class ClassListNamespaceTest
        extends AbstractNamespaceTest<ClassListNamespace> {

    private final ClassListNamespace namespace = createNamespace();

    @Test
    public void removeClassFromList() {
        Set<String> classList = namespace.getAsSet();
        classList.add("class1");
        classList.add("class2");
        List<NodeChange> changes = collectChanges(namespace);
        Assert.assertEquals(2, changes.size());
        classList.remove("class1");
        changes = collectChanges(namespace);
        Assert.assertEquals(1, changes.size());
        classList.remove("class2");
        changes = collectChanges(namespace);
        Assert.assertEquals(1, changes.size());
    }
}

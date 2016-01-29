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

package com.vaadin.hummingbird;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A node owner that is used for nodes that have not yet been attached to a
 * state tree.
 *
 * @since
 * @author Vaadin Ltd
 */
public class TemporaryOwner extends NodeOwner {

    private final Set<StateNode> nodes = new HashSet<>();

    @Override
    public int doRegister(StateNode node) {
        nodes.add(node);
        return -1;
    }

    @Override
    public Collection<StateNode> getNodes() {
        return Collections.unmodifiableSet(nodes);
    }

    @Override
    public void doUnregister(StateNode node) {
        nodes.remove(node);
    }
}

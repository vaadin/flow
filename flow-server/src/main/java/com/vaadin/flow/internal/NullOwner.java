/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.flow.internal;

/**
 * A stateless singleton node owner that is used for nodes that have not yet
 * been attached to a state tree. An instance of this type is used instead of a
 * <code>null</code> pointer to avoid cluttering implementations with null
 * checks.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class NullOwner implements NodeOwner {

    private static final NullOwner INSTANCE = new NullOwner();

    private NullOwner() {
        // Singleton
    }

    /**
     * Gets the singleton null owner instance.
     *
     * @return the singleton instance
     */
    public static NullOwner get() {
        return INSTANCE;
    }

    @Override
    public int register(StateNode node) {
        assert node.getOwner() == this;

        return -1;
    }

    @Override
    public void unregister(StateNode node) {
        assert node.getOwner() == this;
    }

    @Override
    public void markAsDirty(StateNode node) {
        assert node.getOwner() == this;
    }

    @Override
    public boolean hasNode(StateNode node) {
        assert node.getOwner() == this;
        return true;
    }
}

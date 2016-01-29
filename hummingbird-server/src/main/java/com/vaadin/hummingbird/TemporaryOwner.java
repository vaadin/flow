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

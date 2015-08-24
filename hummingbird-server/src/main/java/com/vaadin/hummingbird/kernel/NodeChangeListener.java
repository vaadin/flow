package com.vaadin.hummingbird.kernel;

import java.util.List;

import com.vaadin.hummingbird.kernel.change.NodeChange;

public interface NodeChangeListener {

    void onChange(StateNode stateNode, List<NodeChange> changes);

}

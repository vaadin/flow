package com.vaadin.tests.server.component.treetable;

import com.vaadin.ui.TreeTable;

import junit.framework.TestCase;

public class TreeTableSetContainerNullTest extends TestCase {

    public void testNullContainer() {
        TreeTable treeTable = new TreeTable();

        // should not cause an exception
        treeTable.setContainerDataSource(null);
    }
}

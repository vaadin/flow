package com.vaadin.tests.server.component.treetable;

import com.vaadin.ui.TreeTable;

import junit.framework.TestCase;

public class EmptyTreeTableTest extends TestCase {
    public void testLastId() {
        TreeTable treeTable = new TreeTable();

        assertFalse(treeTable.isLastId(treeTable.getValue()));
    }
}

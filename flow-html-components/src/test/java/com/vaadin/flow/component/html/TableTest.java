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
package com.vaadin.flow.component.html;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasOrderedComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableTest extends ComponentTest {
    // Actual test methods in super class

    @BeforeEach
    @Override
    void setup() throws IntrospectionException, InstantiationException,
            IllegalAccessException, ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException {
        whitelistProperty("captionText");
        super.setup();
    }

    @Test
    void getCaption() {
        var component = (Table) getComponent();
        TableCaption caption = component.getCaption();
        AssertUtils.assertEquals(component.getChildren().toList().get(0),
                caption, "Caption does not match");
    }

    @Test
    void addsCaptionAsFirstChild() {
        var component = (Table) getComponent();
        assertEquals(0, component.getChildren().count());
        component.getHead();
        component.addBody();
        component.getFoot();
        var caption = component.getCaption();
        assertEquals(4, component.getChildren().count());
        AssertUtils.assertEquals(caption,
                component.getChildren().findFirst().orElseThrow(),
                "Caption is not the first child");
        AssertUtils.assertEquals(caption.getParent().orElseThrow(), component,
                "Table is not the caption's father");
    }

    @Test
    void setCaptionText() {
        var component = (Table) getComponent();
        String expectedText = "Test caption text.";
        component.setCaptionText(expectedText);
        var caption = component.getCaption();
        assertEquals(expectedText, caption.getText());
    }

    @Test
    void getCaptionText_emptyWhenNoCaption() {
        var component = (Table) getComponent();
        assertEquals("", component.getCaptionText());
        assertTrue(component.findCaption().isEmpty());
    }

    @Test
    void getCaptionText_returnsCaptionText() {
        var component = (Table) getComponent();
        String expectedText = "Test caption text.";
        var caption = component.getCaption();
        caption.setText(expectedText);
        assertEquals(expectedText, component.getCaptionText());
    }

    @Test
    void removeCaption() {
        var component = (Table) getComponent();
        var caption = component.getCaption();
        component.removeCaption();
        assertTrue(caption.getParent().isEmpty());
        assertTrue(component.findCaption().isEmpty());
    }

    @Test
    void getHead() {
        var component = (Table) getComponent();
        assertEquals(0, component.getChildren().count());
        TableHead head = component.getHead();
        AssertUtils.assertEquals(component, head.getParent().orElseThrow(),
                "head was not added");
    }

    @Test
    void addHeadAfterCaption() {
        var component = (Table) getComponent();
        component.getCaption();
        var head = component.getHead();
        assertEquals(2, component.getChildren().count());
        int headIndex = component.getChildren().toList().indexOf(head);
        assertEquals(1, headIndex);
    }

    @Test
    void removeHead() {
        var component = (Table) getComponent();
        TableHead head = component.getHead();
        component.removeHead();
        assertTrue(head.getParent().isEmpty());
        assertTrue(component.findHead().isEmpty());
    }

    @Test
    void getFoot() {
        var component = (Table) getComponent();
        assertEquals(0, component.getChildren().count());
        TableFoot footer = component.getFoot();
        AssertUtils.assertEquals(component, footer.getParent().orElseThrow(),
                "footer was not added");
    }

    @Test
    void removeFoot() {
        var component = (Table) getComponent();
        TableFoot footer = component.getFoot();
        component.removeFoot();
        assertTrue(footer.getParent().isEmpty());
        assertTrue(component.findFoot().isEmpty());
    }

    @Test
    void addBody() {
        var component = (Table) getComponent();
        component.addBody();
        assertEquals(1, component.getChildren().count());
        component.addBody();
        assertEquals(2, component.getChildren().count());
    }

    @Test
    void addBodyAfterCaption() {
        var component = (Table) getComponent();
        component.getCaption();
        var body = component.addBody();
        assertEquals(1, component.getChildren().toList().indexOf(body));
    }

    @Test
    void addBodyAfterHeader() {
        var component = (Table) getComponent();
        component.getHead();
        var body = component.addBody();
        assertEquals(1, component.getChildren().toList().indexOf(body));
    }

    @Test
    void addBodyAfterBothCaptionAndHeader() {
        var component = (Table) getComponent();
        component.getCaption();
        component.getHead();
        var body = component.addBody();
        assertEquals(2, component.getChildren().toList().indexOf(body));
    }

    @Test
    void addBodyBeforeFoot() {
        var component = (Table) getComponent();
        component.getFoot();
        var body = component.addBody();
        assertEquals(0, component.getChildren().toList().indexOf(body));
        assertEquals(1,
                component.getChildren().toList().indexOf(component.getFoot()));
    }

    @Test
    void getBody() {
        var component = (Table) getComponent();
        var body = component.getBody();
        assertEquals(1, component.getChildren().count());
        component.addBody();
        assertEquals(2, component.getChildren().count());
        var secondCallBody = component.getBody();
        AssertUtils.assertEquals(body, secondCallBody,
                "No new body should've been created");
    }

    @Test
    void getBodyByIndex() {
        var component = (Table) getComponent();
        var body = component.getBody(0);
        assertEquals(1, component.getChildren().count());
        var secondCallBody = component.getBody(0);
        assertEquals(1, component.getChildren().count());
        AssertUtils.assertEquals(body, secondCallBody,
                "No new body should've been created");
    }

    @Test
    void getNonExistentBodyByIndex() {
        var component = (Table) getComponent();
        assertThrows(IndexOutOfBoundsException.class,
                () -> component.getBody(1));
    }

    @Test
    void getBodies() {
        var component = (Table) getComponent();
        for (int i = 0; i < 10; i++) {
            component.addBody();
        }
        List<TableBody> bodies = component.getBodies();
        for (TableBody body : bodies) {
            AssertUtils.assertEquals(component, body.getParent().orElseThrow(),
                    "Body is not a child of table");
        }
    }

    @Test
    void removeBody() {
        var component = (Table) getComponent();
        for (int i = 0; i < 10; i++) {
            component.addBody();
        }
        var bodies = component.getBodies();
        for (int i = 0; i < 10; i++) {
            component.removeBody();
            assertTrue(bodies.get(i).getParent().isEmpty());
        }
    }

    @Test
    void removeBodyByIndex() {
        var component = (Table) getComponent();
        var body0 = component.addBody();
        var body1 = component.addBody();
        var body2 = component.addBody();
        component.removeBody(1);
        assertTrue(body0.getParent().isPresent());
        assertTrue(body1.getParent().isEmpty());
        assertTrue(body2.getParent().isPresent());
    }

    @Test
    void removeBodyByReference() {
        var component = (Table) getComponent();
        var body0 = component.addBody();
        var body1 = component.addBody();
        var body2 = component.addBody();
        component.removeBody(body1);
        assertTrue(body0.getParent().isPresent());
        assertTrue(body1.getParent().isEmpty());
        assertTrue(body2.getParent().isPresent());
    }

    @Test
    void addRow_autoCreatesBody() {
        var table = (Table) getComponent();
        TableRow row = table.addRow();
        assertTrue(table.findHead().isEmpty());
        assertEquals(1, table.getBodies().size());
        assertEquals(1, table.getBody().getRowCount());
        AssertUtils.assertEquals(table.getBody(), row.getParent().orElseThrow(),
                "row must live inside the auto-created tbody");
    }

    @Test
    void addRow_withCellTexts_createsDataCells() {
        var table = (Table) getComponent();
        TableRow row = table.addRow("Alice", "30", "Blue");
        assertEquals(3, row.getDataCells().size());
        assertEquals("Alice", row.getDataCells().get(0).getText());
        assertEquals("30", row.getDataCells().get(1).getText());
        assertEquals("Blue", row.getDataCells().get(2).getText());
        assertEquals(0, row.getHeaderCells().size());
    }

    @Test
    void addHeaderRow_autoCreatesThead() {
        var table = (Table) getComponent();
        TableRow row = table.addHeaderRow("Name", "Age");
        assertTrue(table.findHead().isPresent());
        assertEquals(1, table.getHead().getRowCount());
        assertEquals(2, row.getHeaderCells().size());
        assertEquals("Name", row.getHeaderCells().get(0).getText());
    }

    @Test
    void addFooterRow_autoCreatesTfoot() {
        var table = (Table) getComponent();
        TableRow row = table.addFooterRow("Total", "55");
        assertTrue(table.findFoot().isPresent());
        assertEquals(1, table.getFoot().getRowCount());
        assertEquals(2, row.getDataCells().size());
    }

    @Test
    void mdnTutorialStyleConstruction() {
        // Mirrors the MDN "HTML table basics" walkthrough: caption, header
        // row, body rows. Verifies the resulting structure is spec-compliant
        // (caption first, thead before tbody) and that all rows landed in
        // the right wrappers.
        var table = (Table) getComponent();
        table.setCaptionText("People");
        table.addHeaderRow("Name", "Age", "Color");
        table.addRow("Alice", "30", "Blue");
        table.addRow("Bob", "25", "Green");

        assertEquals("People", table.getCaptionText());
        assertEquals(1, table.getHead().getRowCount());
        assertEquals(2, table.getBody().getRowCount());

        var children = table.getChildren().toList();
        assertEquals(table.getCaption(), children.get(0));
        assertEquals(table.getHead(), children.get(1));
        assertEquals(table.getBody(), children.get(2));
    }

    @Test
    void addRows_addsExistingRowsToBody() {
        var table = (Table) getComponent();
        var r1 = new TableRow();
        var r2 = new TableRow();
        table.addRows(r1, r2);
        assertEquals(2, table.getBody().getRowCount());
        AssertUtils.assertEquals(table.getBody(), r1.getParent().orElseThrow(),
                "r1 must be a child of tbody");
        AssertUtils.assertEquals(table.getBody(), r2.getParent().orElseThrow(),
                "r2 must be a child of tbody");
    }

    @Test
    void addCaption_createsAndAppendsComponents() {
        var table = (Table) getComponent();
        var span = new Span("Cars");
        var caption = table.addCaption(span);
        assertEquals(1, caption.getComponentCount());
        assertEquals(span, caption.getComponentAt(0));
        assertEquals(table.getCaption(), caption);
    }

    @Test
    void addColumnGroup_insertedAfterCaptionBeforeHead() {
        var table = (Table) getComponent();
        table.getCaption();
        table.getHead();
        var group = table.addColumnGroup();
        var children = table.getChildren().toList();
        assertEquals(table.getCaption(), children.get(0));
        assertEquals(group, children.get(1));
        assertEquals(table.getHead(), children.get(2));
    }

    @Test
    void addColumnGroup_beforeHeadEvenIfHeadAddedLater() {
        var table = (Table) getComponent();
        var group = table.addColumnGroup();
        var head = table.getHead();
        var children = table.getChildren().toList();
        assertEquals(group, children.get(0));
        assertEquals(head, children.get(1));
    }

    @Test
    void addColumnGroup_withColumns() {
        var table = (Table) getComponent();
        var c1 = new TableColumn();
        var c2 = new TableColumn(2);
        var group = table.addColumnGroup(c1, c2);
        assertEquals(2, group.getColumns().size());
        assertEquals(List.of(group), table.getColumnGroups());
    }

    @Test
    void multipleColumnGroups_appearInInsertionOrder() {
        var table = (Table) getComponent();
        var g1 = table.addColumnGroup();
        var g2 = table.addColumnGroup();
        var children = table.getChildren().toList();
        assertEquals(g1, children.get(0));
        assertEquals(g2, children.get(1));
    }

    @Test
    void removeColumnGroup() {
        var table = (Table) getComponent();
        var g1 = table.addColumnGroup();
        var g2 = table.addColumnGroup();
        table.removeColumnGroup(g1);
        assertEquals(List.of(g2), table.getColumnGroups());
        assertTrue(g1.getParent().isEmpty());
    }

    @Test
    void bodyAppendIndex_accountsForColumnGroups() {
        // caption + 2 colgroups + thead → tbody must land at index 4
        var table = (Table) getComponent();
        table.setCaptionText("x");
        table.addColumnGroup();
        table.addColumnGroup();
        table.getHead();
        var body = table.addBody();
        assertEquals(4, table.getChildren().toList().indexOf(body));
    }

    @Test
    void doesNotExposeGenericAddComponent() {
        var component = (Table) getComponent();
        // The strict Table API must not expose generic add(Component) or
        // any of the other arbitrary-child container helpers.
        assertFalse(component instanceof HasComponents,
                "Table must not implement HasComponents");
        assertFalse(component instanceof HasOrderedComponents,
                "Table must not implement HasOrderedComponents");
    }

}

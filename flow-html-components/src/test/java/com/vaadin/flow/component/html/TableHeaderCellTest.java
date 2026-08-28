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

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.html.TableHeaderCell.Scope;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableHeaderCellTest extends ComponentTest {
    // Actual test methods in super class

    @Override
    protected void addProperties() {
        // Component defines no new properties
    }

    @Test
    void setScope_writesTheAttributeAndReadsBack() {
        TableHeaderCell cell = new TableHeaderCell();
        assertEquals(Optional.empty(), cell.getScope());

        cell.setScope(Scope.ROWGROUP);

        assertEquals("rowgroup", cell.getElement().getAttribute("scope"));
        assertEquals(Optional.of(Scope.ROWGROUP), cell.getScope());
    }

    @Test
    void setScope_null_removesTheAttribute() {
        TableHeaderCell cell = new TableHeaderCell();
        cell.setScope(Scope.COL);

        cell.setScope(null);

        assertEquals(null, cell.getElement().getAttribute("scope"));
        assertEquals(Optional.empty(), cell.getScope());
    }

    @Test
    void getScope_unrecognizedAttributeValue_isEmpty() {
        TableHeaderCell cell = new TableHeaderCell();
        cell.getElement().setAttribute("scope", "nonsense");

        assertEquals(Optional.empty(), cell.getScope());
    }
}

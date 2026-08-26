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

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.html.NativeTableHeaderCell.Scope;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NativeTableHeaderCellTest extends ComponentTest {
    // Actual test methods in super class

    @Override
    protected void addProperties() {
        addProperty("colspan", int.class, 1, 2, false, false);
        addProperty("rowspan", int.class, 1, 2, false, false);
    }

    @Test
    void listConstructor_addsGivenChildren() {
        var span = new Span("a");
        var paragraph = new Paragraph("b");

        var cell = new NativeTableHeaderCell(List.of(span, paragraph));

        assertEquals(List.of(span, paragraph), cell.getChildren().toList());
    }

    @Test
    void setScope_writesTheAttributeAndReadsBack() {
        NativeTableHeaderCell cell = new NativeTableHeaderCell();
        assertEquals(Optional.empty(), cell.getScope());

        cell.setScope(Scope.ROWGROUP);

        assertEquals("rowgroup", cell.getElement().getAttribute("scope"));
        assertEquals(Optional.of(Scope.ROWGROUP), cell.getScope());
    }

    @Test
    void setScope_null_removesTheAttribute() {
        NativeTableHeaderCell cell = new NativeTableHeaderCell();
        cell.setScope(Scope.COL);

        cell.setScope(null);

        assertEquals(null, cell.getElement().getAttribute("scope"));
        assertEquals(Optional.empty(), cell.getScope());
    }

    @Test
    void getScope_unrecognizedAttributeValue_isEmpty() {
        NativeTableHeaderCell cell = new NativeTableHeaderCell();
        cell.getElement().setAttribute("scope", "nonsense");

        assertEquals(Optional.empty(), cell.getScope());
    }

}

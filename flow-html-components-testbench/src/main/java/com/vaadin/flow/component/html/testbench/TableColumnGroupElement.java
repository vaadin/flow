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
package com.vaadin.flow.component.html.testbench;

import java.util.List;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

/**
 * A TestBench element representing a <code>&lt;colgroup&gt;</code> element.
 *
 * @since 25.3
 */
@Element("colgroup")
public class TableColumnGroupElement extends TestBenchElement {

    /**
     * Returns the <code>&lt;col&gt;</code> children of this group, in document
     * order.
     *
     * @return the columns of this group.
     */
    public List<TableColumnElement> getColumns() {
        return $(TableColumnElement.class).all();
    }

    /**
     * Returns the number of columns this <code>&lt;colgroup&gt;</code> spans
     * when it is used without <code>&lt;col&gt;</code> children.
     *
     * @return the value of the {@code span} attribute, or 1 if it is not set.
     */
    public int getSpan() {
        String span = getDomAttribute("span");
        return span == null ? 1 : Integer.parseInt(span);
    }
}

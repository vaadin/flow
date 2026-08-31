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

import com.vaadin.testbench.TestBenchElement;

/**
 * A TestBench element representing either kind of table cell, a
 * <code>&lt;td&gt;</code> or a <code>&lt;th&gt;</code>. It carries no
 * {@code @Element} annotation of its own: it is the type
 * {@link TableRowElement#getCells()} speaks in when the kind of cell does not
 * matter, mirroring {@link com.vaadin.flow.component.html.TableCell} on the
 * component side.
 *
 * @since 25.3
 */
public class TableCellElement extends TestBenchElement {
}

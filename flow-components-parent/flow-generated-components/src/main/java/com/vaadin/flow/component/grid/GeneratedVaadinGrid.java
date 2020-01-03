/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.component.grid;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasTheme;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import elemental.json.JsonObject;
import com.vaadin.flow.component.Synchronize;
import elemental.json.JsonArray;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-grid>} is a free, high quality data grid / data table Web
 * Component. The content of the the grid can be populated in two ways:
 * imperatively by using renderer callback function and declaratively by using
 * Polymer's Templates.
 * </p>
 * <h3>Quick Start</h3>
 * <p>
 * Start with an assigning an array to the <a
 * href="#/elements/vaadin-grid#property-items">{@code items}</a> property to
 * visualize your data.
 * </p>
 * <p>
 * Use the <a href="#/elements/vaadin-grid-column">{@code <vaadin-grid-column>}
 * </a> element to configure the grid columns. Set {@code path} and
 * {@code header} shorthand properties for the columns to define what gets
 * rendered in the cells of the column.
 * </p>
 * <h4>Example:</h4>
 * <p>
 * &lt;vaadin-grid&gt; &lt;vaadin-grid-column path=&quot;name.first&quot;
 * header=&quot;First name&quot;&gt;&lt;/vaadin-grid-column&gt;
 * &lt;vaadin-grid-column path=&quot;name.last&quot; header=&quot;Last
 * name&quot;&gt;&lt;/vaadin-grid-column&gt; &lt;vaadin-grid-column
 * path=&quot;email&quot;&gt;&lt;/vaadin-grid-column&gt; &lt;/vaadin-grid&gt;
 * </p>
 * <p>
 * For custom content {@code vaadin-grid-column} element provides you with three
 * types of {@code renderer} callback functions: {@code headerRenderer},
 * {@code renderer} and {@code footerRenderer}.
 * </p>
 * <p>
 * Each of those renderer functions provides {@code root}, {@code column},
 * {@code rowData} arguments when applicable. Generate DOM content, append it to
 * the {@code root} element and control the state of the host element by
 * accessing {@code column}. Before generating new content, users are able to
 * check if there is already content in {@code root} for reusing it.
 * </p>
 * <p>
 * Renderers are called on initialization of new column cells and each time the
 * related row data is updated. DOM generated during the renderer call can be
 * reused in the next renderer call and will be provided with the {@code root}
 * argument. On first call it will be empty.
 * </p>
 * <h4>Example:</h4>
 * <p>
 * &lt;vaadin-grid&gt; &lt;vaadin-grid-column&gt;&lt;/vaadin-grid-column&gt;
 * &lt;vaadin-grid-column&gt;&lt;/vaadin-grid-column&gt;
 * &lt;vaadin-grid-column&gt;&lt;/vaadin-grid-column&gt; &lt;/vaadin-grid&gt;
 * {@code const grid = document.querySelector('vaadin-grid');grid.items = [
 * 'name': 'John', 'surname': 'Lennon', 'role': 'singer'}, {'name': 'Ringo',
 * 'surname': 'Starr', 'role': 'drums'}];
 * </p>
 * <p>
 * const columns = grid.querySelectorAll('vaadin-grid-column');
 * </p>
 * <p>
 * columns[0].headerRenderer = function(root) { root.textContent = 'Name'; };
 * columns[0].renderer = function(root, column, rowData) { root.textContent =
 * rowData.item.name; };
 * </p>
 * <p>
 * columns[1].headerRenderer = function(root) { root.textContent = 'Surname'; };
 * columns[1].renderer = function(root, column, rowData) { root.textContent =
 * rowData.item.surname; };
 * </p>
 * <p>
 * columns[2].headerRenderer = function(root) { root.textContent = 'Role'; };
 * columns[2].renderer = function(root, column, rowData) { root.textContent =
 * rowData.item.role; };}
 * </p>
 * <p>
 * Alternatively, the content can be provided with Polymer's Templates:
 * </p>
 * <h4>Example:</h4>
 * <p>
 * &lt;vaadin-grid items='[{&quot;name&quot;: &quot;John&quot;,
 * &quot;surname&quot;: &quot;Lennon&quot;, &quot;role&quot;:
 * &quot;singer&quot;}, {&quot;name&quot;: &quot;Ringo&quot;,
 * &quot;surname&quot;: &quot;Starr&quot;, &quot;role&quot;:
 * &quot;drums&quot;}]'&gt; &lt;vaadin-grid-column&gt; &lt;template
 * class=&quot;header&quot;&gt;Name&lt;/template&gt;
 * &lt;template&gt;[[item.name]]&lt;/template&gt; &lt;/vaadin-grid-column&gt;
 * &lt;vaadin-grid-column&gt; &lt;template
 * class=&quot;header&quot;&gt;Surname&lt;/template&gt;
 * &lt;template&gt;[[item.surname]]&lt;/template&gt; &lt;/vaadin-grid-column&gt;
 * &lt;vaadin-grid-column&gt; &lt;template
 * class=&quot;header&quot;&gt;Role&lt;/template&gt;
 * &lt;template&gt;[[item.role]]&lt;/template&gt; &lt;/vaadin-grid-column&gt;
 * &lt;/vaadin-grid&gt;
 * </p>
 * <p>
 * The following helper elements can be used for further customization:
 * </p>
 * <ul>
 * <li><a href="#/elements/vaadin-grid-column-group">
 * {@code <vaadin-grid-column-group>}</a></li>
 * <li><a href="#/elements/vaadin-grid-filter">{@code <vaadin-grid-filter>}</a></li>
 * <li><a href="#/elements/vaadin-grid-sorter">{@code <vaadin-grid-sorter>}</a></li>
 * <li><a href="#/elements/vaadin-grid-selection-column">
 * {@code <vaadin-grid-selection-column>}</a></li>
 * <li><a href="#/elements/vaadin-grid-tree-toggle">
 * {@code <vaadin-grid-tree-toggle>}</a></li>
 * </ul>
 * <p>
 * <strong>Note that the helper elements must be explicitly imported.</strong>
 * If you want to import everything at once you can use the
 * {@code all-imports.html} bundle.
 * </p>
 * <p>
 * A column template can be decorated with one the following class names to
 * specify its purpose
 * </p>
 * <ul>
 * <li>{@code header}: Marks a header template</li>
 * <li>{@code footer}: Marks a footer template</li>
 * <li>{@code row-details}: Marks a row details template</li>
 * </ul>
 * <p>
 * The following built-in template variables can be bound to inside the column
 * templates:
 * </p>
 * <ul>
 * <li>{@code [[index]]}: Number representing the row index</li>
 * <li>{@code [[item]]} and it's sub-properties: Data object (provided by a data
 * provider / items array)</li>
 * <li>{@code {selected}}}: True if the item is selected (can be two-way bound)
 * </li>
 * <li>{@code {detailsOpened}}}: True if the item has row details opened (can
 * be two-way bound)</li>
 * <li>{@code {expanded}}}: True if the item has tree sublevel expanded (can be
 * two-way bound)</li>
 * <li>{@code [[level]]}: Number of the tree sublevel of the item, first
 * level-items have 0</li>
 * </ul>
 * <h3>Lazy Loading with Function Data Provider</h3>
 * <p>
 * In addition to assigning an array to the items property, you can
 * alternatively provide the {@code <vaadin-grid>} data through the <a
 * href="#/elements/vaadin-grid#property-dataProvider">{@code dataProvider}</a>
 * function property. The {@code <vaadin-grid>} calls this function lazily, only
 * when it needs more data to be displayed.
 * </p>
 * <p>
 * See the <a href="#/elements/vaadin-grid#property-dataProvider">
 * {@code dataProvider}</a> in the API reference below for the detailed data
 * provider arguments description, and the “Assigning Data” page in the demos.
 * </p>
 * <p>
 * <strong>Note that expanding the tree grid's item will trigger a call to the
 * {@code dataProvider}.</strong>
 * </p>
 * <p>
 * <strong>Also, note that when using function data providers, the total number
 * of items needs to be set manually. The total number of items can be returned
 * in the second argument of the data provider callback:</strong>
 * </p>
 * <p>
 * {@code javascript grid.dataProvider = function(params, callback) var url =
 * 'https://api.example/data' + '?page=' + params.page + // the requested page
 * index '&amp;per_page=' + params.pageSize; // number of items on the page var
 * xhr = new XMLHttpRequest(); xhr.onload = function() { var response =
 * JSON.parse(xhr.responseText); callback( response.employees, // requested page
 * of items response.totalSize // total number of items ); }; xhr.open('GET',
 * url, true); xhr.send(); };}
 * </p>
 * <p>
 * <strong>Alternatively, you can use the {@code size} property to set the total
 * number of items:</strong>
 * </p>
 * <p>
 * {@code javascript
grid.size = 200; // The total number of items
 * 
 * grid.dataProvider = function(params, callback) var url =
 * 'https://api.example/data' + '?page=' + params.page + // the requested page
 * index '&amp;per_page=' + params.pageSize; // number of items on the page var
 * xhr = new XMLHttpRequest(); xhr.onload = function() { var response =
 * JSON.parse(xhr.responseText); callback(response.employees); };
 * xhr.open('GET', url, true); xhr.send(); };}
 * </p>
 * <h3>Styling</h3>
 * <p>
 * The following shadow DOM parts are available for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Part name</th>
 * <th>Description</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code row}</td>
 * <td>Row in the internal table</td>
 * </tr>
 * <tr>
 * <td>{@code cell}</td>
 * <td>Cell in the internal table</td>
 * </tr>
 * <tr>
 * <td>{@code header-cell}</td>
 * <td>Header cell in the internal table</td>
 * </tr>
 * <tr>
 * <td>{@code body-cell}</td>
 * <td>Body cell in the internal table</td>
 * </tr>
 * <tr>
 * <td>{@code footer-cell}</td>
 * <td>Footer cell in the internal table</td>
 * </tr>
 * <tr>
 * <td>{@code details-cell}</td>
 * <td>Row details cell in the internal table</td>
 * </tr>
 * <tr>
 * <td>{@code resize-handle}</td>
 * <td>Handle for resizing the columns</td>
 * </tr>
 * <tr>
 * <td>{@code reorder-ghost}</td>
 * <td>Ghost element of the header cell being dragged</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * The following state attributes are available for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Attribute</th>
 * <th>Description</th>
 * <th>Part name</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code loading}</td>
 * <td>Set when the grid is loading data from data provider</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code interacting}</td>
 * <td>Keyboard navigation in interaction mode</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code navigating}</td>
 * <td>Keyboard navigation in navigation mode</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code overflow}</td>
 * <td>Set when rows are overflowing the grid viewport. Possible values:
 * {@code top}, {@code bottom}, {@code left}, {@code right}</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code reordering}</td>
 * <td>Set when the grid's columns are being reordered</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code reorder-status}</td>
 * <td>Reflects the status of a cell while columns are being reordered</td>
 * <td>cell</td>
 * </tr>
 * <tr>
 * <td>{@code frozen}</td>
 * <td>Frozen cell</td>
 * <td>cell</td>
 * </tr>
 * <tr>
 * <td>{@code last-frozen}</td>
 * <td>Last frozen cell</td>
 * <td>cell</td>
 * </tr>
 * <tr>
 * <td>* {@code first-column}</td>
 * <td>First visible cell on a row</td>
 * <td>cell</td>
 * </tr>
 * <tr>
 * <td>{@code last-column}</td>
 * <td>Last visible cell on a row</td>
 * <td>cell</td>
 * </tr>
 * <tr>
 * <td>{@code selected}</td>
 * <td>Selected row</td>
 * <td>row</td>
 * </tr>
 * <tr>
 * <td>{@code expanded}</td>
 * <td>Expanded row</td>
 * <td>row</td>
 * </tr>
 * <tr>
 * <td>{@code loading}</td>
 * <td>Row that is waiting for data from data provider</td>
 * <td>row</td>
 * </tr>
 * <tr>
 * <td>{@code odd}</td>
 * <td>Odd row</td>
 * <td>row</td>
 * </tr>
 * <tr>
 * <td>{@code first}</td>
 * <td>The first body row</td>
 * <td>row</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * See <a
 * href="https://github.com/vaadin/vaadin-themable-mixin/wiki">ThemableMixin –
 * how to apply styles for shadow parts</a>
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.GridElement#5.2.1", "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-grid")
@HtmlImport("frontend://bower_components/vaadin-grid/src/vaadin-grid.html")
public abstract class GeneratedVaadinGrid<R extends GeneratedVaadinGrid<R>>
        extends Component implements HasStyle, HasTheme {

    /**
     * Adds theme variants to the component.
     * 
     * @param variants
     *            theme variants to add
     */
    public void addThemeVariants(GridVariant... variants) {
        getThemeNames().addAll(Stream.of(variants)
                .map(GridVariant::getVariantName).collect(Collectors.toList()));
    }

    /**
     * Removes theme variants from the component.
     * 
     * @param variants
     *            theme variants to remove
     */
    public void removeThemeVariants(GridVariant... variants) {
        getThemeNames().removeAll(Stream.of(variants)
                .map(GridVariant::getVariantName).collect(Collectors.toList()));
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The item user has last interacted with. Turns to {@code null} after user
     * deactivates the item by re-interacting with the currently active item.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'active-item-changed' event happens.
     * </p>
     * 
     * @return the {@code activeItem} property from the webcomponent
     */
    @Synchronize(property = "activeItem", value = "active-item-changed")
    protected JsonObject getActiveItemJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("activeItem");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The item user has last interacted with. Turns to {@code null} after user
     * deactivates the item by re-interacting with the currently active item.
     * </p>
     * 
     * @param activeItem
     *            the JsonObject value to set
     */
    protected void setActiveItem(JsonObject activeItem) {
        getElement().setPropertyJson("activeItem", activeItem);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * An array containing the items which will be stamped to the column
     * template instances.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code items} property from the webcomponent
     */
    protected JsonArray getItemsJsonArray() {
        return (JsonArray) getElement().getPropertyRaw("items");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * An array containing the items which will be stamped to the column
     * template instances.
     * </p>
     * 
     * @param items
     *            the JsonArray value to set
     */
    protected void setItems(JsonArray items) {
        getElement().setPropertyJson("items", items);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Number of items fetched at a time from the dataprovider.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code pageSize} property from the webcomponent
     */
    protected double getPageSizeDouble() {
        return getElement().getProperty("pageSize", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Number of items fetched at a time from the dataprovider.
     * </p>
     * 
     * @param pageSize
     *            the double value to set
     */
    protected void setPageSize(double pageSize) {
        getElement().setProperty("pageSize", pageSize);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Function that provides items lazily. Receives arguments {@code params},
     * {@code callback}
     * </p>
     * <p>
     * {@code params.page} Requested page index
     * </p>
     * <p>
     * {@code params.pageSize} Current page size
     * </p>
     * <p>
     * {@code params.filters} Currently applied filters
     * </p>
     * <p>
     * {@code params.sortOrders} Currently applied sorting orders
     * </p>
     * <p>
     * {@code params.parentItem} When tree is used, and sublevel items are
     * requested, reference to parent item of the requested sublevel. Otherwise
     * {@code undefined}.
     * </p>
     * <p>
     * {@code callback(items, size)} Callback function with arguments:
     * </p>
     * <ul>
     * <li>{@code items} Current page of items</li>
     * <li>{@code size} Total number of items. When tree sublevel items are
     * requested, total number of items in the requested sublevel. Optional when
     * tree is not used, required for tree.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'data-provider-changed' event happens.</li>
     * </ul>
     * 
     * @return the {@code dataProvider} property from the webcomponent
     */
    @Synchronize(property = "dataProvider", value = "data-provider-changed")
    protected JsonObject getDataProviderJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("dataProvider");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Function that provides items lazily. Receives arguments {@code params},
     * {@code callback}
     * </p>
     * <p>
     * {@code params.page} Requested page index
     * </p>
     * <p>
     * {@code params.pageSize} Current page size
     * </p>
     * <p>
     * {@code params.filters} Currently applied filters
     * </p>
     * <p>
     * {@code params.sortOrders} Currently applied sorting orders
     * </p>
     * <p>
     * {@code params.parentItem} When tree is used, and sublevel items are
     * requested, reference to parent item of the requested sublevel. Otherwise
     * {@code undefined}.
     * </p>
     * <p>
     * {@code callback(items, size)} Callback function with arguments:
     * </p>
     * <ul>
     * <li>{@code items} Current page of items</li>
     * <li>{@code size} Total number of items. When tree sublevel items are
     * requested, total number of items in the requested sublevel. Optional when
     * tree is not used, required for tree.</li>
     * </ul>
     * 
     * @param dataProvider
     *            the JsonObject value to set
     */
    protected void setDataProvider(JsonObject dataProvider) {
        getElement().setPropertyJson("dataProvider", dataProvider);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * {@code true} while data is being requested from the data provider.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'loading-changed' event happens.
     * </p>
     * 
     * @return the {@code loading} property from the webcomponent
     */
    @Synchronize(property = "loading", value = "loading-changed")
    protected boolean isLoadingBoolean() {
        return getElement().getProperty("loading", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Path to an item sub-property that identifies the item.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code itemIdPath} property from the webcomponent
     */
    protected String getItemIdPathString() {
        return getElement().getProperty("itemIdPath");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Path to an item sub-property that identifies the item.
     * </p>
     * 
     * @param itemIdPath
     *            the String value to set
     */
    protected void setItemIdPath(String itemIdPath) {
        getElement().setProperty("itemIdPath",
                itemIdPath == null ? "" : itemIdPath);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * An array that contains the expanded items.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'expanded-items-changed' event happens.
     * </p>
     * 
     * @return the {@code expandedItems} property from the webcomponent
     */
    @Synchronize(property = "expandedItems", value = "expanded-items-changed")
    protected JsonObject getExpandedItemsJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("expandedItems");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * An array that contains the expanded items.
     * </p>
     * 
     * @param expandedItems
     *            the JsonObject value to set
     */
    protected void setExpandedItems(JsonObject expandedItems) {
        getElement().setPropertyJson("expandedItems", expandedItems);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * An array containing references to items with open row details.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code detailsOpenedItems} property from the webcomponent
     */
    protected JsonArray getDetailsOpenedItemsJsonArray() {
        return (JsonArray) getElement().getPropertyRaw("detailsOpenedItems");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * An array containing references to items with open row details.
     * </p>
     * 
     * @param detailsOpenedItems
     *            the JsonArray value to set
     */
    protected void setDetailsOpenedItems(JsonArray detailsOpenedItems) {
        getElement().setPropertyJson("detailsOpenedItems", detailsOpenedItems);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * An array that contains the selected items.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'selected-items-changed' event happens.
     * </p>
     * 
     * @return the {@code selectedItems} property from the webcomponent
     */
    @Synchronize(property = "selectedItems", value = "selected-items-changed")
    protected JsonObject getSelectedItemsJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("selectedItems");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * An array that contains the selected items.
     * </p>
     * 
     * @param selectedItems
     *            the JsonObject value to set
     */
    protected void setSelectedItems(JsonObject selectedItems) {
        getElement().setPropertyJson("selectedItems", selectedItems);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When {@code true}, all {@code <vaadin-grid-sorter>} are applied for
     * sorting.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code multiSort} property from the webcomponent
     */
    protected boolean isMultiSortBoolean() {
        return getElement().getProperty("multiSort", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When {@code true}, all {@code <vaadin-grid-sorter>} are applied for
     * sorting.
     * </p>
     * 
     * @param multiSort
     *            the boolean value to set
     */
    protected void setMultiSort(boolean multiSort) {
        getElement().setProperty("multiSort", multiSort);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to allow column reordering.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code columnReorderingAllowed} property from the
     *         webcomponent
     */
    protected boolean isColumnReorderingAllowedBoolean() {
        return getElement().getProperty("columnReorderingAllowed", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to allow column reordering.
     * </p>
     * 
     * @param columnReorderingAllowed
     *            the boolean value to set
     */
    protected void setColumnReorderingAllowed(boolean columnReorderingAllowed) {
        getElement().setProperty("columnReorderingAllowed",
                columnReorderingAllowed);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the grid's height is defined by the number of its rows.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code heightByRows} property from the webcomponent
     */
    protected boolean isHeightByRowsBoolean() {
        return getElement().getProperty("heightByRows", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the grid's height is defined by the number of its rows.
     * </p>
     * 
     * @param heightByRows
     *            the boolean value to set
     */
    protected void setHeightByRows(boolean heightByRows) {
        getElement().setProperty("heightByRows", heightByRows);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns a value that identifies the item. Uses {@code itemIdPath} if
     * available. Can be customized by overriding.
     * </p>
     * 
     * @param item
     *            Missing documentation!
     */
    protected void getItemId(JsonObject item) {
        getElement().callFunction("getItemId", item);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Expands the given item tree.
     * </p>
     * 
     * @param item
     *            Missing documentation!
     */
    protected void expandItem(JsonObject item) {
        getElement().callFunction("expandItem", item);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Collapses the given item tree.
     * </p>
     * 
     * @param item
     *            Missing documentation!
     */
    protected void collapseItem(JsonObject item) {
        getElement().callFunction("collapseItem", item);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Clears the cached pages and reloads data from dataprovider when needed.
     * </p>
     */
    protected void clearCache() {
        getElement().callFunction("clearCache");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Open the details row of a given item.
     * </p>
     * 
     * @param item
     *            Missing documentation!
     */
    protected void openItemDetails(JsonObject item) {
        getElement().callFunction("openItemDetails", item);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Close the details row of a given item.
     * </p>
     * 
     * @param item
     *            Missing documentation!
     */
    protected void closeItemDetails(JsonObject item) {
        getElement().callFunction("closeItemDetails", item);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Selects the given item.
     * </p>
     * 
     * @param item
     *            The item object
     */
    protected void selectItem(JsonObject item) {
        getElement().callFunction("selectItem", item);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Deselects the given item if it is already selected.
     * </p>
     * 
     * @param item
     *            The item object
     */
    protected void deselectItem(JsonObject item) {
        getElement().callFunction("deselectItem", item);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Manually invoke existing renderers for all the columns (header, footer
     * and body cells) and opened row details.
     * </p>
     */
    protected void render() {
        getElement().callFunction("render");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Updates the computed metrics and positioning of internal grid parts
     * (row/details cell positioning etc). Needs to be invoked whenever the
     * sizing of grid content changes asynchronously to ensure consistent
     * appearance (e.g. when a contained image whose bounds aren't known
     * beforehand finishes loading).
     * </p>
     */
    protected void notifyResize() {
        getElement().callFunction("notifyResize");
    }

    public static class ActiveItemChangeEvent<R extends GeneratedVaadinGrid<R>>
            extends ComponentEvent<R> {
        private final JsonObject activeItem;

        public ActiveItemChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.activeItem = source.getActiveItemJsonObject();
        }

        public JsonObject getActiveItem() {
            return activeItem;
        }
    }

    /**
     * Adds a listener for {@code active-item-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addActiveItemChangeListener(
            ComponentEventListener<ActiveItemChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("activeItem",
                        event -> listener.onComponentEvent(
                                new ActiveItemChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }

    public static class DataProviderChangeEvent<R extends GeneratedVaadinGrid<R>>
            extends ComponentEvent<R> {
        private final JsonObject dataProvider;

        public DataProviderChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.dataProvider = source.getDataProviderJsonObject();
        }

        public JsonObject getDataProvider() {
            return dataProvider;
        }
    }

    /**
     * Adds a listener for {@code data-provider-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addDataProviderChangeListener(
            ComponentEventListener<DataProviderChangeEvent<R>> listener) {
        return getElement().addPropertyChangeListener("dataProvider",
                event -> listener.onComponentEvent(
                        new DataProviderChangeEvent<R>((R) this,
                                event.isUserOriginated())));
    }

    public static class LoadingChangeEvent<R extends GeneratedVaadinGrid<R>>
            extends ComponentEvent<R> {
        private final boolean loading;

        public LoadingChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.loading = source.isLoadingBoolean();
        }

        public boolean isLoading() {
            return loading;
        }
    }

    /**
     * Adds a listener for {@code loading-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addLoadingChangeListener(
            ComponentEventListener<LoadingChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("loading",
                        event -> listener.onComponentEvent(
                                new LoadingChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }

    public static class ExpandedItemsChangeEvent<R extends GeneratedVaadinGrid<R>>
            extends ComponentEvent<R> {
        private final JsonObject expandedItems;

        public ExpandedItemsChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.expandedItems = source.getExpandedItemsJsonObject();
        }

        public JsonObject getExpandedItems() {
            return expandedItems;
        }
    }

    /**
     * Adds a listener for {@code expanded-items-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addExpandedItemsChangeListener(
            ComponentEventListener<ExpandedItemsChangeEvent<R>> listener) {
        return getElement().addPropertyChangeListener("expandedItems",
                event -> listener.onComponentEvent(
                        new ExpandedItemsChangeEvent<R>((R) this,
                                event.isUserOriginated())));
    }

    public static class SelectedItemsChangeEvent<R extends GeneratedVaadinGrid<R>>
            extends ComponentEvent<R> {
        private final JsonObject selectedItems;

        public SelectedItemsChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.selectedItems = source.getSelectedItemsJsonObject();
        }

        public JsonObject getSelectedItems() {
            return selectedItems;
        }
    }

    /**
     * Adds a listener for {@code selected-items-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addSelectedItemsChangeListener(
            ComponentEventListener<SelectedItemsChangeEvent<R>> listener) {
        return getElement().addPropertyChangeListener("selectedItems",
                event -> listener.onComponentEvent(
                        new SelectedItemsChangeEvent<R>((R) this,
                                event.isUserOriginated())));
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'nodistribute'.
     * 
     * @param components
     *            The components to add.
     * @see <a
     *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
     *      page about slots</a>
     * @see <a
     *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
     *      website about slots</a>
     */
    protected void addToNodistribute(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "nodistribute");
            getElement().appendChild(component.getElement());
        }
    }

    /**
     * Removes the given child components from this component.
     * 
     * @param components
     *            The components to remove.
     * @throws IllegalArgumentException
     *             if any of the components is not a child of this component.
     */
    protected void remove(Component... components) {
        for (Component component : components) {
            if (getElement().equals(component.getElement().getParent())) {
                component.getElement().removeAttribute("slot");
                getElement().removeChild(component.getElement());
            } else {
                throw new IllegalArgumentException("The given component ("
                        + component + ") is not a child of this component");
            }
        }
    }

    /**
     * Removes all contents from this component, this includes child components,
     * text content as well as child elements that have been added directly to
     * this component using the {@link Element} API.
     */
    protected void removeAll() {
        getElement().getChildren()
                .forEach(child -> child.removeAttribute("slot"));
        getElement().removeAllChildren();
    }
}
/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.ui.combobox;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.HasStyle;
import com.vaadin.ui.common.ComponentSupplier;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import elemental.json.JsonObject;
import elemental.json.JsonArray;
import com.vaadin.ui.common.NotSupported;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * Element for internal use only.
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: Vaadin.ComboBoxOverlayElement#3.0.1",
        "Flow#1.0-SNAPSHOT" })
@Tag("vaadin-combo-box-dropdown-wrapper")
@HtmlImport("frontend://bower_components/vaadin-combo-box/vaadin-combo-box-dropdown-wrapper.html")
public class GeneratedVaadinComboBoxDropdownWrapper<R extends GeneratedVaadinComboBoxDropdownWrapper<R>>
        extends Component implements HasStyle, ComponentSupplier<R> {

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code rootPath} property from the webcomponent
     */
    public String getRootPath() {
        return getElement().getProperty("rootPath");
    }

    /**
     * @param rootPath
     *            the String value to set
     * @return this instance, for method chaining
     */
    public R setRootPath(String rootPath) {
        getElement().setProperty("rootPath", rootPath == null ? "" : rootPath);
        return get();
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code importPath} property from the webcomponent
     */
    public String getImportPath() {
        return getElement().getProperty("importPath");
    }

    /**
     * @param importPath
     *            the String value to set
     * @return this instance, for method chaining
     */
    public R setImportPath(String importPath) {
        getElement().setProperty("importPath",
                importPath == null ? "" : importPath);
        return get();
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code root} property from the webcomponent
     */
    protected JsonObject protectedGetRoot() {
        return (JsonObject) getElement().getPropertyRaw("root");
    }

    /**
     * @param root
     *            the JsonObject value to set
     * @return this instance, for method chaining
     */
    protected R setRoot(JsonObject root) {
        getElement().setPropertyJson("root", root);
        return get();
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code $} property from the webcomponent
     */
    protected JsonObject protectedGet$() {
        return (JsonObject) getElement().getPropertyRaw("$");
    }

    /**
     * @param $
     *            the JsonObject value to set
     * @return this instance, for method chaining
     */
    protected R set$(JsonObject $) {
        getElement().setPropertyJson("$", $);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the device supports touch events.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code touchDevice} property from the webcomponent
     */
    public boolean isTouchDevice() {
        return getElement().getProperty("touchDevice", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the device supports touch events.
     * </p>
     * 
     * @param touchDevice
     *            the boolean value to set
     * @return this instance, for method chaining
     */
    public R setTouchDevice(boolean touchDevice) {
        getElement().setProperty("touchDevice", touchDevice);
        return get();
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code opened} property from the webcomponent
     */
    public boolean isOpened() {
        return getElement().getProperty("opened", false);
    }

    /**
     * @param opened
     *            the boolean value to set
     * @return this instance, for method chaining
     */
    public R setOpened(boolean opened) {
        getElement().setProperty("opened", opened);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * {@code true} when new items are being loaded.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code loading} property from the webcomponent
     */
    public boolean isLoading() {
        return getElement().getProperty("loading", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * {@code true} when new items are being loaded.
     * </p>
     * 
     * @param loading
     *            the boolean value to set
     * @return this instance, for method chaining
     */
    public R setLoading(boolean loading) {
        getElement().setProperty("loading", loading);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Vertical offset for the overlay position.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code verticalOffset} property from the webcomponent
     */
    public double getVerticalOffset() {
        return getElement().getProperty("verticalOffset", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Vertical offset for the overlay position.
     * </p>
     * 
     * @param verticalOffset
     *            the double value to set
     * @return this instance, for method chaining
     */
    public R setVerticalOffset(double verticalOffset) {
        getElement().setProperty("verticalOffset", verticalOffset);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Sets a bag of property changes to this instance, and synchronously
     * processes all effects of the properties as a batch.
     * </p>
     * <p>
     * Property names must be simple properties, not paths. Batched path
     * propagation is not supported.
     * </p>
     * 
     * @param props
     *            Bag of one or more key-value pairs whose key is a property and
     *            value is the new value to set for that property.
     * @param setReadOnly
     *            When true, any private values set in `props` will be set. By
     *            default, `setProperties` will not set `readOnly: true` root
     *            properties.
     */
    protected void setProperties(JsonObject props, JsonObject setReadOnly) {
        getElement().callFunction("setProperties", props, setReadOnly);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Aliases one data path as another, such that path notifications from one
     * are routed to the other.
     * </p>
     * 
     * @param to
     *            Target path to link.
     * @param from
     *            Source path to link.
     */
    protected void linkPaths(JsonObject to, JsonObject from) {
        getElement().callFunction("linkPaths", to, from);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Aliases one data path as another, such that path notifications from one
     * are routed to the other.
     * </p>
     * 
     * @param to
     *            Target path to link.
     * @param from
     *            Source path to link.
     */
    protected void linkPaths(JsonObject to, String from) {
        getElement().callFunction("linkPaths", to, from);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Aliases one data path as another, such that path notifications from one
     * are routed to the other.
     * </p>
     * 
     * @param to
     *            Target path to link.
     * @param from
     *            Source path to link.
     */
    protected void linkPaths(String to, JsonObject from) {
        getElement().callFunction("linkPaths", to, from);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Aliases one data path as another, such that path notifications from one
     * are routed to the other.
     * </p>
     * 
     * @param to
     *            Target path to link.
     * @param from
     *            Source path to link.
     */
    public void linkPaths(String to, String from) {
        getElement().callFunction("linkPaths", to, from);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Removes a data path alias previously established with {@code _linkPaths}.
     * </p>
     * <p>
     * Note, the path to unlink should be the target ({@code to}) used when
     * linking the paths.
     * </p>
     * 
     * @param path
     *            Target path to unlink.
     */
    protected void unlinkPaths(JsonObject path) {
        getElement().callFunction("unlinkPaths", path);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Removes a data path alias previously established with {@code _linkPaths}.
     * </p>
     * <p>
     * Note, the path to unlink should be the target ({@code to}) used when
     * linking the paths.
     * </p>
     * 
     * @param path
     *            Target path to unlink.
     */
    public void unlinkPaths(String path) {
        getElement().callFunction("unlinkPaths", path);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Notify that an array has changed.
     * </p>
     * <p>
     * Example:
     * </p>
     * 
     * <pre>
     * <code>this.items = [ {name: 'Jim'}, {name: 'Todd'}, {name: 'Bill'} ];
     * 	...
     * 	this.items.splice(1, 1, {name: 'Sam'});
     * 	this.items.push({name: 'Bob'});
     * 	this.notifySplices('items', [
     * 	  { index: 1, removed: [{name: 'Todd'}], addedCount: 1, object: this.items, type: 'splice' },
     * 	  { index: 3, removed: [], addedCount: 1, object: this.items, type: 'splice'}
     * 	]);
     * 	</code>
     * </pre>
     * 
     * @param path
     *            Path that should be notified.
     * @param splices
     *            Array of splice records indicating ordered changes that
     *            occurred to the array. Each record should have the following
     *            fields: index: index at which the change occurred removed:
     *            array of items that were removed from this index addedCount:
     *            number of new items added at this index object: a reference to
     *            the array in question type: the string literal 'splice'
     * 
     *            Note that splice records _must_ be normalized such that they
     *            are reported in index order (raw results from `Object.observe`
     *            are not ordered and must be normalized/merged before
     *            notifying).
     */
    protected void notifySplices(String path, JsonArray splices) {
        getElement().callFunction("notifySplices", path, splices);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Convenience method for reading a value from a path.
     * </p>
     * <p>
     * Note, if any part in the path is undefined, this method returns
     * {@code undefined} (this method does not throw when dereferencing
     * undefined paths).
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>elemental.json.JsonObject</code>. Functions with return types
     * different than void are not supported at this moment.
     * 
     * @param path
     *            Path to the value to read. The path may be specified as a
     *            string (e.g. `foo.bar.baz`) or an array of path parts (e.g.
     *            `['foo.bar', 'baz']`). Note that bracketed expressions are not
     *            supported; string-based path parts must* be separated by dots.
     *            Note that when dereferencing array indices, the index may be
     *            used as a dotted part directly (e.g. `users.12.name` or
     *            `['users', 12, 'name']`).
     * @param root
     *            Root object from which the path is evaluated.
     */
    @NotSupported
    protected void get(JsonObject path, JsonObject root) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Convenience method for reading a value from a path.
     * </p>
     * <p>
     * Note, if any part in the path is undefined, this method returns
     * {@code undefined} (this method does not throw when dereferencing
     * undefined paths).
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>elemental.json.JsonObject</code>. Functions with return types
     * different than void are not supported at this moment.
     * 
     * @param path
     *            Path to the value to read. The path may be specified as a
     *            string (e.g. `foo.bar.baz`) or an array of path parts (e.g.
     *            `['foo.bar', 'baz']`). Note that bracketed expressions are not
     *            supported; string-based path parts must* be separated by dots.
     *            Note that when dereferencing array indices, the index may be
     *            used as a dotted part directly (e.g. `users.12.name` or
     *            `['users', 12, 'name']`).
     * @param root
     *            Root object from which the path is evaluated.
     */
    @NotSupported
    protected void get(String path, JsonObject root) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Convenience method for setting a value to a path and notifying any
     * elements bound to the same path.
     * </p>
     * <p>
     * Note, if any part in the path except for the last is undefined, this
     * method does nothing (this method does not throw when dereferencing
     * undefined paths).
     * </p>
     * 
     * @param path
     *            Path to the value to write. The path may be specified as a
     *            string (e.g. `'foo.bar.baz'`) or an array of path parts (e.g.
     *            `['foo.bar', 'baz']`). Note that bracketed expressions are not
     *            supported; string-based path parts must* be separated by dots.
     *            Note that when dereferencing array indices, the index may be
     *            used as a dotted part directly (e.g. `'users.12.name'` or
     *            `['users', 12, 'name']`).
     * @param value
     *            Value to set at the specified path.
     * @param root
     *            Root object from which the path is evaluated. When specified,
     *            no notification will occur.
     */
    protected void set(JsonObject path, JsonObject value, JsonObject root) {
        getElement().callFunction("set", path, value, root);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Convenience method for setting a value to a path and notifying any
     * elements bound to the same path.
     * </p>
     * <p>
     * Note, if any part in the path except for the last is undefined, this
     * method does nothing (this method does not throw when dereferencing
     * undefined paths).
     * </p>
     * 
     * @param path
     *            Path to the value to write. The path may be specified as a
     *            string (e.g. `'foo.bar.baz'`) or an array of path parts (e.g.
     *            `['foo.bar', 'baz']`). Note that bracketed expressions are not
     *            supported; string-based path parts must* be separated by dots.
     *            Note that when dereferencing array indices, the index may be
     *            used as a dotted part directly (e.g. `'users.12.name'` or
     *            `['users', 12, 'name']`).
     * @param value
     *            Value to set at the specified path.
     * @param root
     *            Root object from which the path is evaluated. When specified,
     *            no notification will occur.
     */
    protected void set(String path, JsonObject value, JsonObject root) {
        getElement().callFunction("set", path, value, root);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Adds items onto the end of the array at the path specified.
     * </p>
     * <p>
     * The arguments after {@code path} and return value match that of
     * {@code Array.prototype.push}.
     * </p>
     * <p>
     * This method notifies other paths to the same array that a splice occurred
     * to the array.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>double</code>. Functions with return types different than void are
     * not supported at this moment.
     * 
     * @param path
     *            Path to array.
     * @param _Items
     *            Missing documentation!
     */
    @NotSupported
    protected void push(JsonObject path, JsonObject _Items) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Adds items onto the end of the array at the path specified.
     * </p>
     * <p>
     * The arguments after {@code path} and return value match that of
     * {@code Array.prototype.push}.
     * </p>
     * <p>
     * This method notifies other paths to the same array that a splice occurred
     * to the array.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>double</code>. Functions with return types different than void are
     * not supported at this moment.
     * 
     * @param path
     *            Path to array.
     * @param _Items
     *            Missing documentation!
     */
    @NotSupported
    protected void push(String path, JsonObject _Items) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Removes an item from the end of array at the path specified.
     * </p>
     * <p>
     * The arguments after {@code path} and return value match that of
     * {@code Array.prototype.pop}.
     * </p>
     * <p>
     * This method notifies other paths to the same array that a splice occurred
     * to the array.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>elemental.json.JsonObject</code>. Functions with return types
     * different than void are not supported at this moment.
     * 
     * @param path
     *            Path to array.
     */
    @NotSupported
    protected void pop(JsonObject path) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Removes an item from the end of array at the path specified.
     * </p>
     * <p>
     * The arguments after {@code path} and return value match that of
     * {@code Array.prototype.pop}.
     * </p>
     * <p>
     * This method notifies other paths to the same array that a splice occurred
     * to the array.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>elemental.json.JsonObject</code>. Functions with return types
     * different than void are not supported at this moment.
     * 
     * @param path
     *            Path to array.
     */
    @NotSupported
    protected void pop(String path) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Starting from the start index specified, removes 0 or more items from the
     * array and inserts 0 or more new items in their place.
     * </p>
     * <p>
     * The arguments after {@code path} and return value match that of
     * {@code Array.prototype.splice}.
     * </p>
     * <p>
     * This method notifies other paths to the same array that a splice occurred
     * to the array.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>elemental.json.JsonArray</code>. Functions with return types
     * different than void are not supported at this moment.
     * 
     * @param path
     *            Path to array.
     * @param start
     *            Index from which to start removing/inserting.
     * @param deleteCount
     *            Number of items to remove.
     * @param _Items
     *            Missing documentation!
     */
    @NotSupported
    protected void splice(JsonObject path, double start, double deleteCount,
            JsonObject _Items) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Starting from the start index specified, removes 0 or more items from the
     * array and inserts 0 or more new items in their place.
     * </p>
     * <p>
     * The arguments after {@code path} and return value match that of
     * {@code Array.prototype.splice}.
     * </p>
     * <p>
     * This method notifies other paths to the same array that a splice occurred
     * to the array.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>elemental.json.JsonArray</code>. Functions with return types
     * different than void are not supported at this moment.
     * 
     * @param path
     *            Path to array.
     * @param start
     *            Index from which to start removing/inserting.
     * @param deleteCount
     *            Number of items to remove.
     * @param _Items
     *            Missing documentation!
     */
    @NotSupported
    protected void splice(String path, double start, double deleteCount,
            JsonObject _Items) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Removes an item from the beginning of array at the path specified.
     * </p>
     * <p>
     * The arguments after {@code path} and return value match that of
     * {@code Array.prototype.pop}.
     * </p>
     * <p>
     * This method notifies other paths to the same array that a splice occurred
     * to the array.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>elemental.json.JsonObject</code>. Functions with return types
     * different than void are not supported at this moment.
     * 
     * @param path
     *            Path to array.
     */
    @NotSupported
    protected void shift(JsonObject path) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Removes an item from the beginning of array at the path specified.
     * </p>
     * <p>
     * The arguments after {@code path} and return value match that of
     * {@code Array.prototype.pop}.
     * </p>
     * <p>
     * This method notifies other paths to the same array that a splice occurred
     * to the array.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>elemental.json.JsonObject</code>. Functions with return types
     * different than void are not supported at this moment.
     * 
     * @param path
     *            Path to array.
     */
    @NotSupported
    protected void shift(String path) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Adds items onto the beginning of the array at the path specified.
     * </p>
     * <p>
     * The arguments after {@code path} and return value match that of
     * {@code Array.prototype.push}.
     * </p>
     * <p>
     * This method notifies other paths to the same array that a splice occurred
     * to the array.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>double</code>. Functions with return types different than void are
     * not supported at this moment.
     * 
     * @param path
     *            Path to array.
     * @param _Items
     *            Missing documentation!
     */
    @NotSupported
    protected void unshift(JsonObject path, JsonObject _Items) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Adds items onto the beginning of the array at the path specified.
     * </p>
     * <p>
     * The arguments after {@code path} and return value match that of
     * {@code Array.prototype.push}.
     * </p>
     * <p>
     * This method notifies other paths to the same array that a splice occurred
     * to the array.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>double</code>. Functions with return types different than void are
     * not supported at this moment.
     * 
     * @param path
     *            Path to array.
     * @param _Items
     *            Missing documentation!
     */
    @NotSupported
    protected void unshift(String path, JsonObject _Items) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Notify that a path has changed.
     * </p>
     * <p>
     * Example:
     * </p>
     * 
     * <pre>
     * <code>this.item.user.name = 'Bob';
     * 	this.notifyPath('item.user.name');
     * 	</code>
     * </pre>
     * 
     * @param path
     *            Path that should be notified.
     * @param value
     *            Value at the path (optional).
     */
    protected void notifyPath(String path, JsonObject value) {
        getElement().callFunction("notifyPath", path, value);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When using the ShadyCSS scoping and custom property shim, causes all
     * shimmed styles in this element (and its subtree) to be updated based on
     * current custom property values.
     * </p>
     * <p>
     * The optional parameter overrides inline custom property styles with an
     * object of properties where the keys are CSS properties, and the values
     * are strings.
     * </p>
     * <p>
     * Example: {@code this.updateStyles( '--color': 'blue'})}
     * </p>
     * <p>
     * These properties are retained unless a value of {@code null} is set.
     * </p>
     * 
     * @param properties
     *            Bag of custom property key/values to apply to this element.
     */
    protected void updateStyles(JsonObject properties) {
        getElement().callFunction("updateStyles", properties);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Rewrites a given URL relative to a base URL. The base URL defaults to the
     * original location of the document containing the {@code dom-module} for
     * this element. This method will return the same URL before and after
     * bundling.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>java.lang.String</code>. Functions with return types different than
     * void are not supported at this moment.
     * 
     * @param url
     *            URL to resolve.
     * @param base
     *            Optional base URL to resolve against, defaults to the
     *            element's `importPath`
     */
    @NotSupported
    protected void resolveUrl(String url, JsonObject base) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Gets the index of the item with the provided label.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>double</code>. Functions with return types different than void are
     * not supported at this moment.
     * 
     * @param label
     *            Missing documentation!
     */
    @NotSupported
    protected void indexOfLabel(JsonObject label) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Gets the label string for the item based on the {@code _itemLabelPath}.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>java.lang.String</code>. Functions with return types different than
     * void are not supported at this moment.
     * 
     * @param item
     *            Missing documentation!
     */
    @NotSupported
    protected void getItemLabel(JsonObject item) {
    }

    public void ensureItemsRendered() {
        getElement().callFunction("ensureItemsRendered");
    }

    public void adjustScrollPosition() {
        getElement().callFunction("adjustScrollPosition");
    }

    public void updateViewportBoundaries() {
        getElement().callFunction("updateViewportBoundaries");
    }
}
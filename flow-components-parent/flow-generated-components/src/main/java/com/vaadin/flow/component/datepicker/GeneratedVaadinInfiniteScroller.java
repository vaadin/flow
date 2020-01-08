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
package com.vaadin.flow.component.datepicker;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import elemental.json.JsonObject;
import com.vaadin.flow.component.NotSupported;
import elemental.json.JsonArray;
import com.vaadin.flow.component.Component;

@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.InfiniteScrollerElement#UNKNOWN",
        "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-infinite-scroller")
@HtmlImport("frontend://bower_components/vaadin-date-picker/src/vaadin-infinite-scroller.html")
public abstract class GeneratedVaadinInfiniteScroller<R extends GeneratedVaadinInfiniteScroller<R>>
        extends Component implements HasStyle {

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code rootPath} property from the webcomponent
     */
    protected String getRootPathString() {
        return getElement().getProperty("rootPath");
    }

    /**
     * @param rootPath
     *            the String value to set
     */
    protected void setRootPath(String rootPath) {
        getElement().setProperty("rootPath", rootPath == null ? "" : rootPath);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code importPath} property from the webcomponent
     */
    protected String getImportPathString() {
        return getElement().getProperty("importPath");
    }

    /**
     * @param importPath
     *            the String value to set
     */
    protected void setImportPath(String importPath) {
        getElement().setProperty("importPath",
                importPath == null ? "" : importPath);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code root} property from the webcomponent
     */
    protected JsonObject getRootJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("root");
    }

    /**
     * @param root
     *            the JsonObject value to set
     */
    protected void setRoot(JsonObject root) {
        getElement().setPropertyJson("root", root);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code $} property from the webcomponent
     */
    protected JsonObject get$JsonObject() {
        return (JsonObject) getElement().getPropertyRaw("$");
    }

    /**
     * @param $
     *            the JsonObject value to set
     */
    protected void set$(JsonObject $) {
        getElement().setPropertyJson("$", $);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Count of individual items in each buffer. The scroller has 2 buffers
     * altogether so bufferSize of 20 will result in 40 buffered DOM items in
     * total. Changing after initialization not supported.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code bufferSize} property from the webcomponent
     */
    protected double getBufferSizeDouble() {
        return getElement().getProperty("bufferSize", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Count of individual items in each buffer. The scroller has 2 buffers
     * altogether so bufferSize of 20 will result in 40 buffered DOM items in
     * total. Changing after initialization not supported.
     * </p>
     * 
     * @param bufferSize
     *            the double value to set
     */
    protected void setBufferSize(double bufferSize) {
        getElement().setProperty("bufferSize", bufferSize);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code active} property from the webcomponent
     */
    protected boolean isActiveBoolean() {
        return getElement().getProperty("active", false);
    }

    /**
     * @param active
     *            the boolean value to set
     */
    protected void setActive(boolean active) {
        getElement().setProperty("active", active);
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
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>elemental.json.JsonObject</code>. Functions with return types
     * different than void are not supported at this moment.
     * 
     * @param props
     *            Bag of one or more key-value pairs whose key is a property and
     *            value is the new value to set for that property.
     * @param setReadOnly
     *            When true, any private values set in `props` will be set. By
     *            default, `setProperties` will not set `readOnly: true` root
     *            properties.
     */
    @NotSupported
    protected void setProperties(JsonObject props, JsonObject setReadOnly) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Aliases one data path as another, such that path notifications from one
     * are routed to the other.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>elemental.json.JsonObject</code>. Functions with return types
     * different than void are not supported at this moment.
     * 
     * @param to
     *            Target path to link.
     * @param from
     *            Source path to link.
     */
    @NotSupported
    protected void linkPaths(JsonObject to, JsonObject from) {
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
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>elemental.json.JsonObject</code>. Functions with return types
     * different than void are not supported at this moment.
     * 
     * @param path
     *            Target path to unlink.
     */
    @NotSupported
    protected void unlinkPaths(JsonObject path) {
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
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>elemental.json.JsonObject</code>. Functions with return types
     * different than void are not supported at this moment.
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
    @NotSupported
    protected void notifySplices(String path, JsonArray splices) {
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
     * Convenience method for setting a value to a path and notifying any
     * elements bound to the same path.
     * </p>
     * <p>
     * Note, if any part in the path except for the last is undefined, this
     * method does nothing (this method does not throw when dereferencing
     * undefined paths).
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>elemental.json.JsonObject</code>. Functions with return types
     * different than void are not supported at this moment.
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
    @NotSupported
    protected void set(JsonObject path, JsonObject value, JsonObject root) {
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
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>elemental.json.JsonObject</code>. Functions with return types
     * different than void are not supported at this moment.
     * 
     * @param path
     *            Path that should be notified.
     * @param value
     *            Value at the path (optional).
     */
    @NotSupported
    protected void notifyPath(String path, JsonObject value) {
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
     * <p>
     * Note: This function does not support updating CSS mixins. You can not
     * dynamically change the value of an {@code @apply}.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>elemental.json.JsonObject</code>. Functions with return types
     * different than void are not supported at this moment.
     * 
     * @param properties
     *            Bag of custom property key/values to apply to this element.
     */
    @NotSupported
    protected void updateStyles(JsonObject properties) {
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
     * Note that this function performs no resolution for URLs that start with
     * {@code /} (absolute URLs) or {@code #} (hash identifiers). For general
     * purpose URL resolution, use {@code window.URL}.
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
}
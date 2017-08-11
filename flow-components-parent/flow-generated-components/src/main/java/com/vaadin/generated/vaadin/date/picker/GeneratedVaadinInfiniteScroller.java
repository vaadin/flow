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
package com.vaadin.generated.vaadin.date.picker;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentSupplier;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import elemental.json.JsonArray;
import com.vaadin.components.NotSupported;

@Generated({"Generator: com.vaadin.generator.ComponentGenerator#0.1-SNAPSHOT",
		"WebComponent: InfiniteScrollerElement#2.0.1", "Flow#0.1-SNAPSHOT"})
@Tag("vaadin-infinite-scroller")
@HtmlImport("frontend://bower_components/vaadin-date-picker/vaadin-infinite-scroller.html")
public class GeneratedVaadinInfiniteScroller<R extends GeneratedVaadinInfiniteScroller<R>>
		extends
			Component implements ComponentSupplier<R>, HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Count of individual items in each buffer. The scroller has 2 buffers
	 * altogether so bufferSize of 20 will result in 40 buffered DOM items in
	 * total. Changing after initialization not supported.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getBufferSize() {
		return getElement().getProperty("bufferSize", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Count of individual items in each buffer. The scroller has 2 buffers
	 * altogether so bufferSize of 20 will result in 40 buffered DOM items in
	 * total. Changing after initialization not supported.
	 * 
	 * @param bufferSize
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public R setBufferSize(double bufferSize) {
		getElement().setProperty("bufferSize", bufferSize);
		return get();
	}

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isActive() {
		return getElement().getProperty("active", false);
	}

	/**
	 * @param active
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setActive(boolean active) {
		getElement().setProperty("active", active);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Provides a default implementation of the standard Custom Elements
	 * {@code attributeChangedCallback}.
	 * 
	 * By default, attributes declared in {@code properties} metadata are
	 * deserialized using their {@code type} information to properties of the
	 * same name. "Dash-cased" attributes are deserialzed to "camelCase"
	 * properties.
	 * 
	 * @param name
	 *            Name of attribute.
	 * @param old
	 *            Old value of attribute.
	 * @param value
	 *            Current value of attribute.
	 */
	protected void attributeChangedCallback(java.lang.String name,
			JsonObject old, elemental.json.JsonObject value) {
		getElement().callFunction("attributeChangedCallback", name, old, value);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Sets a bag of property changes to this instance, and synchronously
	 * processes all effects of the properties as a batch.
	 * 
	 * Property names must be simple properties, not paths. Batched path
	 * propagation is not supported.
	 * 
	 * @param props
	 *            Bag of one or more key-value pairs whose key is a property and
	 *            value is the new value to set for that property.
	 * @param setReadOnly
	 *            When true, any private values set in `props` will be set. By
	 *            default, `setProperties` will not set `readOnly: true` root
	 *            properties.
	 */
	protected void setProperties(elemental.json.JsonObject props,
			elemental.json.JsonObject setReadOnly) {
		getElement().callFunction("setProperties", props, setReadOnly);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Aliases one data path as another, such that path notifications from one
	 * are routed to the other.
	 * 
	 * @param to
	 *            Target path to link.
	 * @param from
	 *            Source path to link.
	 */
	protected void linkPaths(elemental.json.JsonObject to, java.lang.String from) {
		getElement().callFunction("linkPaths", to, from);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Aliases one data path as another, such that path notifications from one
	 * are routed to the other.
	 * 
	 * @param to
	 *            Target path to link.
	 * @param from
	 *            Source path to link.
	 */
	public void linkPaths(java.lang.String to, java.lang.String from) {
		getElement().callFunction("linkPaths", to, from);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Aliases one data path as another, such that path notifications from one
	 * are routed to the other.
	 * 
	 * @param to
	 *            Target path to link.
	 * @param from
	 *            Source path to link.
	 */
	protected void linkPaths(elemental.json.JsonObject to,
			elemental.json.JsonObject from) {
		getElement().callFunction("linkPaths", to, from);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Aliases one data path as another, such that path notifications from one
	 * are routed to the other.
	 * 
	 * @param to
	 *            Target path to link.
	 * @param from
	 *            Source path to link.
	 */
	protected void linkPaths(java.lang.String to, elemental.json.JsonObject from) {
		getElement().callFunction("linkPaths", to, from);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Removes a data path alias previously established with {@code _linkPaths}.
	 * 
	 * Note, the path to unlink should be the target ({@code to}) used when
	 * linking the paths.
	 * 
	 * @param path
	 *            Target path to unlink.
	 */
	protected void unlinkPaths(elemental.json.JsonObject path) {
		getElement().callFunction("unlinkPaths", path);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Removes a data path alias previously established with {@code _linkPaths}.
	 * 
	 * Note, the path to unlink should be the target ({@code to}) used when
	 * linking the paths.
	 * 
	 * @param path
	 *            Target path to unlink.
	 */
	public void unlinkPaths(java.lang.String path) {
		getElement().callFunction("unlinkPaths", path);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Notify that an array has changed.
	 * 
	 * Example:
	 * 
	 * this.items = [ {name: 'Jim'}, {name: 'Todd'}, {name: 'Bill'} ]; ...
	 * this.items.splice(1, 1, {name: 'Sam'}); this.items.push({name: 'Bob'});
	 * this.notifySplices('items', [ { index: 1, removed: [{name: 'Todd'}],
	 * addedCount: 1, obect: this.items, type: 'splice' }, { index: 3, removed:
	 * [], addedCount: 1, object: this.items, type: 'splice'} ]);
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
	protected void notifySplices(java.lang.String path, JsonArray splices) {
		getElement().callFunction("notifySplices", path, splices);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Convenience method for reading a value from a path.
	 * 
	 * Note, if any part in the path is undefined, this method returns
	 * {@code undefined} (this method does not throw when dereferencing
	 * undefined paths).
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
	 * @return It would return a interface elemental.json.JsonObject
	 */
	@NotSupported
	protected void get(elemental.json.JsonObject path,
			elemental.json.JsonObject root) {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Convenience method for reading a value from a path.
	 * 
	 * Note, if any part in the path is undefined, this method returns
	 * {@code undefined} (this method does not throw when dereferencing
	 * undefined paths).
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
	 * @return It would return a interface elemental.json.JsonObject
	 */
	@NotSupported
	protected void get(java.lang.String path, elemental.json.JsonObject root) {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Convenience method for setting a value to a path and notifying any
	 * elements bound to the same path.
	 * 
	 * Note, if any part in the path except for the last is undefined, this
	 * method does nothing (this method does not throw when dereferencing
	 * undefined paths).
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
	protected void set(java.lang.String path, elemental.json.JsonObject value,
			elemental.json.JsonObject root) {
		getElement().callFunction("set", path, value, root);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Convenience method for setting a value to a path and notifying any
	 * elements bound to the same path.
	 * 
	 * Note, if any part in the path except for the last is undefined, this
	 * method does nothing (this method does not throw when dereferencing
	 * undefined paths).
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
	protected void set(elemental.json.JsonObject path,
			elemental.json.JsonObject value, elemental.json.JsonObject root) {
		getElement().callFunction("set", path, value, root);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Adds items onto the end of the array at the path specified.
	 * 
	 * The arguments after {@code path} and return value match that of
	 * {@code Array.prototype.push}.
	 * 
	 * This method notifies other paths to the same array that a splice occurred
	 * to the array.
	 * 
	 * @param path
	 *            Path to array.
	 * @param ...items Missing documentation!
	 * @return It would return a double
	 */
	@NotSupported
	protected void push(java.lang.String path, elemental.json.JsonObject _Items) {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Removes an item from the end of array at the path specified.
	 * 
	 * The arguments after {@code path} and return value match that of
	 * {@code Array.prototype.pop}.
	 * 
	 * This method notifies other paths to the same array that a splice occurred
	 * to the array.
	 * 
	 * @param path
	 *            Path to array.
	 * @return It would return a interface elemental.json.JsonObject
	 */
	@NotSupported
	protected void pop(java.lang.String path) {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Starting from the start index specified, removes 0 or more items from the
	 * array and inserts 0 or more new items in their place.
	 * 
	 * The arguments after {@code path} and return value match that of
	 * {@code Array.prototype.splice}.
	 * 
	 * This method notifies other paths to the same array that a splice occurred
	 * to the array.
	 * 
	 * @param path
	 *            Path to array.
	 * @param start
	 *            Index from which to start removing/inserting.
	 * @param deleteCount
	 *            Number of items to remove.
	 * @param ...items Missing documentation!
	 * @return It would return a interface elemental.json.JsonArray
	 */
	@NotSupported
	protected void splice(java.lang.String path, double start,
			double deleteCount, elemental.json.JsonObject _Items) {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Removes an item from the beginning of array at the path specified.
	 * 
	 * The arguments after {@code path} and return value match that of
	 * {@code Array.prototype.pop}.
	 * 
	 * This method notifies other paths to the same array that a splice occurred
	 * to the array.
	 * 
	 * @param path
	 *            Path to array.
	 * @return It would return a interface elemental.json.JsonObject
	 */
	@NotSupported
	protected void shift(java.lang.String path) {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Adds items onto the beginning of the array at the path specified.
	 * 
	 * The arguments after {@code path} and return value match that of
	 * {@code Array.prototype.push}.
	 * 
	 * This method notifies other paths to the same array that a splice occurred
	 * to the array.
	 * 
	 * @param path
	 *            Path to array.
	 * @param ...items Missing documentation!
	 * @return It would return a double
	 */
	@NotSupported
	protected void unshift(java.lang.String path,
			elemental.json.JsonObject _Items) {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Notify that a path has changed.
	 * 
	 * Example:
	 * 
	 * this.item.user.name = 'Bob'; this.notifyPath('item.user.name');
	 * 
	 * @param path
	 *            Path that should be notified.
	 * @param value
	 *            Value at the path (optional).
	 */
	protected void notifyPath(java.lang.String path,
			elemental.json.JsonObject value) {
		getElement().callFunction("notifyPath", path, value);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Provides a default implementation of the standard Custom Elements
	 * {@code connectedCallback}.
	 * 
	 * The default implementation enables the property effects system and
	 * flushes any pending properties, and updates shimmed CSS properties when
	 * using the ShadyCSS scoping/custom properties polyfill.
	 */
	public void connectedCallback() {
		getElement().callFunction("connectedCallback");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Provides a default implementation of the standard Custom Elements
	 * {@code disconnectedCallback}.
	 */
	public void disconnectedCallback() {
		getElement().callFunction("disconnectedCallback");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When using the ShadyCSS scoping and custom property shim, causes all
	 * shimmed styles in this element (and its subtree) to be updated based on
	 * current custom property values.
	 * 
	 * The optional parameter overrides inline custom property styles with an
	 * object of properties where the keys are CSS properties, and the values
	 * are strings.
	 * 
	 * Example: {@code this.updateStyles( '--color': 'blue'})}
	 * 
	 * These properties are retained unless a value of {@code null} is set.
	 * 
	 * @param properties
	 *            Bag of custom property key/values to apply to this element.
	 */
	protected void updateStyles(elemental.json.JsonObject properties) {
		getElement().callFunction("updateStyles", properties);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Rewrites a given URL relative to a base URL. The base URL defaults to the
	 * original location of the document containing the {@code dom-module} for
	 * this element. This method will return the same URL before and after
	 * bundling.
	 * 
	 * @param url
	 *            URL to resolve.
	 * @param base
	 *            Optional base URL to resolve against, defaults to the
	 *            element's `importPath`
	 * @return It would return a class java.lang.String
	 */
	@NotSupported
	protected void resolveUrl(java.lang.String url,
			elemental.json.JsonObject base) {
	}
}
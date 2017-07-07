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
package com.vaadin.components.iron.meta;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.components.iron.meta.IronMeta;
import elemental.json.JsonObject;
import com.vaadin.annotations.Synchronize;
import com.vaadin.components.NotSupported;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code iron-meta} is a generic element you can use for sharing information
 * across the DOM tree. It uses [monostate
 * pattern](http://c2.com/cgi/wiki?MonostatePattern) such that any instance of
 * iron-meta has access to the shared information. You can use {@code iron-meta}
 * to share whatever you want (or create an extension [like x-meta] for
 * enhancements).
 * 
 * The {@code iron-meta} instances containing your actual data can be loaded in
 * an import, or constructed in any way you see fit. The only requirement is
 * that you create them before you try to access them.
 * 
 * Examples:
 * 
 * If I create an instance like this:
 * 
 * <iron-meta key="info" value="foo/bar"></iron-meta>
 * 
 * Note that value="foo/bar" is the metadata I've defined. I could define more
 * attributes or use child nodes to define additional metadata.
 * 
 * Now I can access that element (and it's metadata) from any iron-meta instance
 * via the byKey method, e.g.
 * 
 * meta.byKey('info');
 * 
 * Pure imperative form would be like:
 * 
 * document.createElement('iron-meta').byKey('info');
 * 
 * Or, in a Polymer element, you can include a meta in your template:
 * 
 * <iron-meta id="meta"></iron-meta> ... this.$.meta.byKey('info');
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.13-SNAPSHOT",
		"WebComponent: iron-meta#2.0.2", "Flow#0.1.13-SNAPSHOT"})
@Tag("iron-meta")
@HtmlImport("frontend://bower_components/iron-meta/iron-meta.html")
public class IronMeta extends Component implements HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The type of meta-data. All meta-data of the same type is stored together.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getType() {
		return getElement().getProperty("type");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The type of meta-data. All meta-data of the same type is stored together.
	 * 
	 * @param type
	 * @return this instance, for method chaining
	 */
	public <R extends IronMeta> R setType(java.lang.String type) {
		getElement().setProperty("type", type == null ? "" : type);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The key used to store {@code value} under the {@code type} namespace.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getKey() {
		return (JsonObject) getElement().getPropertyRaw("key");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The key used to store {@code value} under the {@code type} namespace.
	 * 
	 * @param key
	 * @return this instance, for method chaining
	 */
	public <R extends IronMeta> R setKey(elemental.json.JsonObject key) {
		getElement().setPropertyJson("key", key);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The meta-data to store or retrieve.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'value-changed' event happens.
	 */
	@Synchronize(property = "value", value = "value-changed")
	public JsonObject getValue() {
		return (JsonObject) getElement().getPropertyRaw("value");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The meta-data to store or retrieve.
	 * 
	 * @param value
	 * @return this instance, for method chaining
	 */
	public <R extends IronMeta> R setValue(elemental.json.JsonObject value) {
		getElement().setPropertyJson("value", value);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, {@code value} is set to the iron-meta instance itself.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isSelf() {
		return getElement().getProperty("self", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, {@code value} is set to the iron-meta instance itself.
	 * 
	 * @param self
	 * @return this instance, for method chaining
	 */
	public <R extends IronMeta> R setSelf(boolean self) {
		getElement().setProperty("self", self);
		return getSelf();
	}

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getList() {
		return (JsonObject) getElement().getPropertyRaw("list");
	}

	/**
	 * @param list
	 * @return this instance, for method chaining
	 */
	public <R extends IronMeta> R setList(elemental.json.JsonObject list) {
		getElement().setPropertyJson("list", list);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Retrieves meta data value by key.
	 * 
	 * @param key
	 * @return It would return a interface elemental.json.JsonObject
	 */
	@NotSupported
	protected void byKey(java.lang.String key) {
	}

	@DomEvent("value-changed")
	public static class ValueChangedEvent extends ComponentEvent<IronMeta> {
		public ValueChangedEvent(IronMeta source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addValueChangedListener(
			ComponentEventListener<ValueChangedEvent> listener) {
		return addListener(ValueChangedEvent.class, listener);
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected <R extends IronMeta> R getSelf() {
		return (R) this;
	}
}
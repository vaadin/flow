package com.vaadin.components.iron.meta;

import com.vaadin.ui.Component;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.shared.Registration;
import com.vaadin.flow.dom.DomEventListener;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * `iron-meta` is a generic element you can use for sharing information across
 * the DOM tree. It uses [monostate
 * pattern](http://c2.com/cgi/wiki?MonostatePattern) such that any instance of
 * iron-meta has access to the shared information. You can use `iron-meta` to
 * share whatever you want (or create an extension [like x-meta] for
 * enhancements).
 * 
 * The `iron-meta` instances containing your actual data can be loaded in an
 * import, or constructed in any way you see fit. The only requirement is that
 * you create them before you try to access them.
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
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.9-SNAPSHOT",
		"WebComponent: iron-meta#2.0.0", "Flow#0.1.9-SNAPSHOT"})
@Tag("iron-meta")
@HtmlImport("frontend://bower_components/iron-meta/iron-meta.html")
public class IronMeta extends Component {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The type of meta-data. All meta-data of the same type is stored together.
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
	 */
	public void setType(java.lang.String type) {
		getElement().setProperty("type", type == null ? "" : type);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The key used to store `value` under the `type` namespace.
	 */
	public String getKey() {
		return getElement().getProperty("key");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The key used to store `value` under the `type` namespace.
	 * 
	 * @param key
	 */
	public void setKey(java.lang.String key) {
		getElement().setProperty("key", key == null ? "" : key);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The meta-data to store or retrieve.
	 */
	public String getValue() {
		return getElement().getProperty("value");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The meta-data to store or retrieve.
	 * 
	 * @param value
	 */
	public void setValue(java.lang.String value) {
		getElement().setProperty("value", value == null ? "" : value);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, `value` is set to the iron-meta instance itself.
	 */
	public boolean isSelf() {
		return getElement().getProperty("self", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, `value` is set to the iron-meta instance itself.
	 * 
	 * @param self
	 */
	public void setSelf(boolean self) {
		getElement().setProperty("self", self);
	}

	public JsonObject getList() {
		return (JsonObject) getElement().getPropertyRaw("list");
	}

	/**
	 * @param list
	 */
	public void setList(elemental.json.JsonObject list) {
		getElement().setPropertyJson("list", list);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Retrieves meta data value by key.
	 * 
	 * @param key
	 */
	public void byKey(java.lang.String key) {
		getElement().callFunction("byKey", key);
	}

	public Registration addValueChangedListener(DomEventListener listener) {
		return getElement().addEventListener("value-changed", listener);
	}
}
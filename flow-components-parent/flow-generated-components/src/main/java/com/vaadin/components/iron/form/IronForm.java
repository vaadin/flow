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
package com.vaadin.components.iron.form;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.components.NotSupported;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code <iron-form>} is a wrapper around the HTML {@code <form>} element, that
 * can validate and submit both custom and native HTML elements. Note that this
 * is a breaking change from iron-form 1.0, which was a type extension.
 * 
 * It has two modes: if {@code allow-redirect} is true, then after the form
 * submission you will be redirected to the server response. Otherwise, if it is
 * false, it will use an {@code iron-ajax} element to submit the form contents
 * to the server.
 * 
 * Example:
 * 
 * <iron-form> <form method="get" action="/form/handler"> <input type="text"
 * name="name" value="Batman"> <input type="checkbox" name="donuts" checked> I
 * like donuts<br>
 * <paper-checkbox name="cheese" value="yes" checked></paper-checkbox> </form>
 * </iron-form>
 * 
 * By default, a native {@code <button>} element will submit this form. However,
 * if you want to submit it from a custom element's click handler, you need to
 * explicitly call the {@code iron-form}'s {@code submit} method.
 * 
 * Example:
 * 
 * <paper-button raised onclick="submitForm()">Submit</paper-button>
 * 
 * function submitForm() { document.getElementById('iron-form').submit(); }
 * 
 * If you are not using the {@code allow-redirect} mode, then you also have the
 * option of customizing the request sent to the server. To do so, you can
 * listen to the {@code iron-form-presubmit} event, and modify the form's [
 * {@code iron-ajax}
 * ](https://elements.polymer-project.org/elements/iron-ajax) object. However,
 * If you want to not use {@code iron-ajax} at all, you can cancel the event and
 * do your own custom submission:
 * 
 * Example of modifying the request, but still using the build-in form
 * submission:
 * 
 * form.addEventListener('iron-form-presubmit', function() { this.request.method
 * = 'put'; this.request.params['extraParam'] = 'someValue'; });
 * 
 * Example of bypassing the build-in form submission:
 * 
 * form.addEventListener('iron-form-presubmit', function(event) {
 * event.preventDefault(); var firebase = new
 * Firebase(form.getAttribute('action')); firebase.set(form.serializeForm());
 * });
 * 
 * Note that if you're dynamically creating this element, it's mandatory that
 * you first create the contained {@code <form>} element and all its children,
 * and only then attach it to the {@code <iron-form>}:
 * 
 * var wrapper = document.createElement('iron-form'); var form =
 * document.createElement('form'); var input = document.createElement('input');
 * form.appendChild(input); document.body.appendChild(wrapper);
 * wrapper.appendChild(form);
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.12-SNAPSHOT",
		"WebComponent: iron-form#2.0.0", "Flow#0.1.12-SNAPSHOT"})
@Tag("iron-form")
@HtmlImport("frontend://bower_components/iron-form/iron-form.html")
public class IronForm<R extends IronForm<R>> extends Component
		implements
			HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set this to true if you don't want the form to be submitted through an
	 * ajax request, and you want the page to redirect to the action URL after
	 * the form has been submitted.
	 */
	public boolean isAllowRedirect() {
		return getElement().getProperty("allowRedirect", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set this to true if you don't want the form to be submitted through an
	 * ajax request, and you want the page to redirect to the action URL after
	 * the form has been submitted.
	 * 
	 * @param allowRedirect
	 * @return This instance, for method chaining.
	 */
	public R setAllowRedirect(boolean allowRedirect) {
		getElement().setProperty("allowRedirect", allowRedirect);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * HTTP request headers to send. See PolymerElements/iron-ajax for more
	 * details. Only works when {@code allowRedirect} is false.
	 */
	public JsonObject getHeaders() {
		return (JsonObject) getElement().getPropertyRaw("headers");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * HTTP request headers to send. See PolymerElements/iron-ajax for more
	 * details. Only works when {@code allowRedirect} is false.
	 * 
	 * @param headers
	 * @return This instance, for method chaining.
	 */
	public R setHeaders(elemental.json.JsonObject headers) {
		getElement().setPropertyJson("headers", headers);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set the {@code withCredentials} flag on the request. See
	 * PolymerElements/iron-ajax for more details. Only works when
	 * {@code allowRedirect} is false.
	 */
	public boolean isWithCredentials() {
		return getElement().getProperty("withCredentials", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set the {@code withCredentials} flag on the request. See
	 * PolymerElements/iron-ajax for more details. Only works when
	 * {@code allowRedirect} is false.
	 * 
	 * @param withCredentials
	 * @return This instance, for method chaining.
	 */
	public R setWithCredentials(boolean withCredentials) {
		getElement().setProperty("withCredentials", withCredentials);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Validates all the required elements (custom and native) in the form.
	 * 
	 * @return It would return a boolean
	 */
	@NotSupported
	protected void validate() {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Submits the form.
	 * 
	 * @param event
	 */
	public void submit(elemental.json.JsonObject event) {
		getElement().callFunction("submit", event);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Resets the form to the default values.
	 * 
	 * @param event
	 */
	public void reset(elemental.json.JsonObject event) {
		getElement().callFunction("reset", event);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Serializes the form as will be used in submission. Note that
	 * {@code serialize} is a Polymer reserved keyword, so calling
	 * {@code someIronForm}.serialize()` will give you unexpected results.
	 * 
	 * @return It would return a interface elemental.json.JsonObject
	 */
	@NotSupported
	protected void serializeForm() {
	}

	@DomEvent("iron-form-error")
	public static class IronFormErrorEvent extends ComponentEvent<IronForm> {
		public IronFormErrorEvent(IronForm source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronFormErrorListener(
			ComponentEventListener<IronFormErrorEvent> listener) {
		return addListener(IronFormErrorEvent.class, listener);
	}

	@DomEvent("iron-form-invalid")
	public static class IronFormInvalidEvent extends ComponentEvent<IronForm> {
		public IronFormInvalidEvent(IronForm source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronFormInvalidListener(
			ComponentEventListener<IronFormInvalidEvent> listener) {
		return addListener(IronFormInvalidEvent.class, listener);
	}

	@DomEvent("iron-form-presubmit")
	public static class IronFormPresubmitEvent extends ComponentEvent<IronForm> {
		public IronFormPresubmitEvent(IronForm source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronFormPresubmitListener(
			ComponentEventListener<IronFormPresubmitEvent> listener) {
		return addListener(IronFormPresubmitEvent.class, listener);
	}

	@DomEvent("iron-form-response")
	public static class IronFormResponseEvent extends ComponentEvent<IronForm> {
		public IronFormResponseEvent(IronForm source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronFormResponseListener(
			ComponentEventListener<IronFormResponseEvent> listener) {
		return addListener(IronFormResponseEvent.class, listener);
	}

	@DomEvent("iron-form-submit")
	public static class IronFormSubmitEvent extends ComponentEvent<IronForm> {
		public IronFormSubmitEvent(IronForm source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronFormSubmitListener(
			ComponentEventListener<IronFormSubmitEvent> listener) {
		return addListener(IronFormSubmitEvent.class, listener);
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected R getSelf() {
		return (R) this;
	}
}
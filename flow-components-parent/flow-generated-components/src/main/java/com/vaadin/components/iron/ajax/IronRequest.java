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
package com.vaadin.components.iron.ajax;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.annotations.Synchronize;
import com.vaadin.components.iron.ajax.IronRequest;
import com.vaadin.components.NotSupported;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * iron-request can be used to perform XMLHttpRequests.
 * 
 * <iron-request id="xhr"></iron-request> ... this.$.xhr.send({url: url, body:
 * params});
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.13-SNAPSHOT",
		"WebComponent: iron-request#2.0.2", "Flow#0.1.13-SNAPSHOT"})
@Tag("iron-request")
@HtmlImport("frontend://bower_components/iron-ajax/iron-request.html")
public class IronRequest extends Component implements HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A reference to the XMLHttpRequest instance used to generate the network
	 * request.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * "xhr-changed" event happens.
	 */
	@Synchronize(property = "xhr", value = "xhr-changed")
	public JsonObject getXhr() {
		return (JsonObject) getElement().getPropertyRaw("xhr");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A reference to the XMLHttpRequest instance used to generate the network
	 * request.
	 * 
	 * @param xhr
	 * @return This instance, for method chaining.
	 */
	public <R extends IronRequest> R setXhr(elemental.json.JsonObject xhr) {
		getElement().setPropertyJson("xhr", xhr);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A reference to the parsed response body, if the {@code xhr} has
	 * completely resolved.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * "response-changed" event happens.
	 */
	@Synchronize(property = "response", value = "response-changed")
	public JsonObject getResponse() {
		return (JsonObject) getElement().getPropertyRaw("response");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A reference to the parsed response body, if the {@code xhr} has
	 * completely resolved.
	 * 
	 * @param response
	 * @return This instance, for method chaining.
	 */
	public <R extends IronRequest> R setResponse(
			elemental.json.JsonObject response) {
		getElement().setPropertyJson("response", response);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A reference to the status code, if the {@code xhr} has completely
	 * resolved.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * "status-changed" event happens.
	 */
	@Synchronize(property = "status", value = "status-changed")
	public double getStatus() {
		return getElement().getProperty("status", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A reference to the status code, if the {@code xhr} has completely
	 * resolved.
	 * 
	 * @param status
	 * @return This instance, for method chaining.
	 */
	public <R extends IronRequest> R setStatus(double status) {
		getElement().setProperty("status", status);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A reference to the status text, if the {@code xhr} has completely
	 * resolved.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getStatusText() {
		return getElement().getProperty("statusText");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A reference to the status text, if the {@code xhr} has completely
	 * resolved.
	 * 
	 * @param statusText
	 * @return This instance, for method chaining.
	 */
	public <R extends IronRequest> R setStatusText(java.lang.String statusText) {
		getElement().setProperty("statusText",
				statusText == null ? "" : statusText);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A promise that resolves when the {@code xhr} response comes back, or
	 * rejects if there is an error before the {@code xhr} completes.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * "completes-changed" event happens.
	 */
	@Synchronize(property = "completes", value = "completes-changed")
	public JsonObject getCompletes() {
		return (JsonObject) getElement().getPropertyRaw("completes");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A promise that resolves when the {@code xhr} response comes back, or
	 * rejects if there is an error before the {@code xhr} completes.
	 * 
	 * @param completes
	 * @return This instance, for method chaining.
	 */
	public <R extends IronRequest> R setCompletes(
			elemental.json.JsonObject completes) {
		getElement().setPropertyJson("completes", completes);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An object that contains progress information emitted by the XHR if
	 * available.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * "progress-changed" event happens.
	 */
	@Synchronize(property = "progress", value = "progress-changed")
	public JsonObject getProgress() {
		return (JsonObject) getElement().getPropertyRaw("progress");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An object that contains progress information emitted by the XHR if
	 * available.
	 * 
	 * @param progress
	 * @return This instance, for method chaining.
	 */
	public <R extends IronRequest> R setProgress(
			elemental.json.JsonObject progress) {
		getElement().setPropertyJson("progress", progress);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Aborted will be true if an abort of the request is attempted.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * "aborted-changed" event happens.
	 */
	@Synchronize(property = "aborted", value = "aborted-changed")
	public boolean isAborted() {
		return getElement().getProperty("aborted", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Aborted will be true if an abort of the request is attempted.
	 * 
	 * @param aborted
	 * @return This instance, for method chaining.
	 */
	public <R extends IronRequest> R setAborted(boolean aborted) {
		getElement().setProperty("aborted", aborted);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Errored will be true if the browser fired an error event from the XHR
	 * object (mainly network errors).
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * "errored-changed" event happens.
	 */
	@Synchronize(property = "errored", value = "errored-changed")
	public boolean isErrored() {
		return getElement().getProperty("errored", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Errored will be true if the browser fired an error event from the XHR
	 * object (mainly network errors).
	 * 
	 * @param errored
	 * @return This instance, for method chaining.
	 */
	public <R extends IronRequest> R setErrored(boolean errored) {
		getElement().setProperty("errored", errored);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * TimedOut will be true if the XHR threw a timeout event.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isTimedOut() {
		return getElement().getProperty("timedOut", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * TimedOut will be true if the XHR threw a timeout event.
	 * 
	 * @param timedOut
	 * @return This instance, for method chaining.
	 */
	public <R extends IronRequest> R setTimedOut(boolean timedOut) {
		getElement().setProperty("timedOut", timedOut);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Succeeded is true if the request succeeded. The request succeeded if it
	 * loaded without error, wasn't aborted, and the status code is ≥ 200, and <
	 * 300, or if the status code is 0.
	 * 
	 * The status code 0 is accepted as a success because some schemes - e.g.
	 * file:// - don't provide status codes.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getSucceeded() {
		return (JsonObject) getElement().getPropertyRaw("succeeded");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Succeeded is true if the request succeeded. The request succeeded if it
	 * loaded without error, wasn't aborted, and the status code is ≥ 200, and <
	 * 300, or if the status code is 0.
	 * 
	 * The status code 0 is accepted as a success because some schemes - e.g.
	 * file:// - don't provide status codes.
	 * 
	 * @param succeeded
	 * @return This instance, for method chaining.
	 */
	public <R extends IronRequest> R setSucceeded(
			elemental.json.JsonObject succeeded) {
		getElement().setPropertyJson("succeeded", succeeded);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Sends an HTTP request to the server and returns the XHR object.
	 * 
	 * The handling of the {@code body} parameter will vary based on the
	 * Content-Type header. See the docs for iron-ajax's {@code body} param for
	 * details.
	 * 
	 * @return It would return a interface elemental.json.JsonObject
	 */
	@NotSupported
	protected void send() {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Attempts to parse the response body of the XHR. If parsing succeeds, the
	 * value returned will be deserialized based on the {@code responseType} set
	 * on the XHR.
	 * 
	 * @return It would return a interface elemental.json.JsonObject
	 */
	@NotSupported
	protected void parseResponse() {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Aborts the request.
	 */
	public void abort() {
		getElement().callFunction("abort");
	}

	@DomEvent("xhr-changed")
	public static class XhrChangedEvent extends ComponentEvent<IronRequest> {
		public XhrChangedEvent(IronRequest source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addXhrChangedListener(
			ComponentEventListener<XhrChangedEvent> listener) {
		return addListener(XhrChangedEvent.class, listener);
	}

	@DomEvent("response-changed")
	public static class ResponseChangedEvent
			extends
				ComponentEvent<IronRequest> {
		public ResponseChangedEvent(IronRequest source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addResponseChangedListener(
			ComponentEventListener<ResponseChangedEvent> listener) {
		return addListener(ResponseChangedEvent.class, listener);
	}

	@DomEvent("status-changed")
	public static class StatusChangedEvent extends ComponentEvent<IronRequest> {
		public StatusChangedEvent(IronRequest source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addStatusChangedListener(
			ComponentEventListener<StatusChangedEvent> listener) {
		return addListener(StatusChangedEvent.class, listener);
	}

	@DomEvent("status-text-changed")
	public static class StatusTextChangedEvent
			extends
				ComponentEvent<IronRequest> {
		public StatusTextChangedEvent(IronRequest source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addStatusTextChangedListener(
			ComponentEventListener<StatusTextChangedEvent> listener) {
		return addListener(StatusTextChangedEvent.class, listener);
	}

	@DomEvent("completes-changed")
	public static class CompletesChangedEvent
			extends
				ComponentEvent<IronRequest> {
		public CompletesChangedEvent(IronRequest source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addCompletesChangedListener(
			ComponentEventListener<CompletesChangedEvent> listener) {
		return addListener(CompletesChangedEvent.class, listener);
	}

	@DomEvent("progress-changed")
	public static class ProgressChangedEvent
			extends
				ComponentEvent<IronRequest> {
		public ProgressChangedEvent(IronRequest source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addProgressChangedListener(
			ComponentEventListener<ProgressChangedEvent> listener) {
		return addListener(ProgressChangedEvent.class, listener);
	}

	@DomEvent("aborted-changed")
	public static class AbortedChangedEvent extends ComponentEvent<IronRequest> {
		public AbortedChangedEvent(IronRequest source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addAbortedChangedListener(
			ComponentEventListener<AbortedChangedEvent> listener) {
		return addListener(AbortedChangedEvent.class, listener);
	}

	@DomEvent("errored-changed")
	public static class ErroredChangedEvent extends ComponentEvent<IronRequest> {
		public ErroredChangedEvent(IronRequest source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addErroredChangedListener(
			ComponentEventListener<ErroredChangedEvent> listener) {
		return addListener(ErroredChangedEvent.class, listener);
	}

	@DomEvent("timed-out-changed")
	public static class TimedOutChangedEvent
			extends
				ComponentEvent<IronRequest> {
		public TimedOutChangedEvent(IronRequest source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addTimedOutChangedListener(
			ComponentEventListener<TimedOutChangedEvent> listener) {
		return addListener(TimedOutChangedEvent.class, listener);
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected <R extends IronRequest> R getSelf() {
		return (R) this;
	}
}
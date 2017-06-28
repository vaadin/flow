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
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import elemental.json.JsonArray;
import com.vaadin.components.NotSupported;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * The {@code iron-ajax} element exposes network request functionality.
 * 
 * <iron-ajax auto url="https://www.googleapis.com/youtube/v3/search"
 * params='{"part":"snippet", "q":"polymer", "key": "YOUTUBE_API_KEY", "type":
 * "video"}' handle-as="json" on-response="handleResponse"
 * debounce-duration="300"></iron-ajax>
 * 
 * With {@code auto} set to {@code true}, the element performs a request
 * whenever its {@code url}, {@code params} or {@code body} properties are
 * changed. Automatically generated requests will be debounced in the case that
 * multiple attributes are changed sequentially.
 * 
 * Note: The {@code params} attribute must be double quoted JSON.
 * 
 * You can trigger a request explicitly by calling {@code generateRequest} on
 * the element.
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.11-SNAPSHOT",
		"WebComponent: iron-ajax#2.0.2", "Flow#0.1.11-SNAPSHOT"})
@Tag("iron-ajax")
@HtmlImport("frontend://bower_components/iron-ajax/iron-ajax.html")
public class IronAjax<R extends IronAjax<R>> extends Component {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The URL target of the request.
	 */
	public String getUrl() {
		return getElement().getProperty("url");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The URL target of the request.
	 * 
	 * @param url
	 * @return This instance, for method chaining.
	 */
	public R setUrl(java.lang.String url) {
		getElement().setProperty("url", url == null ? "" : url);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An object that contains query parameters to be appended to the specified
	 * {@code url} when generating a request. If you wish to set the body
	 * content when making a POST request, you should use the {@code body}
	 * property instead.
	 */
	public JsonObject getParams() {
		return (JsonObject) getElement().getPropertyRaw("params");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An object that contains query parameters to be appended to the specified
	 * {@code url} when generating a request. If you wish to set the body
	 * content when making a POST request, you should use the {@code body}
	 * property instead.
	 * 
	 * @param params
	 * @return This instance, for method chaining.
	 */
	public R setParams(elemental.json.JsonObject params) {
		getElement().setPropertyJson("params", params);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The HTTP method to use such as 'GET', 'POST', 'PUT', or 'DELETE'. Default
	 * is 'GET'.
	 */
	public String getMethod() {
		return getElement().getProperty("method");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The HTTP method to use such as 'GET', 'POST', 'PUT', or 'DELETE'. Default
	 * is 'GET'.
	 * 
	 * @param method
	 * @return This instance, for method chaining.
	 */
	public R setMethod(java.lang.String method) {
		getElement().setProperty("method", method == null ? "" : method);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * HTTP request headers to send.
	 * 
	 * Example:
	 * 
	 * <iron-ajax auto url="http://somesite.com" headers='{"X-Requested-With":
	 * "XMLHttpRequest"}' handle-as="json"></iron-ajax>
	 * 
	 * Note: setting a {@code Content-Type} header here will override the value
	 * specified by the {@code contentType} property of this element.
	 */
	public JsonObject getHeaders() {
		return (JsonObject) getElement().getPropertyRaw("headers");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * HTTP request headers to send.
	 * 
	 * Example:
	 * 
	 * <iron-ajax auto url="http://somesite.com" headers='{"X-Requested-With":
	 * "XMLHttpRequest"}' handle-as="json"></iron-ajax>
	 * 
	 * Note: setting a {@code Content-Type} header here will override the value
	 * specified by the {@code contentType} property of this element.
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
	 * Content type to use when sending data. If the {@code contentType}
	 * property is set and a {@code Content-Type} header is specified in the
	 * {@code headers} property, the {@code headers} property value will take
	 * precedence.
	 * 
	 * Varies the handling of the {@code body} param.
	 */
	public String getContentType() {
		return getElement().getProperty("contentType");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Content type to use when sending data. If the {@code contentType}
	 * property is set and a {@code Content-Type} header is specified in the
	 * {@code headers} property, the {@code headers} property value will take
	 * precedence.
	 * 
	 * Varies the handling of the {@code body} param.
	 * 
	 * @param contentType
	 * @return This instance, for method chaining.
	 */
	public R setContentType(java.lang.String contentType) {
		getElement().setProperty("contentType",
				contentType == null ? "" : contentType);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Body content to send with the request, typically used with "POST"
	 * requests.
	 * 
	 * If body is a string it will be sent unmodified.
	 * 
	 * If Content-Type is set to a value listed below, then the body will be
	 * encoded accordingly.
	 * 
	 * {@code content-type="application/json"} body is encoded like {@code
	 * "foo":"bar baz","x":1}}
	 * {@code content-type="application/x-www-form-urlencoded"} body is encoded
	 * like {@code foo=bar+baz&x=1}
	 * 
	 * Otherwise the body will be passed to the browser unmodified, and it will
	 * handle any encoding (e.g. for FormData, Blob, ArrayBuffer).
	 */
	public JsonObject getBody() {
		return (JsonObject) getElement().getPropertyRaw("body");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Body content to send with the request, typically used with "POST"
	 * requests.
	 * 
	 * If body is a string it will be sent unmodified.
	 * 
	 * If Content-Type is set to a value listed below, then the body will be
	 * encoded accordingly.
	 * 
	 * {@code content-type="application/json"} body is encoded like {@code
	 * "foo":"bar baz","x":1}}
	 * {@code content-type="application/x-www-form-urlencoded"} body is encoded
	 * like {@code foo=bar+baz&x=1}
	 * 
	 * Otherwise the body will be passed to the browser unmodified, and it will
	 * handle any encoding (e.g. for FormData, Blob, ArrayBuffer).
	 * 
	 * @param body
	 * @return This instance, for method chaining.
	 */
	public R setBody(elemental.json.JsonObject body) {
		getElement().setPropertyJson("body", body);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Toggle whether XHR is synchronous or asynchronous. Don't change this to
	 * true unless You Know What You Are Doing™.
	 */
	public boolean isSync() {
		return getElement().getProperty("sync", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Toggle whether XHR is synchronous or asynchronous. Don't change this to
	 * true unless You Know What You Are Doing™.
	 * 
	 * @param sync
	 * @return This instance, for method chaining.
	 */
	public R setSync(boolean sync) {
		getElement().setProperty("sync", sync);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specifies what data to store in the {@code response} property, and to
	 * deliver as {@code event.detail.response} in {@code response} events.
	 * 
	 * One of:
	 * 
	 * {@code text}: uses {@code XHR.responseText}.
	 * 
	 * {@code xml}: uses {@code XHR.responseXML}.
	 * 
	 * {@code json}: uses {@code XHR.responseText} parsed as JSON.
	 * 
	 * {@code arraybuffer}: uses {@code XHR.response}.
	 * 
	 * {@code blob}: uses {@code XHR.response}.
	 * 
	 * {@code document}: uses {@code XHR.response}.
	 */
	public String getHandleAs() {
		return getElement().getProperty("handleAs");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specifies what data to store in the {@code response} property, and to
	 * deliver as {@code event.detail.response} in {@code response} events.
	 * 
	 * One of:
	 * 
	 * {@code text}: uses {@code XHR.responseText}.
	 * 
	 * {@code xml}: uses {@code XHR.responseXML}.
	 * 
	 * {@code json}: uses {@code XHR.responseText} parsed as JSON.
	 * 
	 * {@code arraybuffer}: uses {@code XHR.response}.
	 * 
	 * {@code blob}: uses {@code XHR.response}.
	 * 
	 * {@code document}: uses {@code XHR.response}.
	 * 
	 * @param handleAs
	 * @return This instance, for method chaining.
	 */
	public R setHandleAs(java.lang.String handleAs) {
		getElement().setProperty("handleAs", handleAs == null ? "" : handleAs);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set the withCredentials flag on the request.
	 */
	public boolean isWithCredentials() {
		return getElement().getProperty("withCredentials", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set the withCredentials flag on the request.
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
	 * Set the timeout flag on the request.
	 */
	public double getTimeout() {
		return getElement().getProperty("timeout", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set the timeout flag on the request.
	 * 
	 * @param timeout
	 * @return This instance, for method chaining.
	 */
	public R setTimeout(double timeout) {
		getElement().setProperty("timeout", timeout);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, automatically performs an Ajax request when either {@code url}
	 * or {@code params} changes.
	 */
	public boolean isAuto() {
		return getElement().getProperty("auto", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, automatically performs an Ajax request when either {@code url}
	 * or {@code params} changes.
	 * 
	 * @param auto
	 * @return This instance, for method chaining.
	 */
	public R setAuto(boolean auto) {
		getElement().setProperty("auto", auto);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, error messages will automatically be logged to the console.
	 */
	public boolean isVerbose() {
		return getElement().getProperty("verbose", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, error messages will automatically be logged to the console.
	 * 
	 * @param verbose
	 * @return This instance, for method chaining.
	 */
	public R setVerbose(boolean verbose) {
		getElement().setProperty("verbose", verbose);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The most recent request made by this iron-ajax element.
	 */
	public JsonObject getLastRequest() {
		return (JsonObject) getElement().getPropertyRaw("lastRequest");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The most recent request made by this iron-ajax element.
	 * 
	 * @param lastRequest
	 * @return This instance, for method chaining.
	 */
	public R setLastRequest(elemental.json.JsonObject lastRequest) {
		getElement().setPropertyJson("lastRequest", lastRequest);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True while lastRequest is in flight.
	 */
	public boolean isLoading() {
		return getElement().getProperty("loading", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True while lastRequest is in flight.
	 * 
	 * @param loading
	 * @return This instance, for method chaining.
	 */
	public R setLoading(boolean loading) {
		getElement().setProperty("loading", loading);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * lastRequest's response.
	 * 
	 * Note that lastResponse and lastError are set when lastRequest finishes,
	 * so if loading is true, then lastResponse and lastError will correspond to
	 * the result of the previous request.
	 * 
	 * The type of the response is determined by the value of {@code handleAs}
	 * at the time that the request was generated.
	 */
	public JsonObject getLastResponse() {
		return (JsonObject) getElement().getPropertyRaw("lastResponse");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * lastRequest's response.
	 * 
	 * Note that lastResponse and lastError are set when lastRequest finishes,
	 * so if loading is true, then lastResponse and lastError will correspond to
	 * the result of the previous request.
	 * 
	 * The type of the response is determined by the value of {@code handleAs}
	 * at the time that the request was generated.
	 * 
	 * @param lastResponse
	 * @return This instance, for method chaining.
	 */
	public R setLastResponse(elemental.json.JsonObject lastResponse) {
		getElement().setPropertyJson("lastResponse", lastResponse);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * lastRequest's error, if any.
	 */
	public JsonObject getLastError() {
		return (JsonObject) getElement().getPropertyRaw("lastError");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * lastRequest's error, if any.
	 * 
	 * @param lastError
	 * @return This instance, for method chaining.
	 */
	public R setLastError(elemental.json.JsonObject lastError) {
		getElement().setPropertyJson("lastError", lastError);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An Array of all in-flight requests originating from this iron-ajax
	 * element.
	 */
	public JsonArray getActiveRequests() {
		return (JsonArray) getElement().getPropertyRaw("activeRequests");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An Array of all in-flight requests originating from this iron-ajax
	 * element.
	 * 
	 * @param activeRequests
	 * @return This instance, for method chaining.
	 */
	public R setActiveRequests(elemental.json.JsonArray activeRequests) {
		getElement().setPropertyJson("activeRequests", activeRequests);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Length of time in milliseconds to debounce multiple automatically
	 * generated requests.
	 */
	public double getDebounceDuration() {
		return getElement().getProperty("debounceDuration", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Length of time in milliseconds to debounce multiple automatically
	 * generated requests.
	 * 
	 * @param debounceDuration
	 * @return This instance, for method chaining.
	 */
	public R setDebounceDuration(double debounceDuration) {
		getElement().setProperty("debounceDuration", debounceDuration);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Prefix to be stripped from a JSON response before parsing it.
	 * 
	 * In order to prevent an attack using CSRF with Array responses
	 * (http://haacked
	 * .com/archive/2008/11/20/anatomy-of-a-subtle-json-vulnerability.aspx/)
	 * many backends will mitigate this by prefixing all JSON response bodies
	 * with a string that would be nonsensical to a JavaScript parser.
	 */
	public String getJsonPrefix() {
		return getElement().getProperty("jsonPrefix");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Prefix to be stripped from a JSON response before parsing it.
	 * 
	 * In order to prevent an attack using CSRF with Array responses
	 * (http://haacked
	 * .com/archive/2008/11/20/anatomy-of-a-subtle-json-vulnerability.aspx/)
	 * many backends will mitigate this by prefixing all JSON response bodies
	 * with a string that would be nonsensical to a JavaScript parser.
	 * 
	 * 
	 * @param jsonPrefix
	 * @return This instance, for method chaining.
	 */
	public R setJsonPrefix(java.lang.String jsonPrefix) {
		getElement().setProperty("jsonPrefix",
				jsonPrefix == null ? "" : jsonPrefix);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * By default, iron-ajax's events do not bubble. Setting this attribute will
	 * cause its request and response events as well as its iron-ajax-request,
	 * -response, and -error events to bubble to the window object. The vanilla
	 * error event never bubbles when using shadow dom even if this.bubbles is
	 * true because a scoped flag is not passed with it (first link) and because
	 * the shadow dom spec did not used to allow certain events, including
	 * events named error, to leak outside of shadow trees (second link).
	 * https://www.w3.org/TR/shadow-dom/#scoped-flag
	 * https://www.w3.org/TR/2015/WD
	 * -shadow-dom-20151215/#events-that-are-not-leaked-into-ancestor-trees
	 */
	public boolean isBubbles() {
		return getElement().getProperty("bubbles", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * By default, iron-ajax's events do not bubble. Setting this attribute will
	 * cause its request and response events as well as its iron-ajax-request,
	 * -response, and -error events to bubble to the window object. The vanilla
	 * error event never bubbles when using shadow dom even if this.bubbles is
	 * true because a scoped flag is not passed with it (first link) and because
	 * the shadow dom spec did not used to allow certain events, including
	 * events named error, to leak outside of shadow trees (second link).
	 * https://www.w3.org/TR/shadow-dom/#scoped-flag
	 * https://www.w3.org/TR/2015/WD
	 * -shadow-dom-20151215/#events-that-are-not-leaked-into-ancestor-trees
	 * 
	 * @param bubbles
	 * @return This instance, for method chaining.
	 */
	public R setBubbles(boolean bubbles) {
		getElement().setProperty("bubbles", bubbles);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Changes the completes promise chain from generateRequest to reject with
	 * an object containing the error message as well as the request.
	 */
	public boolean isRejectWithRequest() {
		return getElement().getProperty("rejectWithRequest", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Changes the completes promise chain from generateRequest to reject with
	 * an object containing the error message as well as the request.
	 * 
	 * @param rejectWithRequest
	 * @return This instance, for method chaining.
	 */
	public R setRejectWithRequest(boolean rejectWithRequest) {
		getElement().setProperty("rejectWithRequest", rejectWithRequest);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The query string that should be appended to the {@code url}, serialized
	 * from the current value of {@code params}.
	 */
	public JsonObject getQueryString() {
		return (JsonObject) getElement().getPropertyRaw("queryString");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The query string that should be appended to the {@code url}, serialized
	 * from the current value of {@code params}.
	 * 
	 * @param queryString
	 * @return This instance, for method chaining.
	 */
	public R setQueryString(elemental.json.JsonObject queryString) {
		getElement().setPropertyJson("queryString", queryString);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The {@code url} with query string (if {@code params} are specified),
	 * suitable for providing to an {@code iron-request} instance.
	 */
	public JsonObject getRequestUrl() {
		return (JsonObject) getElement().getPropertyRaw("requestUrl");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The {@code url} with query string (if {@code params} are specified),
	 * suitable for providing to an {@code iron-request} instance.
	 * 
	 * @param requestUrl
	 * @return This instance, for method chaining.
	 */
	public R setRequestUrl(elemental.json.JsonObject requestUrl) {
		getElement().setPropertyJson("requestUrl", requestUrl);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An object that maps header names to header values, first applying the the
	 * value of {@code Content-Type} and then overlaying the headers specified
	 * in the {@code headers} property.
	 */
	public JsonObject getRequestHeaders() {
		return (JsonObject) getElement().getPropertyRaw("requestHeaders");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An object that maps header names to header values, first applying the the
	 * value of {@code Content-Type} and then overlaying the headers specified
	 * in the {@code headers} property.
	 * 
	 * @param requestHeaders
	 * @return This instance, for method chaining.
	 */
	public R setRequestHeaders(elemental.json.JsonObject requestHeaders) {
		getElement().setPropertyJson("requestHeaders", requestHeaders);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Request options suitable for generating an {@code iron-request} instance
	 * based on the current state of the {@code iron-ajax} instance's
	 * properties.
	 * 
	 * @return It would return a interface elemental.json.JsonObject
	 */
	@NotSupported
	protected void toRequestOptions() {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Performs an AJAX request to the specified URL.
	 * 
	 * @return It would return a interface elemental.json.JsonObject
	 */
	@NotSupported
	protected void generateRequest() {
	}

	@DomEvent("error")
	public static class ErrorEvent extends ComponentEvent<IronAjax> {
		public ErrorEvent(IronAjax source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addErrorListener(
			ComponentEventListener<ErrorEvent> listener) {
		return addListener(ErrorEvent.class, listener);
	}

	@DomEvent("iron-ajax-presend")
	public static class IronAjaxPresendEvent extends ComponentEvent<IronAjax> {
		public IronAjaxPresendEvent(IronAjax source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronAjaxPresendListener(
			ComponentEventListener<IronAjaxPresendEvent> listener) {
		return addListener(IronAjaxPresendEvent.class, listener);
	}

	@DomEvent("request")
	public static class RequestEvent extends ComponentEvent<IronAjax> {
		public RequestEvent(IronAjax source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addRequestListener(
			ComponentEventListener<RequestEvent> listener) {
		return addListener(RequestEvent.class, listener);
	}

	@DomEvent("response")
	public static class ResponseEvent extends ComponentEvent<IronAjax> {
		public ResponseEvent(IronAjax source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addResponseListener(
			ComponentEventListener<ResponseEvent> listener) {
		return addListener(ResponseEvent.class, listener);
	}

	@DomEvent("last-request-changed")
	public static class LastRequestChangedEvent
			extends
				ComponentEvent<IronAjax> {
		public LastRequestChangedEvent(IronAjax source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addLastRequestChangedListener(
			ComponentEventListener<LastRequestChangedEvent> listener) {
		return addListener(LastRequestChangedEvent.class, listener);
	}

	@DomEvent("loading-changed")
	public static class LoadingChangedEvent extends ComponentEvent<IronAjax> {
		public LoadingChangedEvent(IronAjax source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addLoadingChangedListener(
			ComponentEventListener<LoadingChangedEvent> listener) {
		return addListener(LoadingChangedEvent.class, listener);
	}

	@DomEvent("last-response-changed")
	public static class LastResponseChangedEvent
			extends
				ComponentEvent<IronAjax> {
		public LastResponseChangedEvent(IronAjax source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addLastResponseChangedListener(
			ComponentEventListener<LastResponseChangedEvent> listener) {
		return addListener(LastResponseChangedEvent.class, listener);
	}

	@DomEvent("last-error-changed")
	public static class LastErrorChangedEvent extends ComponentEvent<IronAjax> {
		public LastErrorChangedEvent(IronAjax source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addLastErrorChangedListener(
			ComponentEventListener<LastErrorChangedEvent> listener) {
		return addListener(LastErrorChangedEvent.class, listener);
	}

	@DomEvent("active-requests-changed")
	public static class ActiveRequestsChangedEvent
			extends
				ComponentEvent<IronAjax> {
		public ActiveRequestsChangedEvent(IronAjax source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addActiveRequestsChangedListener(
			ComponentEventListener<ActiveRequestsChangedEvent> listener) {
		return addListener(ActiveRequestsChangedEvent.class, listener);
	}

	@DomEvent("debounce-duration-changed")
	public static class DebounceDurationChangedEvent
			extends
				ComponentEvent<IronAjax> {
		public DebounceDurationChangedEvent(IronAjax source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addDebounceDurationChangedListener(
			ComponentEventListener<DebounceDurationChangedEvent> listener) {
		return addListener(DebounceDurationChangedEvent.class, listener);
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
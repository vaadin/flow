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

import { publishClient } from './publishClient';
import { getElementById, getElementByName, hasTag } from './ElementUtil';
import { isLitElement, whenRendered } from './LitUtils';
import { addReadyCallback } from './ReactUtils';
import * as ConnectionIndicator from './ConnectionIndicator';
import { checkForTouchDevice, getBrowserString, isIos } from './BrowserInfo';
import { resendRequest } from './XhrConnection';
import { isPropertyDefined } from './ExecuteJavaScriptElementUtils';
import { getShadowRootElement, handleError, recreateNodes, showPopover } from './SystemErrorHandler';
import { applyCaptures, createReturnChannelCallback } from './ClientJsonCodec';
import {
  addHtmlImportsReadyHandler,
  addOnloadHandler,
  getStyleSheetLength,
  runPromiseExpression,
  supportsHtmlWhenReady
} from './ResourceLoader';
import {
  calculateBootstrapTime,
  callAfterServerUpdates,
  getFetchStartTime,
  parseJSONResponse,
  removeStylesheetByIdFromDom
} from './MessageHandler';
import {
  getDomElementById,
  getDomRoot,
  getElementInShadowRootById,
  invokeWhenDefined,
  isInShadowRoot,
  isPolymerElement,
  isReady,
  mayBePolymerElement,
  searchForElementInShadowRoot,
  setListValueByIndex,
  setProperty,
  splice,
  storeNodeId
} from './PolymerUtils';
import { createConfig, doConnect, doDisconnect, doPush, isAtmosphereLoaded } from './AtmospherePushConnection';
import { getContextExecutionObject } from './ExecuteJavaScriptProcessor';
import { getLocalItem, getSessionItem, setLocalItem, setSessionItem } from './StorageUtil';
import { sendBeacon } from './MessageSender';
import { bindPolymerModelProperties } from './SimpleElementBindingStrategy';
import { isLocalStorageFlagEnabled } from './Console';
import { clearEventsList, getGwtStatsEvents, getPerformanceTiming, hasHighPrecisionTime, round } from './Profiler';
import {
  deferStartApplication,
  getJsoConfiguration,
  registerCallback,
  startApplicationImmediately
} from './Bootstrapper';
import {
  getAtmosphereVersion,
  getConfigBoolean,
  getConfigError,
  getConfigString,
  getConfigStringArray,
  getConfigValueMap,
  getVaadinVersion
} from './JsoConfiguration';
import {
  getMethods,
  getPolymerPropertyObject,
  initPromiseHandler,
  rejectPromises,
  removeMethod
} from './ServerEventObject';
import {
  createJsonObject,
  createJsonObjectWithoutPrototype,
  deleteJsProperty,
  equalsInJS,
  getAbsoluteUrl,
  getJsProperty,
  getKeys,
  hasJsProperty,
  hasOwnJsProperty,
  isAbsoluteUrl,
  isTrueish,
  isUndefined,
  redirect,
  setJsProperty,
  stringify,
  toPrettyJson
} from './WidgetUtil';

/**
 * Publishes the TypeScript implementations that the GWT engine calls into, under
 * `window.Vaadin.Flow.internal.*`. Invoked at the start of the engine's `init()`
 * (see `FlowClient.js`), so the implementations are present before any GWT code
 * runs, in every bootstrap path. As classes are migrated from Java to TypeScript
 * (see `MIGRATION_STRATEGY.md`), their implementations are registered here.
 */
export function registerInternals(): void {
  const flow = (((window as any).Vaadin ??= {}).Flow ??= {});
  const internal = (flow.internal ??= {});

  internal.publishClient = publishClient;
  internal.ElementUtil = { hasTag, getElementById, getElementByName };
  internal.LitUtils = { isLitElement, whenRendered };
  internal.ReactUtils = { addReadyCallback };
  internal.ConnectionIndicator = { ...ConnectionIndicator };
  internal.BrowserInfo = { getBrowserString, checkForTouchDevice, isIos };
  internal.XhrConnection = { resendRequest };
  internal.ExecuteJavaScriptElementUtils = { isPropertyDefined };
  internal.SystemErrorHandler = { recreateNodes, showPopover, getShadowRootElement, handleError };
  internal.ClientJsonCodec = { createReturnChannelCallback, applyCaptures };
  internal.ResourceLoader = {
    supportsHtmlWhenReady,
    addHtmlImportsReadyHandler,
    addOnloadHandler,
    getStyleSheetLength,
    runPromiseExpression
  };
  internal.MessageHandler = {
    removeStylesheetByIdFromDom,
    callAfterServerUpdates,
    calculateBootstrapTime,
    parseJSONResponse,
    getFetchStartTime
  };
  internal.PolymerUtils = {
    isPolymerElement,
    mayBePolymerElement,
    isInShadowRoot,
    isReady,
    getDomRoot,
    getDomElementById,
    searchForElementInShadowRoot,
    getElementInShadowRootById,
    invokeWhenDefined,
    setListValueByIndex,
    splice,
    storeNodeId,
    setProperty
  };
  internal.AtmospherePushConnection = { isAtmosphereLoaded, doPush, doDisconnect, doConnect, createConfig };
  internal.ExecuteJavaScriptProcessor = { getContextExecutionObject };
  internal.StorageUtil = { getLocalItem, setLocalItem, getSessionItem, setSessionItem };
  internal.MessageSender = { sendBeacon };
  internal.SimpleElementBindingStrategy = { bindPolymerModelProperties };
  internal.Console = { isLocalStorageFlagEnabled };
  internal.Profiler = { getPerformanceTiming, getGwtStatsEvents, clearEventsList, hasHighPrecisionTime, round };
  internal.Bootstrapper = { startApplicationImmediately, deferStartApplication, registerCallback, getJsoConfiguration };
  internal.JsoConfiguration = {
    getConfigString,
    getConfigValueMap,
    getConfigStringArray,
    getConfigBoolean,
    getConfigError,
    getVaadinVersion,
    getAtmosphereVersion
  };
  internal.ServerEventObject = {
    initPromiseHandler,
    removeMethod,
    getMethods,
    rejectPromises,
    getPolymerPropertyObject
  };
  internal.WidgetUtil = {
    redirect,
    getAbsoluteUrl,
    isAbsoluteUrl,
    getJsProperty,
    setJsProperty,
    hasOwnJsProperty,
    hasJsProperty,
    isUndefined,
    deleteJsProperty,
    isTrueish,
    getKeys,
    createJsonObject,
    createJsonObjectWithoutPrototype,
    equalsInJS,
    toPrettyJson,
    stringify
  };
}

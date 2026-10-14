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

/**
 * Triggers a file download from the given URL using the standard
 * <a href download> click pattern.
 *
 * The anchor is synthesised, clicked synchronously inside the caller's
 * gesture context, and removed. The browser then either navigates to the
 * URL (server responds with Content-Disposition: attachment) or saves the
 * resource directly when the download attribute applies.
 *
 * The {@code download} attribute is honoured only for same-origin URLs;
 * cross-origin responses must set Content-Disposition themselves for the
 * filename to take effect.
 */
function startDownload(url: string, filename?: string): void {
  const a = document.createElement('a');
  a.href = url;
  // Always set `download` so the browser saves the response rather than
  // navigating to it. Empty value lets the browser pick the filename from
  // Content-Disposition or the URL pathname; a non-empty value is the
  // suggested filename (honoured only same-origin). Cross-origin responses
  // without Content-Disposition: attachment still navigate — that's a
  // server-side concern this client helper can't override.
  a.download = filename ?? '';
  // Opt out of Vaadin's client-side router so the click reaches the
  // browser's native download handling instead of being intercepted as an
  // in-app navigation. Matches Anchor.setHref(DownloadHandler).
  a.setAttribute('router-ignore', '');
  // Hidden but in the document — some browsers ignore clicks on detached
  // anchors.
  a.style.display = 'none';
  document.body.appendChild(a);
  a.click();
  a.remove();
}

const $wnd = window as any;
$wnd.Vaadin ??= {};
$wnd.Vaadin.Flow ??= {};
$wnd.Vaadin.Flow.download = {
  start: startDownload
};

// Empty export to ensure TypeScript emits this as an ES module,
// which is required for Vite to load it via import.
export {};

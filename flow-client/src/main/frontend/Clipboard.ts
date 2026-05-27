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
 * Textual clipboard payload sent to the server, matching the Java
 * ClipboardPayload record.
 */
interface VaadinClipboardPayload {
  text: string | null;
  html: string | null;
}

/**
 * Reads the first item from the system clipboard and returns its text/plain
 * and text/html representations. Either field is {@code null} if the
 * corresponding MIME type is not present.
 *
 * The caller is expected to be inside a transient user gesture and to have
 * been granted the {@code clipboard-read} permission; otherwise
 * {@code navigator.clipboard.read} rejects and this function propagates the
 * rejection.
 */
async function readClipboardPayload(): Promise<VaadinClipboardPayload | null> {
  const items = await navigator.clipboard.read();
  if (!items.length) {
    return null;
  }
  const item = items[0];
  const get = async (type: string): Promise<string | null> =>
    item.types.includes(type) ? (await item.getType(type)).text() : null;
  return {
    text: await get('text/plain'),
    html: await get('text/html')
  };
}

/**
 * Writes the given text/plain and/or text/html representations to the system
 * clipboard as a single ClipboardItem. Either argument may be {@code null} to
 * omit that MIME type; at least one is expected to be non-null (the caller
 * enforces this).
 *
 * The caller is expected to be inside a transient user gesture; otherwise
 * {@code navigator.clipboard.write} rejects and this function propagates the
 * rejection.
 *
 * Resolves with the {@code text/plain} value if present, otherwise with the
 * {@code text/html} value — so the caller's success handler sees the exact
 * string that reached the clipboard.
 */
async function writeClipboardPayload(text: string | null, html: string | null): Promise<string | null> {
  const entries: Record<string, Blob> = {};
  if (text !== null) {
    entries['text/plain'] = new Blob([text], { type: 'text/plain' });
  }
  if (html !== null) {
    entries['text/html'] = new Blob([html], { type: 'text/html' });
  }
  await navigator.clipboard.write([new ClipboardItem(entries)]);
  return text !== null ? text : html;
}

const $wnd = window as any;
$wnd.Vaadin ??= {};
$wnd.Vaadin.Flow ??= {};
$wnd.Vaadin.Flow.clipboard = {
  readPayload: readClipboardPayload,
  writePayload: writeClipboardPayload
};

// Empty export to ensure TypeScript emits this as an ES module,
// which is required for Vite to load it via import.
export {};

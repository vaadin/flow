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
 * Re-encodes the given {@code <img>} as {@code image/png} via a canvas
 * round-trip. The source can be any rasterisable format the browser already
 * decodes ({@code image/png}, {@code image/jpeg}, {@code image/svg+xml}, ...);
 * the output is always a {@code Promise<Blob>} of {@code image/png}, the only
 * image MIME type every browser's asynchronous Clipboard API accepts on write.
 *
 * Cross-origin images need {@code crossorigin="anonymous"} on the {@code <img>}
 * plus matching CORS headers, otherwise the canvas is tainted and
 * {@code toBlob} throws.
 */
function imageToPngBlob(img: HTMLImageElement): Promise<Blob> {
  return new Promise((resolve, reject) => {
    const draw = () => {
      try {
        const width = img.naturalWidth || img.width;
        const height = img.naturalHeight || img.height;
        if (!width || !height) {
          reject(new Error('image has no intrinsic size'));
          return;
        }
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        if (!ctx) {
          reject(new Error('2D canvas context not available'));
          return;
        }
        ctx.drawImage(img, 0, 0, width, height);
        canvas.toBlob((png) => (png ? resolve(png) : reject(new Error('canvas.toBlob returned null'))), 'image/png');
      } catch (err) {
        reject(err);
      }
    };
    if (img.complete) {
      // `complete` is also true for an image that already failed to load or has
      // an empty src; those have naturalWidth === 0 and their load/error events
      // have already fired and will never fire again, so we must settle here
      // rather than wait for an event that never comes.
      if (img.naturalWidth > 0) {
        draw();
      } else {
        reject(new Error('image failed to load or has empty src'));
      }
    } else {
      img.addEventListener('load', draw, { once: true });
      img.addEventListener('error', () => reject(new Error('image load failed')), { once: true });
    }
  });
}

/**
 * Writes any combination of text/plain, text/html and image/png to the system
 * clipboard as a single ClipboardItem. Any argument may be {@code null} to omit
 * that MIME type; at least one is expected to be non-null (the caller enforces
 * this). The image argument is the source {@code <img>}; it is re-encoded as
 * {@code image/png} via {@link imageToPngBlob} and the resulting
 * {@code Promise<Blob>} is fed directly to {@code ClipboardItem} so the
 * {@code navigator.clipboard.write} call stays synchronous inside the user
 * gesture (Safari otherwise loses activation on the first await).
 *
 * The caller is expected to be inside a transient user gesture; otherwise
 * {@code navigator.clipboard.write} rejects and this function propagates the
 * rejection.
 *
 * Resolves with the {@code text/plain} value if present, otherwise with the
 * {@code text/html} value, otherwise with {@code null} (image-only case).
 */
async function writeClipboardPayload(
  text: string | null,
  html: string | null,
  image: HTMLImageElement | null
): Promise<string | null> {
  const entries: Record<string, string | Promise<Blob>> = {};
  if (text !== null) {
    entries['text/plain'] = text;
  }
  if (html !== null) {
    entries['text/html'] = html;
  }
  if (image !== null) {
    entries['image/png'] = imageToPngBlob(image);
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

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

window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.clipboard = {
  setupCopyOnClick: function (element) {
    if (!element.__clipboardClickHandler) {
      element.__clipboardClickHandler = function () {
        var text = element.__clipboardText || '';
        navigator.clipboard.writeText(text);
      };
      element.addEventListener('click', element.__clipboardClickHandler);
    }
  },

  setupCopyOnClickWithCallbacks: function (element, onSuccess, onError) {
    if (element.__clipboardClickHandler) {
      element.removeEventListener('click', element.__clipboardClickHandler);
    }
    element.__clipboardClickHandler = function () {
      var text = element.__clipboardText || '';
      navigator.clipboard.writeText(text).then(
        function () {
          onSuccess();
        },
        function () {
          onError();
        }
      );
    };
    element.addEventListener('click', element.__clipboardClickHandler);
  },

  setupCopyOnClickFromSource: function (element, sourceElement) {
    element.__clipboardSourceElement = sourceElement;
    if (element.__clipboardClickHandler) {
      element.removeEventListener('click', element.__clipboardClickHandler);
    }
    element.__clipboardClickHandler = function () {
      var src = element.__clipboardSourceElement;
      var text = src ? (src.value !== undefined && src.value !== null ? String(src.value) : src.textContent || '') : '';
      navigator.clipboard.writeText(text);
    };
    element.addEventListener('click', element.__clipboardClickHandler);
  },

  setupCopyImageOnClick: function (element, imageElement) {
    element.__clipboardImageElement = imageElement;
    if (element.__clipboardClickHandler) {
      element.removeEventListener('click', element.__clipboardClickHandler);
    }
    element.__clipboardClickHandler = function () {
      var img = element.__clipboardImageElement;
      if (!img || !img.src) return;
      fetch(img.src)
        .then(function (r) {
          return r.blob();
        })
        .then(function (blob) {
          var type = blob.type || 'image/png';
          var pngBlob = blob;
          if (type !== 'image/png') {
            pngBlob = new Blob([blob], { type: 'image/png' });
          }
          return navigator.clipboard.write([new ClipboardItem(Object.fromEntries([[pngBlob.type, pngBlob]]))]);
        });
    };
    element.addEventListener('click', element.__clipboardClickHandler);
  },

  cleanupCopyOnClick: function (element) {
    if (element.__clipboardClickHandler) {
      element.removeEventListener('click', element.__clipboardClickHandler);
      delete element.__clipboardClickHandler;
      delete element.__clipboardText;
      delete element.__clipboardSourceElement;
      delete element.__clipboardImageElement;
    }
  },

  writeText: function (text) {
    return navigator.clipboard.writeText(text);
  },

  readText: function () {
    return navigator.clipboard.readText();
  },

  writeImage: function (url) {
    return fetch(url)
      .then(function (r) {
        return r.blob();
      })
      .then(function (blob) {
        return navigator.clipboard.write([new ClipboardItem(Object.fromEntries([[blob.type, blob]]))]);
      });
  },

  setupPasteListener: function (element, channel) {
    if (element.__clipboardPasteHandler) {
      element.removeEventListener('paste', element.__clipboardPasteHandler);
    }
    element.__clipboardPasteHandler = async function (e) {
      e.preventDefault();
      var text = e.clipboardData.getData('text/plain') || null;
      var html = e.clipboardData.getData('text/html') || null;
      var files = [];
      for (var i = 0; i < e.clipboardData.items.length; i++) {
        if (e.clipboardData.items[i].kind === 'file') {
          files.push(e.clipboardData.items[i].getAsFile());
        }
      }
      var uploadUrl = element.getAttribute('__clipboard-paste-upload');
      if (uploadUrl && files.length > 0) {
        for (var i = 0; i < files.length; i++) {
          var fd = new FormData();
          fd.append('file', files[i], files[i].name || 'pasted-file');
          await fetch(uploadUrl, { method: 'POST', body: fd });
        }
      }
      channel(text, html, files.length);
    };
    element.addEventListener('paste', element.__clipboardPasteHandler);
  },

  cleanupPasteListener: function (element) {
    if (element.__clipboardPasteHandler) {
      element.removeEventListener('paste', element.__clipboardPasteHandler);
      delete element.__clipboardPasteHandler;
    }
  }
};

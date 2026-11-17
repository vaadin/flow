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

/*
 * Gives the browser tab a `window.name`, which the server uses to recognize the
 * tab after a reload or a navigation, for example for @PreserveOnRefresh.
 *
 * Browsers clear `window.name` when the tab navigates to another site, so
 * returning from a payment provider or a login service would look like a new
 * tab. The name is therefore also kept in `sessionStorage`, which survives that
 * round trip. Browsers also copy `sessionStorage` into a duplicated tab and into
 * a window opened with `window.open()`, which must not share the name of the
 * original tab. A name found only in `sessionStorage` is therefore taken back
 * only when no other open page answers that it is using it.
 */
import { getSessionItem, setSessionItem } from './internal/client/StorageUtil';

const STORAGE_KEY = 'vaadin.windowName';
const CHANNEL_NAME = 'vaadin-window-name';
// Long enough for a page that is busy with other work to answer, and only
// waited for when the name has to be taken back from sessionStorage
const ANSWER_TIMEOUT_MS = 250;

interface WindowNameMessage {
  type: 'query' | 'in-use';
  name: string;
}

let channel: BroadcastChannel | undefined;
let ready: Promise<void> = Promise.resolve();

function readStoredName(): string | null {
  try {
    return getSessionItem(STORAGE_KEY);
  } catch {
    // Storage is blocked, for example by privacy settings
    return null;
  }
}

function storeName(name: string): void {
  try {
    setSessionItem(STORAGE_KEY, name);
  } catch {
    // Storage is blocked or full; window.name still identifies the tab
  }
}

function isInUseByAnotherPage(openChannel: BroadcastChannel, name: string): Promise<boolean> {
  return new Promise((resolve) => {
    const onMessage = (event: MessageEvent<WindowNameMessage>) => {
      if (event.data?.type === 'in-use' && event.data.name === name) {
        finish(true);
      }
    };
    const timeout = setTimeout(() => finish(false), ANSWER_TIMEOUT_MS);
    function finish(inUse: boolean) {
      clearTimeout(timeout);
      openChannel.removeEventListener('message', onMessage);
      resolve(inUse);
    }
    openChannel.addEventListener('message', onMessage);
    openChannel.postMessage({ type: 'query', name } satisfies WindowNameMessage);
  });
}

async function resolveWindowName(openChannel: BroadcastChannel): Promise<void> {
  if (!window.name) {
    const storedName = readStoredName();
    window.name =
      storedName && !(await isInUseByAnotherPage(openChannel, storedName)) ? storedName : `v-${Math.random()}`;
  }
  storeName(window.name);
}

/**
 * Makes sure that the browser tab has a `window.name` that identifies it, and
 * starts answering other pages that ask whether the name is in use. Keeps a
 * name that is already set, takes the name stored in `sessionStorage` back when
 * no other page uses it, and generates a new name otherwise.
 *
 * @returns a promise that resolves when `window.name` is set
 */
export function initWindowName(): Promise<void> {
  channel?.close();
  const openChannel = new BroadcastChannel(CHANNEL_NAME);
  openChannel.addEventListener('message', (event: MessageEvent<WindowNameMessage>) => {
    if (event.data?.type === 'query' && window.name && event.data.name === window.name) {
      openChannel.postMessage({ type: 'in-use', name: window.name } satisfies WindowNameMessage);
    }
  });
  channel = openChannel;
  ready = resolveWindowName(openChannel);
  return ready;
}

/**
 * Waits until a `window.name` that {@link initWindowName} is resolving has been
 * set. Resolves right away if the name is not being resolved.
 *
 * @returns a promise that resolves when `window.name` can be read
 */
export function windowNameReady(): Promise<void> {
  return ready;
}

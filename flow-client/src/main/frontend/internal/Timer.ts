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

// TypeScript port of elemental.util.Timer, the GWT elemental primitive Debouncer
// schedules on. It is not part of a com.vaadin package, so it lives here as a
// standalone elemental primitive next to the other elemental-style ports (e.g.
// EventRemover).

/**
 * A timer that can be scheduled once or repeatedly, and cancelled: `schedule` is
 * one-shot, `scheduleRepeating` is an interval, `cancel` stops either.
 */
export class Timer {
  #handle: ReturnType<typeof setTimeout> | null = null;

  #repeating = false;

  readonly #task: () => void;

  constructor(task: () => void) {
    this.#task = task;
  }

  schedule(ms: number): void {
    this.cancel();
    this.#repeating = false;
    this.#handle = setTimeout(() => {
      this.#handle = null;
      this.#task();
    }, ms);
  }

  scheduleRepeating(ms: number): void {
    this.cancel();
    this.#repeating = true;
    this.#handle = setInterval(() => this.#task(), ms);
  }

  cancel(): void {
    if (this.#handle !== null) {
      if (this.#repeating) {
        clearInterval(this.#handle);
      } else {
        clearTimeout(this.#handle);
      }
      this.#handle = null;
    }
  }
}

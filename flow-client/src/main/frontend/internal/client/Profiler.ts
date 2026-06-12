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
 * Time helpers used by XHR / UI timing logs. The original
 * `com.vaadin.client.Profiler` aggregated GWT `__gwtStatsEvent` samples, but
 * that runtime hook is gone with the GWT bundle, so only the
 * `performance.now()`-based timing wrappers remain.
 */
export const Profiler = {
  /**
   * Returns a high-resolution timestamp suitable for `getRelativeTimeString`
   * deltas. The result is only meaningful relative to another value from this
   * function (it does not represent wall-clock time).
   */
  getRelativeTimeMillis(): number {
    return performance.now();
  },

  /**
   * Formats milliseconds elapsed since {@code reference} (a value returned by
   * {@link Profiler.getRelativeTimeMillis}), rounded to 3 decimals.
   */
  getRelativeTimeString(reference: number): string {
    return String(round(performance.now() - reference, 3));
  }
};

function round(num: number, exp: number): number {
  return +`${Math.round(+`${num}e+${exp}`)}e-${exp}`;
}

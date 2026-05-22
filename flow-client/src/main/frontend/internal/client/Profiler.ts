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

import { Console } from './Console';

/**
 * Lightweight profiler. Migrated from `com.vaadin.client.Profiler`.
 *
 * The original Java implementation collected timings via the GWT
 * `__gwtStatsEvent` hook only when the `vaadin.profiler` GWT compile flag was
 * set; production builds always used the disabled no-op subclass. The
 * `__gwtStatsEvent` stream is GWT-specific and disappears with the GWT
 * runtime, so the aggregation, pretty-printing, and result-consumer plumbing
 * become unreachable after migration. The time helpers remain useful (they
 * power XHR/UI timing logs) and are kept as thin wrappers over
 * `performance.now()`.
 *
 * Java callers reach this module via `Vaadin.Flow.internal.client.Profiler`.
 */
export const Profiler = {
  /** Profiling collection is always disabled post-GWT. */
  isEnabled(): boolean {
    return false;
  },

  /** Marks entry into a named block; no-op since collection is disabled. */
  enter(_name: string): void {
    // Compile-time stripped in original Java; runtime no-op here.
  },

  /** Marks exit from a named block; no-op since collection is disabled. */
  leave(_name: string): void {
    // Compile-time stripped in original Java; runtime no-op here.
  },

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
  },

  /**
   * Resets collection state. With aggregation removed there is nothing to
   * clear, but the entry point is kept so existing Java guards
   * (`if (Profiler.isEnabled()) Profiler.reset()`) keep linking.
   */
  reset(): void {
    // No-op
  },

  /**
   * Sets up the profiler. Previously chose between high-resolution and
   * default time suppliers; `performance.now()` is universally available so
   * no setup is needed.
   */
  initialize(): void {
    // No-op
  },

  /** Would dump aggregated timings to the console. */
  logTimings(): void {
    Console.warn('Profiler is not enabled, no data has been collected.');
  },

  /** Would dump browser bootstrap timings to the console. */
  logBootstrapTimings(): void {
    // No-op: bootstrap timing collection required the GWT runtime hook.
  }
};

function round(num: number, exp: number): number {
  return +`${Math.round(+`${num}e+${exp}`)}e-${exp}`;
}

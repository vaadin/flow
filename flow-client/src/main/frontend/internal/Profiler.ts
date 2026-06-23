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

// Self-contained profiler leaf helpers migrated from Profiler.java, registered
// on window.Vaadin.Flow.internal.Profiler by registerInternals; the Java methods
// delegate here. The GwtStatsEvent JSO accessors, the __gwtStatsEvent
// logger setup (logGwtEvent/ensureLogger/ensureNoLogger) and the
// RelativeTimeSupplier getRelativeTime implementations stay in Java for now.
// Also bundled to ES5 for the HtmlUnit used by GwtTests.

interface PerformanceTiming {
  performance?: { timing?: Record<string, number>; now?: () => number };
}

interface GwtStats {
  Vaadin: { Flow: { gwtStatsEvents?: unknown[] } };
  __gwtStatsEvent?: (event?: unknown) => boolean;
}

/** The named window.performance.timing value, or 0 if unavailable. */
export function getPerformanceTiming(name: string): number {
  const timing = (window as unknown as PerformanceTiming).performance?.timing;
  return timing && timing[name] ? timing[name] : 0;
}

/** The collected GWT stats events, or an empty array. */
export function getGwtStatsEvents(): unknown[] {
  return (window as unknown as GwtStats).Vaadin.Flow.gwtStatsEvents || [];
}

/** Resets the collected GWT stats events list. */
export function clearEventsList(): void {
  (window as unknown as GwtStats).Vaadin.Flow.gwtStatsEvents = [];
}

/**
 * Installs the __gwtStatsEvent logger (collecting events into the events list)
 * if it is not already present, initializing the events list if needed.
 */
export function ensureLogger(): void {
  const w = window as unknown as GwtStats;
  if (typeof w.__gwtStatsEvent !== 'function') {
    if (typeof w.Vaadin.Flow.gwtStatsEvents !== 'object') {
      w.Vaadin.Flow.gwtStatsEvents = [];
    }
    w.__gwtStatsEvent = function (event?: unknown): boolean {
      w.Vaadin.Flow.gwtStatsEvents!.push(event);
      return true;
    };
  }
}

/**
 * Removes the events list and neutralizes the logger function if it looks like
 * the one installed by ensureLogger.
 */
export function ensureNoLogger(): void {
  const w = window as unknown as GwtStats;
  if (typeof w.Vaadin.Flow.gwtStatsEvents === 'object') {
    delete w.Vaadin.Flow.gwtStatsEvents;
    if (typeof w.__gwtStatsEvent === 'function') {
      w.__gwtStatsEvent = function (): boolean {
        return true;
      };
    }
  }
}

/** Whether the browser provides a high-precision performance.now() clock. */
export function hasHighPrecisionTime(): boolean {
  const perf = (window as unknown as PerformanceTiming).performance;
  return !!perf && typeof perf.now === 'function';
}

/** Rounds the number to the given number of decimal places. */
export function round(num: number, exp: number): number {
  const rounded = Math.round(Number(`${num}e+${exp}`));
  return Number(`${rounded}e-${exp}`);
}

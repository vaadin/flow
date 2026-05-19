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

interface GwtStatsContainer {
  Vaadin?: { Flow?: { gwtStatsEvents?: unknown[] } };
  __gwtStatsEvent?: (event: unknown) => boolean;
  performance?: Performance & { timing?: Record<string, number> };
}

function gwtStats(): GwtStatsContainer {
  return globalThis as unknown as GwtStatsContainer;
}

function flowNamespace(): { gwtStatsEvents?: unknown[] } | undefined {
  return gwtStats().Vaadin?.Flow;
}

/**
 * Browser-touching helpers from `com.vaadin.client.Profiler`. Reached from
 * GWT code via the `NativeProfiler` JsType shim. The pure-Java aggregation
 * (`Block`, `Node`, `enter`/`leave`, the {@code logTimings} pretty-printer,
 * etc.) stays in `Profiler.java`. The inner {@code GwtStatsEvent} JSO and
 * the two `RelativeTimeSupplier` implementations also stay Java because
 * their methods sit on JS instances.
 */
export const Profiler = {
  // eslint-disable-next-line @typescript-eslint/max-params
  logGwtEvent(evtGroup: string, moduleName: string, name: string, type: string, relativeMillis: number): void {
    if (typeof gwtStats().__gwtStatsEvent === 'function') {
      gwtStats().__gwtStatsEvent!({
        evtGroup,
        moduleName,
        millis: Date.now(),
        sessionId: undefined,
        subSystem: name,
        type,
        relativeMillis
      });
    }
  },

  getPerformanceTiming(name: string): number {
    return gwtStats().performance?.timing?.[name] ?? 0;
  },

  getGwtStatsEvents(): unknown[] {
    return flowNamespace()?.gwtStatsEvents ?? [];
  },

  ensureLogger(): void {
    const win = gwtStats();
    const flow = flowNamespace();
    if (typeof win.__gwtStatsEvent !== 'function') {
      if (flow && typeof flow.gwtStatsEvents !== 'object') {
        flow.gwtStatsEvents = [];
      }
      win.__gwtStatsEvent = (event: unknown) => {
        flowNamespace()?.gwtStatsEvents?.push(event);
        return true;
      };
    }
  },

  ensureNoLogger(): void {
    const flow = flowNamespace();
    if (flow && typeof flow.gwtStatsEvents === 'object') {
      delete flow.gwtStatsEvents;
      if (typeof gwtStats().__gwtStatsEvent === 'function') {
        gwtStats().__gwtStatsEvent = () => true;
      }
    }
  },

  clearEventsList(): unknown[] {
    const flow = flowNamespace();
    if (flow) {
      flow.gwtStatsEvents = [];
    }
    return [];
  },

  hasHighPrecisionTime(): boolean {
    return typeof gwtStats().performance?.now === 'function';
  },

  defaultRelativeTime(): number {
    return Date.now();
  },

  highResolutionRelativeTime(): number {
    return gwtStats().performance!.now();
  },

  round(num: number, exp: number): number {
    return +`${Math.round(+`${num}e+${exp}`)}e-${exp}`;
  }
};

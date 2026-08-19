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
 * Lightweight profiling tool that can be used to collect profiling data with
 * zero overhead unless enabled. To enable profiling, add
 * `<set-property name="vaadin.profiler" value="true" />` to your .gwt.xml file.
 *
 * TypeScript port of com.vaadin.client.Profiler. The public surface
 * (isEnabled/enter/leave/reset/initialize/logTimings/
 * logBootstrapTimings/getRelativeTimeMillis/getRelativeTimeString/
 * setProfilerResultConsumer plus the Node tree and ProfilerResultConsumer) is
 * exported; the private native helpers of Profiler.java (logGwtEvent,
 * ensureLogger, ensureNoLogger, getGwtStatsEvents, clearEventsList,
 * getPerformanceTiming, hasHighPrecisionTime, round, the relative-time
 * suppliers and the GwtStatsEvent reader) are non-exported module-local. GWT
 * enabled profiling at compile time via deferred binding on the
 * `vaadin.profiler` property; the TypeScript engine has no deferred binding, so
 * the bootstrap toggles it at runtime through setEnabled().
 *
 * @since 1.0
 */

import { Console } from './Console';

const EVT_GROUP = 'VaadinProfiler';

// GWT.getModuleName() has no TypeScript-engine equivalent, so the payload
// carries an empty module name; the field is kept for shape-compatibility with
// existing __gwtStatsEvent listeners.
const MODULE_NAME = '';

interface GwtStatsEvent {
  evtGroup?: string;
  moduleName?: string;
  millis?: number;
  sessionId?: unknown;
  subSystem?: string;
  type?: string;
  relativeMillis?: number;
}

interface GwtStatsWindow {
  Vaadin: { Flow: { gwtStatsEvents?: GwtStatsEvent[] } };
  __gwtStatsEvent?: (event?: GwtStatsEvent) => boolean;
  performance?: { timing?: Record<string, number>; now?: () => number };
}

function gwtWindow(): GwtStatsWindow {
  return window as unknown as GwtStatsWindow;
}

/**
 * Consumer that receives the profiler results, mirroring
 * Profiler.ProfilerResultConsumer.
 * <p>
 * <b>Warning!</b> This interface is most likely to change in the future.
 *
 * @since 1.0
 */
export interface ProfilerResultConsumer {
  addProfilerData(rootNode: Node, totals: Node[]): void;
  addBootstrapData(timings: Map<string, number>): void;
}

/** Rounds to three significant figures, mirroring Node.roundToSignificantFigures. */
function roundToSignificantFigures(num: number): number {
  // Number of significant digits
  const n = 3;
  if (num < 0.0005) {
    return 0;
  }

  const d = Math.ceil(Math.log10(num < 0 ? -num : num));
  const power = n - d;

  const magnitude = Math.pow(10, power);
  const shifted = Math.round(num * magnitude);
  return shifted / magnitude;
}

/**
 * A hierarchical representation of the time spent running a named block of
 * code. Mirrors Profiler.Node.
 * <p>
 * <b>Warning!</b> This class is most likely to change in the future.
 */
export class Node {
  readonly #name: string | null;

  readonly #children = new Map<string, Node>();

  #time = 0;

  #count = 0;

  #enterTime = 0;

  #minTime = 1000000000;

  #maxTime = 0;

  /**
   * Creates a new node with the given name.
   *
   * @param name the name of the node
   */
  constructor(name: string | null) {
    this.#name = name;
  }

  /**
   * Gets the name of the node.
   *
   * @return the name of the node
   */
  getName(): string | null {
    return this.#name;
  }

  /**
   * Creates a new child node or retrieves an existing child and updates its
   * total time and hit count.
   *
   * @param name the name of the child
   * @param timestamp the timestamp for when the node is entered
   * @return the child node object
   */
  enterChild(name: string, timestamp: number): Node {
    let child = this.#children.get(name);
    if (child === undefined) {
      child = new Node(name);
      this.#children.set(name, child);
    }
    child.#enterTime = timestamp;
    child.#count++;
    return child;
  }

  /**
   * Gets the total time spent in this node, including sub nodes, in ms.
   *
   * @return the total time spent, in milliseconds
   */
  getTimeSpent(): number {
    return this.#time;
  }

  /**
   * Gets the minimum time spent for one invocation, including sub nodes, in ms.
   *
   * @return the time spent for the fastest invocation, in milliseconds
   */
  getMinTimeSpent(): number {
    return this.#minTime;
  }

  /**
   * Gets the maximum time spent for one invocation, including sub nodes, in ms.
   *
   * @return the time spent for the slowest invocation, in milliseconds
   */
  getMaxTimeSpent(): number {
    return this.#maxTime;
  }

  /**
   * Gets the number of times this node has been entered.
   *
   * @return the number of times the node has been entered
   */
  getCount(): number {
    return this.#count;
  }

  /**
   * Gets the total time spent in this node, excluding sub nodes, in ms.
   *
   * @return the total time spent, in milliseconds
   */
  getOwnTime(): number {
    let time = this.getTimeSpent();
    for (const node of this.#children.values()) {
      time -= node.getTimeSpent();
    }
    return time;
  }

  /**
   * Gets the child nodes of this node.
   *
   * @return a collection of child nodes
   */
  getChildren(): Node[] {
    return Array.from(this.#children.values());
  }

  toString(): string {
    return this.getStringRepresentation('');
  }

  getStringRepresentation(prefix: string): string {
    if (this.getName() === null) {
      return '';
    }
    let msg = `${prefix} ${this.getName()} in ${roundToSignificantFigures(this.getTimeSpent())} ms.`;
    if (this.getCount() > 1) {
      msg +=
        ` Invoked ${this.getCount()} times (${roundToSignificantFigures(this.getTimeSpent() / this.getCount())}` +
        ` ms per time, min ${roundToSignificantFigures(this.getMinTimeSpent())}` +
        ` ms, max ${roundToSignificantFigures(this.getMaxTimeSpent())} ms).`;
    }
    if (this.#children.size > 0) {
      const ownTime = this.getOwnTime();
      msg += ` ${roundToSignificantFigures(ownTime)} ms spent in own code`;
      if (this.getCount() > 1) {
        msg += ` (${roundToSignificantFigures(ownTime / this.getCount())} ms per time)`;
      }
      msg += '.';
    }
    return msg;
  }

  sumUpTotals(totals: Map<string, Node>): void {
    const name = this.getName();
    if (name !== null) {
      let totalNode = totals.get(name);
      if (totalNode === undefined) {
        totalNode = new Node(name);
        totals.set(name, totalNode);
      }

      totalNode.#time += this.getOwnTime();
      totalNode.#count += this.getCount();
      totalNode.#minTime = roundToSignificantFigures(Math.min(totalNode.#minTime, this.getMinTimeSpent()));
      totalNode.#maxTime = roundToSignificantFigures(Math.max(totalNode.#maxTime, this.getMaxTimeSpent()));
    }
    for (const node of this.#children.values()) {
      node.sumUpTotals(totals);
    }
  }

  /**
   * Marks the time spent in the child node.
   *
   * @param timestamp the timestamp for when the node was left
   */
  leave(timestamp: number): void {
    const elapsed = timestamp - this.#enterTime;
    this.#time += elapsed;
    this.#enterTime = 0;
    if (elapsed < this.#minTime) {
      this.#minTime = elapsed;
    }
    if (elapsed > this.#maxTime) {
      this.#maxTime = elapsed;
    }
  }
}

// Whether profiling data gathering is enabled. In the GWT client this was a
// compile-time choice made by deferred binding on the `vaadin.profiler`
// property (Profiler.EnabledProfiler); the TypeScript engine has no deferred
// binding, so the bootstrap toggles it at runtime through setEnabled(). It
// defaults to disabled, matching a standard (non-profiling) build.
let profilingEnabled = false;

// Supplies a time relative to page load, in ms. initialize() upgrades this to
// performance.now() when available; the default low-resolution supplier keeps
// getRelativeTimeMillis() usable before initialize() is called.
let relativeTimeSupplier: () => number = () => Date.now();

let consumer: ProfilerResultConsumer | null = null;

/** Reports a profiler event to the __gwtStatsEvent logger. Private in Profiler.java. */
function logGwtEvent(name: string, type: string): void {
  gwtWindow().__gwtStatsEvent?.({
    evtGroup: EVT_GROUP,
    moduleName: MODULE_NAME,
    millis: Date.now(),
    sessionId: undefined,
    subSystem: name,
    type,
    relativeMillis: getRelativeTimeMillis()
  });
}

/** The named window.performance.timing value, or 0 if unavailable. */
function getPerformanceTiming(name: string): number {
  const timing = gwtWindow().performance?.timing;
  return timing && timing[name] ? timing[name] : 0;
}

/** The collected GWT stats events, or an empty array. */
function getGwtStatsEvents(): GwtStatsEvent[] {
  return gwtWindow().Vaadin.Flow.gwtStatsEvents || [];
}

/** Resets the collected GWT stats events list. */
function clearEventsList(): void {
  gwtWindow().Vaadin.Flow.gwtStatsEvents = [];
}

/**
 * Installs the __gwtStatsEvent logger (collecting events into the events list)
 * if it is not already present, initializing the events list if needed.
 */
function ensureLogger(): void {
  const w = gwtWindow();
  if (typeof w.__gwtStatsEvent !== 'function') {
    if (typeof w.Vaadin.Flow.gwtStatsEvents !== 'object') {
      w.Vaadin.Flow.gwtStatsEvents = [];
    }
    w.__gwtStatsEvent = function (event?: GwtStatsEvent): boolean {
      w.Vaadin.Flow.gwtStatsEvents!.push(event!);
      return true;
    };
  }
}

/**
 * Removes the events list and neutralizes the logger function if it looks like
 * the one installed by ensureLogger.
 */
function ensureNoLogger(): void {
  const w = gwtWindow();
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
function hasHighPrecisionTime(): boolean {
  const perf = gwtWindow().performance;
  return !!perf && typeof perf.now === 'function';
}

/** Rounds the number up to the given number of decimal positions. */
function round(num: number, exp: number): number {
  const rounded = Math.round(Number(`${num}e+${exp}`));
  return Number(`${rounded}e-${exp}`);
}

/** The event name, resolving the profiler group to just its subsystem name. */
function getEventName(event: GwtStatsEvent): string {
  const group = event.evtGroup;
  if (group === EVT_GROUP) {
    return event.subSystem ?? '';
  }
  return `${group}.${event.subSystem}`;
}

/**
 * Enables or disables profiling data gathering. Replaces the GWT deferred
 * binding on the `vaadin.profiler` property (Profiler.EnabledProfiler); the
 * engine bootstrap calls this to select the profiling build at runtime. There
 * is no compile-time equivalent in the TypeScript engine.
 */
export function setEnabled(enabled: boolean): void {
  profilingEnabled = enabled;
}

/**
 * Whether the profiling data gathering is enabled. Mirrors Profiler.isEnabled.
 *
 * @return `true` if the profiling is enabled, else `false`
 */
export function isEnabled(): boolean {
  return profilingEnabled;
}

/**
 * Enters a named block. There should always be a matching invocation of
 * {@link leave} when leaving the block.
 *
 * @param name the name of the entered block
 */
export function enter(name: string): void {
  if (isEnabled()) {
    logGwtEvent(name, 'begin');
  }
}

/**
 * Leaves a named block. There should always be a matching invocation of
 * {@link enter} when entering the block.
 *
 * @param name the name of the left block
 */
export function leave(name: string): void {
  if (isEnabled()) {
    logGwtEvent(name, 'end');
  }
}

/**
 * Returns time relative to the particular page load time. The value should not
 * be used directly but rather the difference between two values returned by
 * this method should be used to compare measurements.
 *
 * @return the relative time in milliseconds
 */
export function getRelativeTimeMillis(): number {
  return relativeTimeSupplier();
}

/** Resets the collected profiler data. No-op unless profiling is enabled. */
export function reset(): void {
  if (isEnabled()) {
    // Old implementations might call reset for initialization, so ensure it is
    // initialized here as well. Initialization has no side effects if already
    // done.
    initialize();

    clearEventsList();
  }
}

/**
 * Initializes the profiler. This should be done before calling any other
 * function in this class. This method has no side effects if the initialization
 * has already been done.
 * <p>
 * Should be called even if the profiler is not enabled, because it then removes
 * a logger function that might have been included in the HTML page and that
 * would otherwise leak memory.
 */
export function initialize(): void {
  if (hasHighPrecisionTime()) {
    relativeTimeSupplier = () => gwtWindow().performance!.now!();
  } else {
    relativeTimeSupplier = () => Date.now();
  }
  if (isEnabled()) {
    ensureLogger();
  } else {
    ensureNoLogger();
  }
}

/** Outputs the gathered profiling data to the registered result consumer. */
export function logTimings(): void {
  if (!isEnabled()) {
    Console.warn('Profiler is not enabled, no data has been collected.');
    return;
  }

  const stack: Node[] = [];
  const rootNode = new Node(null);
  stack.push(rootNode);
  const gwtStatsEvents = getGwtStatsEvents();
  if (gwtStatsEvents.length === 0) {
    Console.warn('No profiling events recorded, this might happen if another __gwtStatsEvent handler is installed.');
    return;
  }

  const extendedTimeNodes = new Set<Node>();
  for (const gwtStatsEvent of gwtStatsEvents) {
    if (gwtStatsEvent.evtGroup !== EVT_GROUP) {
      // Only log our own events to avoid problems with events which are not of
      // type start+end
      continue;
    }
    const eventName = getEventName(gwtStatsEvent);
    const type = gwtStatsEvent.type;
    const isExtendedEvent = 'relativeMillis' in gwtStatsEvent;
    const isBeginEvent = type === 'begin';
    const relativeMillis = gwtStatsEvent.relativeMillis ?? 0;
    const eventMillis = gwtStatsEvent.millis ?? 0;

    let stackTop = stack[stack.length - 1];
    let inEvent = eventName === stackTop.getName() && !isBeginEvent;

    if (!inEvent && stack.length >= 2 && eventName === stack[stack.length - 2].getName() && !isBeginEvent) {
      // back out of sub event
      if (extendedTimeNodes.has(stackTop) && isExtendedEvent) {
        stackTop.leave(relativeMillis);
      } else {
        stackTop.leave(eventMillis);
      }
      stack.pop();
      stackTop = stack[stack.length - 1];

      inEvent = true;
    }

    if (type === 'end') {
      if (!inEvent) {
        Console.error(`Got end event for ${eventName} but is currently in ${stackTop.getName()}`);
        return;
      }
      const previousStackTop = stack.pop()!;
      if (extendedTimeNodes.has(previousStackTop)) {
        previousStackTop.leave(relativeMillis);
      } else {
        previousStackTop.leave(eventMillis);
      }
    } else {
      const millis = isExtendedEvent ? relativeMillis : eventMillis;
      if (!inEvent) {
        stackTop = stackTop.enterChild(eventName, millis);
        stack.push(stackTop);
        if (isExtendedEvent) {
          extendedTimeNodes.add(stackTop);
        }
      }
      if (!isBeginEvent) {
        // Create sub event
        const subNode = stackTop.enterChild(`${eventName}.${type}`, millis);
        if (isExtendedEvent) {
          extendedTimeNodes.add(subNode);
        }
        stack.push(subNode);
      }
    }
  }

  if (stack.length !== 1) {
    Console.warn(`Not all nodes are left, the last node is ${stack[stack.length - 1].getName()}`);
    return;
  }

  const totals = new Map<string, Node>();
  rootNode.sumUpTotals(totals);

  const totalList = Array.from(totals.values());
  totalList.sort((o1, o2) => Math.trunc(o2.getTimeSpent() - o1.getTimeSpent()));

  if (consumer !== null) {
    consumer.addProfilerData(stack[0], totalList);
  }
}

/**
 * Outputs the time passed since various events recorded in performance.timing
 * if supported by the browser.
 */
export function logBootstrapTimings(): void {
  if (isEnabled()) {
    const now = Date.now();

    const keys = [
      'navigationStart',
      'unloadEventStart',
      'unloadEventEnd',
      'redirectStart',
      'redirectEnd',
      'fetchStart',
      'domainLookupStart',
      'domainLookupEnd',
      'connectStart',
      'connectEnd',
      'requestStart',
      'responseStart',
      'responseEnd',
      'domLoading',
      'domInteractive',
      'domContentLoadedEventStart',
      'domContentLoadedEventEnd',
      'domComplete',
      'loadEventStart',
      'loadEventEnd'
    ];

    const timings = new Map<string, number>();

    for (const key of keys) {
      const value = getPerformanceTiming(key);
      if (value === 0) {
        // Ignore missing value
        continue;
      }
      timings.set(key, now - value);
    }

    if (timings.size === 0) {
      Console.log('Bootstrap timings not supported, please ensure your browser supports performance.timing');
      return;
    }

    if (consumer !== null) {
      consumer.addBootstrapData(timings);
    }
  }
}

/**
 * Returns a string containing the number of milliseconds which have elapsed
 * since the given reference time.
 *
 * @param reference the reference time, as returned by {@link getRelativeTimeMillis}
 * @return a string containing the number of ms elapsed since the reference time
 */
export function getRelativeTimeString(reference: number): string {
  return `${round(getRelativeTimeMillis() - reference, 3)}`;
}

/**
 * Sets the profiler result consumer that is used to output the profiler data to
 * the user.
 * <p>
 * <b>Warning!</b> This is internal API and should not be used by applications
 * or add-ons.
 *
 * @param profilerResultConsumer the consumer that gets profiler data
 */
export function setProfilerResultConsumer(profilerResultConsumer: ProfilerResultConsumer): void {
  if (consumer !== null) {
    throw new Error('The consumer has already been set');
  }
  consumer = profilerResultConsumer;
}

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
import { Reactive } from '../reactive/Reactive';
import { ReactiveEventRouter } from '../reactive/ReactiveEventRouter';
import { MapPropertyChangeEvent } from './MapPropertyChangeEvent';

type NodeMapLike = any;

type AnyListener = any;

const NO_OP = (): void => {};

/**
 * A property in a node map. Migrated from
 * `com.vaadin.client.flow.nodefeature.MapProperty`. State (value, hasValue,
 * isServerUpdate, previousDomValue, eventRouter) lives here.
 */
export class MapProperty {
  private readonly eventRouter: ReactiveEventRouter;
  private propValue: unknown = undefined;
  private hasValueFlag = false;
  private serverUpdateFlag = false;
  private previousDomValue: unknown = undefined;
  private previousDomValueSet = false;
  private readonly name: string;
  private readonly map: NodeMapLike;
  private readonly forceValueUpdate: boolean;

  constructor(name: string, map: NodeMapLike, forceValueUpdate = false) {
    this.name = name;
    this.map = map;
    this.forceValueUpdate = forceValueUpdate;
    // Mirrors Java MapProperty: wrap turns a ReactiveValueChangeListener into
    // a MapPropertyChangeListener whose `onPropertyChange` fires the
    // value-change handler.
    this.eventRouter = new ReactiveEventRouter(
      this,
      (listener: AnyListener) => ({
        onPropertyChange: (event: unknown): void => listener.onValueChange(event)
      }),
      (listener: AnyListener, event: unknown): void => listener.onPropertyChange(event)
    );
  }

  getName(): string {
    return this.name;
  }

  getMap(): NodeMapLike {
    return this.map;
  }

  isForceValueUpdate(): boolean {
    return this.forceValueUpdate;
  }

  getValue(): unknown {
    this.eventRouter.registerRead();
    return this.propValue;
  }

  hasValue(): boolean {
    this.eventRouter.registerRead();
    return this.hasValueFlag;
  }

  /** Read without dependency registration. */
  hasPropertyValue(): boolean {
    return this.hasValueFlag;
  }

  isServerUpdate(): boolean {
    return this.serverUpdateFlag;
  }

  markServerUpdate(value: boolean): void {
    this.serverUpdateFlag = value;
  }

  setValue(value: unknown): void {
    this.serverUpdateFlag = true;
    this.doSetValue(value);
    Reactive.addPostFlushListener(() => {
      this.serverUpdateFlag = false;
    });
  }

  removeValue(): void {
    if (this.hasValueFlag) {
      this.serverUpdateFlag = true;
      this.updateValue(null, false);
      Reactive.addPostFlushListener(() => {
        this.serverUpdateFlag = false;
      });
    }
  }

  doSetValue(value: unknown): void {
    if (!this.forceValueUpdate && this.hasValueFlag && deepEquals(value, this.propValue)) {
      return;
    }
    this.updateValue(value, true);
  }

  fireMapPropertyChangeEvent(oldValue: unknown, newValue: unknown): void {
    this.eventRouter.fireEvent(new MapPropertyChangeEvent(this, oldValue, newValue));
  }

  private updateValue(value: unknown, hasValue: boolean): void {
    const oldValue = this.propValue;
    this.hasValueFlag = hasValue;
    this.propValue = value;
    this.fireMapPropertyChangeEvent(oldValue, value);
  }

  addChangeListener(listener: AnyListener): { remove(): void } {
    // Accept either a bare `(event) => void` callback (the common Java
    // `MapPropertyChangeListener` lambda) or an object with
    // `onPropertyChange`. Normalize to the latter.
    const adapted = typeof listener === 'function' ? { onPropertyChange: listener } : listener;
    return this.eventRouter.addListener(adapted);
  }

  addReactiveValueChangeListener(listener: AnyListener): { remove(): void } {
    return this.eventRouter.addReactiveListener(listener);
  }

  getValueOrDefaultNumber(defaultValue: number): number {
    if (this.hasValue()) {
      const v = this.getValue();
      if (v == null) return defaultValue;
      return typeof v === 'number' ? Math.trunc(v) : Number(v);
    }
    return defaultValue;
  }

  getValueOrDefaultBoolean(defaultValue: boolean): boolean {
    if (this.hasValue()) {
      const v = this.getValue();
      if (v == null) return defaultValue;
      return v as boolean;
    }
    return defaultValue;
  }

  getValueOrDefaultString(defaultValue: string): string {
    if (this.hasValue()) {
      const v = this.getValue();
      if (v == null) return defaultValue;
      return v as string;
    }
    return defaultValue;
  }

  setPreviousDomValue(previousDomValue: unknown): void {
    this.previousDomValue = previousDomValue;
    this.previousDomValueSet = true;
  }

  hasPreviousDomValue(): boolean {
    return this.previousDomValueSet;
  }

  getPreviousDomValueRaw(): unknown {
    return this.previousDomValue;
  }

  clearPreviousDomValue(): void {
    this.previousDomValue = undefined;
    this.previousDomValueSet = false;
  }

  /** Test/JsOverlay helper: flushes the post-flush listener queue manually. */
  __getEventRouter(): ReactiveEventRouter {
    return this.eventRouter;
  }

  // Java's Objects.equals: null-safe equals. For non-null GWT/JS objects this
  // collapses to reference equality, which === also gives us.
  private static objectsEquals(a: unknown, b: unknown): boolean {
    if (a === b) {
      return true;
    }
    return a == null ? b == null : false;
  }

  /** Sets the value and synchronizes it to the server. */
  syncToServer(newValue: unknown): void {
    this.getSyncToServerCommand(newValue)();
  }

  /** Returns the deferred send command for syncing the new value. */
  getSyncToServerCommand(newValue: unknown): () => void {
    const currentValue = this.hasValue() ? this.getValue() : null;
    if (MapProperty.objectsEquals(newValue, currentValue)) {
      this.markServerUpdate(false);
    }
    if (!(MapProperty.objectsEquals(newValue, currentValue) && this.hasValue()) && !this.isServerUpdate()) {
      const node = (this.map as { getNode(): unknown }).getNode() as {
        getTree(): { isActive(node: unknown): boolean; sendNodePropertySyncToServer(p: MapProperty): void };
      };
      const tree = node.getTree();
      if (tree.isActive(node)) {
        this.doSetValue(newValue);
        return () => tree.sendNodePropertySyncToServer(this);
      } else {
        // Fire a fake event so any DOM listeners reset the property value;
        // flush since we're out of the normal lifecycle.
        this.doSetValue(currentValue);
        (Reactive as { flush(): void }).flush();
      }
    }
    return NO_OP;
  }

  /** Static no-op constant exposed for Debouncer.runCommands(). */
  static readonly NO_OP = NO_OP;
}

function deepEquals(a: any, b: any): boolean {
  if (a === b) return true;
  if (a == null || b == null) return false;
  if (typeof a === 'object' && typeof b === 'object') {
    try {
      return JSON.stringify(a) === JSON.stringify(b);
    } catch {
      return false;
    }
  }
  return false;
}

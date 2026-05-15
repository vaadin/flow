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
 * Snapshot of trigger bindings for one host element, sent by the server.
 * Shape mirrors com.vaadin.flow.component.trigger.internal.TriggerSupport
 * #buildSnapshot.
 */
interface Snapshot {
  triggers: Record<string, { type: string; config: Record<string, unknown> }>;
  actions: Record<string, { type: string; config: Record<string, unknown> }>;
  outputs: Record<string, { type: string; config: Record<string, unknown> }>;
  bindings: Array<{ trigger: number; actions: number[] }>;
}

/**
 * Resolves an output's current value at the moment a trigger fires.
 */
type OutputResolver = (id: number) => unknown;

interface TriggerInstance {
  uninstall(): void;
}

type TriggerFactory = (
  host: HTMLElement,
  config: Record<string, unknown>,
  extraElements: HTMLElement[],
  fire: () => void
) => TriggerInstance;

interface ActionInstance {
  run(resolveOutput: OutputResolver): void;
}

type ActionFactory = (config: Record<string, unknown>, extraElements: HTMLElement[]) => ActionInstance;

interface OutputInstance {
  read(): unknown;
}

type OutputFactory = (config: Record<string, unknown>, extraElements: HTMLElement[]) => OutputInstance;

const triggerFactories = new Map<string, TriggerFactory>();
const actionFactories = new Map<string, ActionFactory>();
const outputFactories = new Map<string, OutputFactory>();

interface Installation {
  triggers: TriggerInstance[];
}

const installations = new WeakMap<HTMLElement, Installation>();

function disposeInstallation(host: HTMLElement): void {
  const existing = installations.get(host);
  if (existing) {
    for (const t of existing.triggers) {
      try {
        t.uninstall();
      } catch (e) {
        console.debug('trigger uninstall failed', e);
      }
    }
    installations.delete(host);
  }
}

function resolveExtraElements(maybeRefs: unknown): HTMLElement[] {
  if (!Array.isArray(maybeRefs)) {
    return [];
  }
  return maybeRefs.filter((e): e is HTMLElement => e instanceof HTMLElement);
}

function bind(host: HTMLElement, snapshot: Snapshot, extraRefs?: unknown): void {
  if (!(host instanceof HTMLElement)) {
    return;
  }
  disposeInstallation(host);

  const extras = resolveExtraElements(extraRefs);

  // Lazily instantiate actions and outputs so a trigger that never fires
  // doesn't pay for its actions.
  const actionCache = new Map<number, ActionInstance>();
  const outputCache = new Map<number, OutputInstance>();

  function getAction(id: number): ActionInstance | null {
    const cached = actionCache.get(id);
    if (cached) {
      return cached;
    }
    const def = snapshot.actions[String(id)];
    if (!def) {
      console.debug(`trigger action id ${id} not found in snapshot`);
      return null;
    }
    const factory = actionFactories.get(def.type);
    if (!factory) {
      console.debug(`no client factory registered for action type ${def.type}`);
      return null;
    }
    const instance = factory(def.config, extras);
    actionCache.set(id, instance);
    return instance;
  }

  function getOutput(id: number): OutputInstance | null {
    const cached = outputCache.get(id);
    if (cached) {
      return cached;
    }
    const def = snapshot.outputs[String(id)];
    if (!def) {
      console.debug(`trigger output id ${id} not found in snapshot`);
      return null;
    }
    const factory = outputFactories.get(def.type);
    if (!factory) {
      console.debug(`no client factory registered for output type ${def.type}`);
      return null;
    }
    const instance = factory(def.config, extras);
    outputCache.set(id, instance);
    return instance;
  }

  const resolveOutput: OutputResolver = (id) => getOutput(id)?.read();

  const installedTriggers: TriggerInstance[] = [];

  for (const [idStr, def] of Object.entries(snapshot.triggers)) {
    const id = Number(idStr);
    const factory = triggerFactories.get(def.type);
    if (!factory) {
      console.debug(`no client factory registered for trigger type ${def.type}`);
      continue;
    }
    const fire = () => {
      for (const binding of snapshot.bindings) {
        if (binding.trigger !== id) {
          continue;
        }
        for (const actionId of binding.actions) {
          const action = getAction(actionId);
          if (!action) {
            continue;
          }
          try {
            action.run(resolveOutput);
          } catch (e) {
            console.debug(`trigger action ${actionId} threw`, e);
          }
        }
      }
    };
    try {
      installedTriggers.push(factory(host, def.config, extras, fire));
    } catch (e) {
      console.debug(`trigger ${id} (${def.type}) install threw`, e);
    }
  }

  installations.set(host, { triggers: installedTriggers });
}

function unbind(host: HTMLElement): void {
  disposeInstallation(host);
}

// --- Built-in factories --------------------------------------------------

triggerFactories.set('flow:click', (host, _config, _extras, fire) => {
  const listener = () => fire();
  host.addEventListener('click', listener);
  return {
    uninstall() {
      host.removeEventListener('click', listener);
    }
  };
});

outputFactories.set('flow:property', (config, extras) => {
  const elementIndex = Number(config.element ?? 0);
  const property = String(config.property ?? '');
  return {
    read() {
      const target = elementIndex === 0 ? null : extras[elementIndex - 1];
      // elementIndex 0 means "host"; not supported for property outputs in
      // v0 (outputs aren't bound to the host element directly).
      if (!target) {
        return undefined;
      }
      return (target as unknown as Record<string, unknown>)[property];
    }
  };
});

actionFactories.set('flow:clipboard-copy', (config) => {
  const textOutputId = Number(config.textOutput);
  return {
    run(resolveOutput) {
      const text = resolveOutput(textOutputId);
      const clipboard = (navigator as Navigator & { clipboard?: Clipboard }).clipboard;
      if (!clipboard || typeof clipboard.writeText !== 'function') {
        console.debug('navigator.clipboard.writeText unavailable');
        return;
      }
      void clipboard.writeText(text == null ? '' : String(text)).catch((e) => {
        console.debug('clipboard.writeText failed', e);
      });
    }
  };
});

// --- Public registry on window.Vaadin.Flow.triggers ----------------------

const $wnd = window as unknown as { Vaadin?: { Flow?: Record<string, unknown> } };
$wnd.Vaadin ??= {};
$wnd.Vaadin.Flow ??= {};
$wnd.Vaadin.Flow.triggers = {
  bind,
  unbind,
  registerTrigger(typeId: string, factory: TriggerFactory): void {
    triggerFactories.set(typeId, factory);
  },
  registerAction(typeId: string, factory: ActionFactory): void {
    actionFactories.set(typeId, factory);
  },
  registerOutput(typeId: string, factory: OutputFactory): void {
    outputFactories.set(typeId, factory);
  }
};

// Ensures this file is emitted as an ES module by TypeScript so Vite
// loads it correctly.
export {};

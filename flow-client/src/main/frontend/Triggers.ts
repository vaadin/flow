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
  arguments: Record<string, { type: string; config: Record<string, unknown> }>;
  bindings: Array<{ trigger: number; actions: number[] }>;
}

/**
 * Resolves an argument's current value at the moment a trigger fires.
 */
type ArgumentResolver = (id: number) => unknown;

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
  run(resolveArgument: ArgumentResolver): void;
}

type ServerNotify = (payload?: unknown) => void;

type ActionFactory = (
  config: Record<string, unknown>,
  extraElements: HTMLElement[],
  notifyServer: ServerNotify
) => ActionInstance;

interface ArgumentInstance {
  read(): unknown;
}

type ArgumentFactory = (config: Record<string, unknown>, extraElements: HTMLElement[]) => ArgumentInstance;

const triggerFactories = new Map<string, TriggerFactory>();
const actionFactories = new Map<string, ActionFactory>();
const argumentFactories = new Map<string, ArgumentFactory>();

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

type MirrorChannel = (actionId: number, payload?: unknown) => void;

function bind(host: HTMLElement, snapshot: Snapshot, extraRefs?: unknown, channel?: MirrorChannel): void {
  if (!(host instanceof HTMLElement)) {
    return;
  }
  disposeInstallation(host);

  const extras = resolveExtraElements(extraRefs);
  const mirror: MirrorChannel = typeof channel === 'function' ? channel : () => undefined;

  // Lazily instantiate actions and arguments so a trigger that never fires
  // doesn't pay for its actions.
  const actionCache = new Map<number, ActionInstance>();
  const argumentCache = new Map<number, ArgumentInstance>();

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
    const notify: ServerNotify = (payload) => mirror(id, payload);
    const instance = factory(def.config, extras, notify);
    actionCache.set(id, instance);
    return instance;
  }

  function getArgument(id: number): ArgumentInstance | null {
    const cached = argumentCache.get(id);
    if (cached) {
      return cached;
    }
    const def = snapshot.arguments[String(id)];
    if (!def) {
      console.debug(`trigger argument id ${id} not found in snapshot`);
      return null;
    }
    const factory = argumentFactories.get(def.type);
    if (!factory) {
      console.debug(`no client factory registered for argument type ${def.type}`);
      return null;
    }
    const instance = factory(def.config, extras);
    argumentCache.set(id, instance);
    return instance;
  }

  const resolveArgument: ArgumentResolver = (id) => getArgument(id)?.read();

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
            action.run(resolveArgument);
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

// Must match TYPE_ID constants in the Java side (DomEventTrigger,
// PropertyArgument, SetPropertyAction).
const TYPE_DOM_EVENT = 'flow:event';
const TYPE_PROPERTY = 'flow:property';
const TYPE_SET_PROPERTY = 'flow:set-property';

triggerFactories.set(TYPE_DOM_EVENT, (host, config, _extras, fire) => {
  const eventName = String(config.eventName ?? '');
  const listener = () => fire();
  host.addEventListener(eventName, listener);
  return {
    uninstall() {
      host.removeEventListener(eventName, listener);
    }
  };
});

argumentFactories.set(TYPE_PROPERTY, (config, extras) => {
  const elementIndex = Number(config.element ?? 0);
  const property = String(config.property ?? '');
  return {
    read() {
      // elementIndex 0 means "host"; not supported for property arguments in
      // v0 (arguments aren't bound to the host element directly).
      const target = elementIndex === 0 ? null : extras[elementIndex - 1];
      if (!target) {
        return undefined;
      }
      return (target as unknown as Record<string, unknown>)[property];
    }
  };
});

actionFactories.set(TYPE_SET_PROPERTY, (config, extras) => {
  const elementIndex = Number(config.element ?? 0);
  const property = String(config.property ?? '');
  const value = config.value;
  return {
    run() {
      const target = elementIndex === 0 ? null : extras[elementIndex - 1];
      if (!target) {
        return;
      }
      (target as unknown as Record<string, unknown>)[property] = value;
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
  registerArgument(typeId: string, factory: ArgumentFactory): void {
    argumentFactories.set(typeId, factory);
  }
};

// Ensures this file is emitted as an ES module by TypeScript so Vite
// loads it correctly.
export {};

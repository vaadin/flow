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

// TypeScript port of com.vaadin.client.flow.binding.Debouncer, built alongside
// the Java version. The GWT elemental Timer is mapped to setTimeout (one-shot,
// schedule) / setInterval (scheduleRepeating).

import { MapProperty } from '../nodefeature/MapProperty';

// com.vaadin.flow.shared.JsonConstants event phases.
const EVENT_PHASE_LEADING = 'leading';
const EVENT_PHASE_INTERMEDIATE = 'intermediate';
const EVENT_PHASE_TRAILING = 'trailing';

type SendCommand = (phase: string) => void;
type Command = () => void;

// Re-usable timer mirroring elemental.util.Timer (schedule = one-shot,
// scheduleRepeating = interval, cancel = stop).
class Timer {
  private handle: ReturnType<typeof setTimeout> | null = null;

  private repeating = false;

  private readonly task: () => void;

  constructor(task: () => void) {
    this.task = task;
  }

  schedule(ms: number): void {
    this.cancel();
    this.repeating = false;
    this.handle = setTimeout(() => {
      this.handle = null;
      this.task();
    }, ms);
  }

  scheduleRepeating(ms: number): void {
    this.cancel();
    this.repeating = true;
    this.handle = setInterval(() => this.task(), ms);
  }

  cancel(): void {
    if (this.handle !== null) {
      if (this.repeating) {
        clearInterval(this.handle);
      } else {
        clearTimeout(this.handle);
      }
      this.handle = null;
    }
  }
}

/**
 * Manages debouncing of events; mirrors Debouncer.java. Use
 * {@link Debouncer.getOrCreate} to create or look up an instance tracking a
 * sequence of similar events.
 */
export class Debouncer {
  private static readonly debouncers = new Map<Node, Map<string, Map<number, Debouncer>>>();

  private readonly timeout: number;

  private readonly element: Node;

  private readonly identifier: string;

  private idleTimer: Timer | null = null;

  private intermediateTimer: Timer | null = null;

  private bufferedSendCommand: SendCommand | null = null;

  private bufferedCommands: Map<string, Command> | null = null;

  private previousBufferedNonExecutedCommands: Map<string, Command> | null = null;

  private potentialTrailing: SendCommand | null = null;

  private potentialTrailingBufferedCommands: Map<string, Command> | null = null;

  private constructor(element: Node, identifier: string, timeout: number) {
    this.element = element;
    this.identifier = identifier;
    this.timeout = timeout;
  }

  /**
   * Informs this debouncer that an event has occurred. Returns <code>true</code>
   * if the event should be processed as-is without delaying.
   */
  trigger(phases: Set<string>, command: SendCommand, commands: Map<string, Command>): boolean {
    // If "leading" events are requested and no timers created yet, this is the
    // leading event, triggered immediately and not saved.
    const triggerImmediately =
      phases.has(EVENT_PHASE_LEADING) && this.idleTimer === null && this.intermediateTimer === null;

    if (!triggerImmediately && (phases.has(EVENT_PHASE_TRAILING) || phases.has(EVENT_PHASE_INTERMEDIATE))) {
      // last command is saved for timers unless this is a "leading" event
      this.bufferedSendCommand = command;
      this.bufferedCommands = commands;
      if (
        !phases.has(EVENT_PHASE_INTERMEDIATE) &&
        (this.idleTimer === null || this.previousBufferedNonExecutedCommands === null)
      ) {
        this.previousBufferedNonExecutedCommands = commands;
      }
      this.potentialTrailing = null;
      this.potentialTrailingBufferedCommands = null;
    }

    if (phases.has(EVENT_PHASE_LEADING) || phases.has(EVENT_PHASE_TRAILING)) {
      if (this.idleTimer === null) {
        this.idleTimer = new Timer(() => {
          if (this.bufferedSendCommand !== null) {
            Debouncer.runCommands(
              EVENT_PHASE_TRAILING,
              this.bufferedSendCommand,
              this.bufferedCommands!,
              this.previousBufferedNonExecutedCommands
            );
            this.bufferedSendCommand = null;
            this.bufferedCommands = null;
            this.previousBufferedNonExecutedCommands = null;
          } else if (this.potentialTrailing !== null) {
            // Both trailing & intermediate configured and e.g. typing stopped:
            // after one more timeout with no new commands, re-post the same
            // event to the server.
            Debouncer.runCommands(
              EVENT_PHASE_TRAILING,
              this.potentialTrailing,
              this.potentialTrailingBufferedCommands!,
              null
            );
          }
          this.unregister(); // release memory
        });
      }
      this.idleTimer.cancel();
      this.idleTimer.schedule(this.timeout);
    }

    if (this.intermediateTimer === null && phases.has(EVENT_PHASE_INTERMEDIATE)) {
      this.intermediateTimer = new Timer(() => {
        if (this.bufferedSendCommand !== null) {
          Debouncer.runCommands(EVENT_PHASE_INTERMEDIATE, this.bufferedSendCommand, this.bufferedCommands!, null);
          if (phases.has(EVENT_PHASE_TRAILING)) {
            this.potentialTrailing = this.bufferedSendCommand;
            this.potentialTrailingBufferedCommands = this.bufferedCommands;
          }
          this.bufferedSendCommand = null;
          this.bufferedCommands = null;
        } else {
          // no new last command during the period, stop and unregister
          this.unregister();
        }
      });
      this.intermediateTimer.scheduleRepeating(this.timeout);
    }

    return triggerImmediately;
  }

  private static runCommands(
    phase: string,
    sendCommand: SendCommand,
    commands: Map<string, Command>,
    previousCommands: Map<string, Command> | null
  ): void {
    if (phase === EVENT_PHASE_TRAILING) {
      commands.forEach((command, property) => {
        if (command === MapProperty.NO_OP && Debouncer.hasPreviousCommand(previousCommands, property)) {
          previousCommands!.get(property)!();
        } else {
          command();
        }
      });
    } else {
      commands.forEach((command) => command());
    }
    sendCommand(phase);
  }

  private static hasPreviousCommand(previousCommands: Map<string, Command> | null, property: string): boolean {
    return previousCommands !== null && previousCommands.has(property);
  }

  private unregister(): void {
    if (this.intermediateTimer !== null) {
      this.intermediateTimer.cancel();
      this.intermediateTimer = null;
    }
    if (this.idleTimer !== null) {
      this.idleTimer.cancel();
      this.idleTimer = null;
    }
    const elementMap = Debouncer.debouncers.get(this.element);
    if (elementMap === undefined) {
      return;
    }
    const identifierMap = elementMap.get(this.identifier);
    if (identifierMap === undefined) {
      return;
    }
    identifierMap.delete(this.timeout);
    if (identifierMap.size === 0) {
      elementMap.delete(this.identifier);
      if (elementMap.size === 0) {
        Debouncer.debouncers.delete(this.element);
      }
    }
  }

  /**
   * Gets an existing debouncer or creates a new one associated with the given
   * DOM node, identifier and debounce timeout.
   */
  static getOrCreate(element: Node, identifier: string, debounce: number): Debouncer {
    let elementMap = Debouncer.debouncers.get(element);
    if (elementMap === undefined) {
      elementMap = new Map();
      Debouncer.debouncers.set(element, elementMap);
    }
    let identifierMap = elementMap.get(identifier);
    if (identifierMap === undefined) {
      identifierMap = new Map();
      elementMap.set(identifier, identifierMap);
    }
    let debouncer = identifierMap.get(debounce);
    if (debouncer === undefined) {
      debouncer = new Debouncer(element, identifier, debounce);
      identifierMap.set(debounce, debouncer);
    }
    return debouncer;
  }

  /**
   * Flushes all pending changes, rescheduling idle timers afterwards. Returns
   * the send commands executed during the flush.
   */
  static flushAll(): SendCommand[] {
    const executedCommands: SendCommand[] = [];
    Debouncer.debouncers.forEach((elementMap) => {
      elementMap.forEach((identifierMap) => {
        identifierMap.forEach((debouncer) => {
          if (debouncer.idleTimer !== null) {
            if (debouncer.bufferedSendCommand !== null) {
              // trailing timer present: treat as an extra trailing event
              Debouncer.runCommands(
                EVENT_PHASE_TRAILING,
                debouncer.bufferedSendCommand,
                debouncer.bufferedCommands!,
                null
              );
            }
            // else: in queue with no command, likely a leading-only subscription
          } else if (debouncer.bufferedSendCommand !== null) {
            // otherwise an extra intermediate event; comes a bit early but
            // better than out of order.
            // Deviation from Debouncer.java: Java runs runCommands here
            // unconditionally and then restarts the intermediate timer. With no
            // buffered command that runCommands NPEs on the null command, so we
            // guard on bufferedSendCommand and skip; the timer restart is moot
            // with nothing to fire.
            Debouncer.runCommands(
              EVENT_PHASE_INTERMEDIATE,
              debouncer.bufferedSendCommand,
              debouncer.bufferedCommands!,
              null
            );
            // restart so we don't fire more than one event quicker than ordered
            debouncer.intermediateTimer!.scheduleRepeating(debouncer.timeout);
          }
          if (debouncer.bufferedSendCommand !== null) {
            executedCommands.push(debouncer.bufferedSendCommand);
            // clean so the idle timer can't fire it again
            debouncer.bufferedSendCommand = null;
            debouncer.bufferedCommands = null;
            debouncer.previousBufferedNonExecutedCommands = null;
          }
        });
      });
    });
    return executedCommands;
  }
}

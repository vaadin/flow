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
import { MapProperty } from '../nodefeature/MapProperty';

// Mirrors com.vaadin.flow.shared.JsonConstants.EVENT_PHASE_* literals.
const EVENT_PHASE_LEADING = 'leading';
const EVENT_PHASE_INTERMEDIATE = 'intermediate';
const EVENT_PHASE_TRAILING = 'trailing';

type JsRunnable = () => void;
type SendCommand = (phase: string | null) => void;
type CommandsMap = { forEach(cb: (command: JsRunnable, property: string) => void): void };

// Module-level registry — Node -> identifier -> timeout -> Debouncer.
const debouncers = new Map<unknown, Map<string, Map<number, Debouncer>>>();

/**
 * Debounces server-bound event delivery for a given (DOM node, identifier,
 * timeout) tuple. Migrated from
 * `com.vaadin.client.flow.binding.Debouncer`.
 *
 * The leading/trailing/intermediate semantics match the Java original
 * verbatim; see the JsonConstants `EVENT_PHASE_*` constants and
 * `DebouncePhase` Javadoc on the server side.
 */
export class Debouncer {
  private readonly element: unknown;
  private readonly identifier: string;
  private readonly timeout: number;

  private idleTimerId: ReturnType<typeof setTimeout> | null = null;
  private intermediateTimerId: ReturnType<typeof setInterval> | null = null;

  private bufferedSendCommand: SendCommand | null = null;
  private bufferedCommands: CommandsMap | null = null;
  private previousBufferedNonExecutedCommands: CommandsMap | null = null;
  private potentialTrailingWithBothTrailingAndIntermediate: SendCommand | null = null;
  private potentialTrailingWithBothTrailingAndIntermediateBufferedCommands: CommandsMap | null = null;

  private constructor(element: unknown, identifier: string, timeout: number) {
    this.element = element;
    this.identifier = identifier;
    this.timeout = timeout;
  }

  trigger(phases: { has(phase: string): boolean }, command: SendCommand, commands: CommandsMap): boolean {
    const triggerImmediately =
      phases.has(EVENT_PHASE_LEADING) && this.idleTimerId === null && this.intermediateTimerId === null;

    if (!triggerImmediately && (phases.has(EVENT_PHASE_TRAILING) || phases.has(EVENT_PHASE_INTERMEDIATE))) {
      this.bufferedSendCommand = command;
      this.bufferedCommands = commands;
      if (
        !phases.has(EVENT_PHASE_INTERMEDIATE) &&
        (this.idleTimerId === null || this.previousBufferedNonExecutedCommands === null)
      ) {
        this.previousBufferedNonExecutedCommands = commands;
      }
      this.potentialTrailingWithBothTrailingAndIntermediate = null;
      this.potentialTrailingWithBothTrailingAndIntermediateBufferedCommands = null;
    }

    if (phases.has(EVENT_PHASE_LEADING) || phases.has(EVENT_PHASE_TRAILING)) {
      if (this.idleTimerId !== null) {
        clearTimeout(this.idleTimerId);
      }
      this.idleTimerId = setTimeout(() => {
        if (this.bufferedSendCommand !== null) {
          runCommands(
            EVENT_PHASE_TRAILING,
            this.bufferedSendCommand,
            this.bufferedCommands!,
            this.previousBufferedNonExecutedCommands
          );
          this.bufferedSendCommand = null;
          this.bufferedCommands = null;
          this.previousBufferedNonExecutedCommands = null;
        } else if (this.potentialTrailingWithBothTrailingAndIntermediate !== null) {
          // Both trailing + intermediate configured and typing has stopped:
          // wait one more timeout, and if still no new commands, re-post the
          // SAME event. Documented in DebouncePhase.
          runCommands(
            EVENT_PHASE_TRAILING,
            this.potentialTrailingWithBothTrailingAndIntermediate,
            this.potentialTrailingWithBothTrailingAndIntermediateBufferedCommands!,
            null
          );
        }
        this.unregister();
      }, this.timeout);
    }

    if (this.intermediateTimerId === null && phases.has(EVENT_PHASE_INTERMEDIATE)) {
      this.intermediateTimerId = setInterval(() => {
        if (this.bufferedSendCommand !== null) {
          runCommands(EVENT_PHASE_INTERMEDIATE, this.bufferedSendCommand, this.bufferedCommands!, null);
          if (phases.has(EVENT_PHASE_TRAILING)) {
            this.potentialTrailingWithBothTrailingAndIntermediate = this.bufferedSendCommand;
            this.potentialTrailingWithBothTrailingAndIntermediateBufferedCommands = this.bufferedCommands;
          }
          this.bufferedSendCommand = null;
          this.bufferedCommands = null;
        } else {
          // No new last command during the period — stop and unregister.
          this.unregister();
        }
      }, this.timeout);
    }

    return triggerImmediately;
  }

  private unregister(): void {
    if (this.intermediateTimerId !== null) {
      clearInterval(this.intermediateTimerId);
      this.intermediateTimerId = null;
    }
    if (this.idleTimerId !== null) {
      clearTimeout(this.idleTimerId);
      this.idleTimerId = null;
    }
    const elementMap = debouncers.get(this.element);
    if (!elementMap) {
      return;
    }
    const identifierMap = elementMap.get(this.identifier);
    if (!identifierMap) {
      return;
    }
    identifierMap.delete(this.timeout);
    if (identifierMap.size === 0) {
      elementMap.delete(this.identifier);
      if (elementMap.size === 0) {
        debouncers.delete(this.element);
      }
    }
  }

  static getOrCreate(element: unknown, identifier: string, debounce: number): Debouncer {
    let elementMap = debouncers.get(element);
    if (!elementMap) {
      elementMap = new Map();
      debouncers.set(element, elementMap);
    }
    let identifierMap = elementMap.get(identifier);
    if (!identifierMap) {
      identifierMap = new Map();
      elementMap.set(identifier, identifierMap);
    }
    let debouncer = identifierMap.get(debounce);
    if (!debouncer) {
      debouncer = new Debouncer(element, identifier, debounce);
      identifierMap.set(debounce, debouncer);
    }
    return debouncer;
  }

  // Returns the list of send commands actually executed during this flush.
  // Callers compare references to decide whether to re-execute their command.
  static flushAll(): SendCommand[] {
    const executedCommands: SendCommand[] = [];
    debouncers.forEach((identifierMap) => {
      identifierMap.forEach((timeoutMap) => {
        timeoutMap.forEach((debouncer) => {
          if (debouncer.idleTimerId !== null) {
            if (debouncer.bufferedSendCommand !== null) {
              // Trailing timer is armed — treat as an extra trailing event.
              runCommands(EVENT_PHASE_TRAILING, debouncer.bufferedSendCommand, debouncer.bufferedCommands!, null);
            }
            // else: leading-only subscription with no pending command — skip.
          } else {
            // No idle timer — must be an extra intermediate event. Triggered
            // a bit early, but more accurate than firing out of order.
            if (debouncer.bufferedSendCommand !== null) {
              runCommands(EVENT_PHASE_INTERMEDIATE, debouncer.bufferedSendCommand, debouncer.bufferedCommands!, null);
            }
            // Restart the intermediate timer so we don't fire two events
            // quicker than the original schedule allowed.
            if (debouncer.intermediateTimerId !== null) {
              clearInterval(debouncer.intermediateTimerId);
              debouncer.intermediateTimerId = setInterval(() => {
                if (debouncer.bufferedSendCommand !== null) {
                  runCommands(
                    EVENT_PHASE_INTERMEDIATE,
                    debouncer.bufferedSendCommand,
                    debouncer.bufferedCommands!,
                    null
                  );
                } else {
                  debouncer['unregister']();
                }
              }, debouncer.timeout);
            }
          }
          if (debouncer.bufferedSendCommand !== null) {
            executedCommands.push(debouncer.bufferedSendCommand);
            // Clear so the idle timer can't fire it again.
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

function runCommands(
  phase: string,
  sendCommand: SendCommand,
  commands: CommandsMap,
  previousCommands: CommandsMap | null
): void {
  if (phase === EVENT_PHASE_TRAILING) {
    commands.forEach((command, property) => {
      if (
        command === (MapProperty as unknown as { NO_OP: JsRunnable }).NO_OP &&
        hasPreviousCommand(previousCommands, property)
      ) {
        const previous = lookupPreviousCommand(previousCommands!, property);
        if (previous !== null) {
          previous();
        }
      } else {
        command();
      }
    });
  } else {
    commands.forEach((command) => command());
  }
  sendCommand(phase);
}

function hasPreviousCommand(previousCommands: CommandsMap | null, property: string | null): boolean {
  if (previousCommands === null || property === null) {
    return false;
  }
  let found = false;
  previousCommands.forEach((_command, key) => {
    if (key === property) {
      found = true;
    }
  });
  return found;
}

function lookupPreviousCommand(previousCommands: CommandsMap, property: string): JsRunnable | null {
  let result: JsRunnable | null = null;
  previousCommands.forEach((command, key) => {
    if (key === property) {
      result = command;
    }
  });
  return result;
}

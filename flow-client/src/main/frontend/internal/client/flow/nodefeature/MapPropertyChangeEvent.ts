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
import { ReactiveValueChangeEvent } from '../reactive/ReactiveValueChangeEvent';

/** Event fired when the value of a map property changes. */
export class MapPropertyChangeEvent extends ReactiveValueChangeEvent {
  readonly oldValue: unknown;
  readonly newValue: unknown;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  constructor(property: any, oldValue: unknown, newValue: unknown) {
    super(property);
    this.oldValue = oldValue;
    this.newValue = newValue;
  }
  getOldValue(): unknown {
    return this.oldValue;
  }
  getNewValue(): unknown {
    return this.newValue;
  }
}

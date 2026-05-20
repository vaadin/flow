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

/** Event fired when the structure of a {@code NodeList} changes. */
export class ListSpliceEvent extends ReactiveValueChangeEvent {
  readonly index: number;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  readonly remove: any;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  readonly add: any;
  readonly clear: boolean;

  /* eslint-disable @typescript-eslint/no-explicit-any, @typescript-eslint/max-params */
  constructor(source: any, index: number, remove: any, add: any, clear: boolean) {
    /* eslint-enable @typescript-eslint/no-explicit-any, @typescript-eslint/max-params */
    super(source);
    this.index = index;
    this.remove = remove;
    this.add = add;
    this.clear = clear;
  }

  getIndex(): number {
    return this.index;
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  getRemove(): any {
    return this.remove;
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  getAdd(): any {
    return this.add;
  }

  isClear(): boolean {
    return this.clear;
  }
}

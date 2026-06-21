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

import { publishClient } from './publishClient';
import { getElementById, getElementByName, hasTag } from './ElementUtil';
import { getAbsoluteUrl, isAbsoluteUrl, redirect } from './WidgetUtil';

/**
 * Publishes the TypeScript implementations that the GWT engine calls into, under
 * `window.Vaadin.Flow.internal.*`. Invoked at the start of the engine's `init()`
 * (see `FlowClient.js`), so the implementations are present before any GWT code
 * runs, in every bootstrap path. As classes are migrated from Java to TypeScript
 * (see `MIGRATION_STRATEGY.md`), their implementations are registered here.
 */
export function registerInternals(): void {
  const flow = (((window as any).Vaadin ??= {}).Flow ??= {});
  const internal = (flow.internal ??= {});

  internal.publishClient = publishClient;
  internal.ElementUtil = { hasTag, getElementById, getElementByName };
  internal.WidgetUtil = { redirect, getAbsoluteUrl, isAbsoluteUrl };
}

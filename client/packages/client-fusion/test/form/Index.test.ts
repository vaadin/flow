/*
 * Copyright 2000-2020 Vaadin Ltd.
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
import { assert } from '@open-wc/testing';

describe('form/Index', () => {
  const $wnd = window as any;

  beforeEach(() => {
    delete $wnd.Vaadin;
  });

  it('should add registration', async () => {
    await import('../../src/form');
    assert.isDefined($wnd.Vaadin);
    assert.isArray($wnd.Vaadin.registrations);
    const formRegistrations = $wnd.Vaadin.registrations.filter((r: any) => r.is === '@vaadin/form');
    assert.lengthOf(formRegistrations, 1);
  });
});

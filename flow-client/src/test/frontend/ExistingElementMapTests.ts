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
import { expect } from '@open-wc/testing';

import { ExistingElementMap } from '../../main/frontend/internal/client/ExistingElementMap';

// Ported from src/test/java/com/vaadin/client/ExistingElementMapTest.java.

describe('ExistingElementMap', () => {
  it('add: id and element are returned by getElement and getId', () => {
    const map = new ExistingElementMap();
    const element = document.createElement('div');
    map.add(1, element);

    expect(map.getElement(1)).to.equal(element);
    expect(map.getId(element)).to.equal(1);
  });

  it('remove: id and element are no longer returned by the getters', () => {
    const map = new ExistingElementMap();
    const element = document.createElement('div');
    map.add(1, element);

    map.remove(1);

    expect(map.getElement(1)).to.be.null;
    expect(map.getId(element)).to.be.null;
  });
});

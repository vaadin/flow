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

import { ElementUtil } from '../../main/frontend/internal/client/ElementUtil';

describe('ElementUtil.getElementById', () => {
  it('finds an element via the document API', () => {
    const root = document.createElement('div');
    const child = document.createElement('span');
    child.id = 'target';
    root.appendChild(child);
    document.body.appendChild(root);
    try {
      expect(ElementUtil.getElementById(root, 'target')).to.equal(child);
    } finally {
      root.remove();
    }
  });

  it('uses shadowRoot.getElementById when present', () => {
    const host = document.createElement('div');
    document.body.appendChild(host);
    const root = host.attachShadow({ mode: 'open' });
    const child = document.createElement('p');
    child.id = 'inner';
    root.appendChild(child);
    try {
      expect(ElementUtil.getElementById(host, 'inner')).to.equal(child);
    } finally {
      host.remove();
    }
  });

  it('falls back to a querySelector match for ids with simple characters', () => {
    const container = document.createElement('section');
    const child = document.createElement('span');
    child.setAttribute('id', 'foo');
    container.appendChild(child);
    expect(ElementUtil.getElementById(container, 'foo')).to.equal(child);
  });

  it('walks [id] attributes for ids with reserved characters', () => {
    const container = document.createElement('section');
    const child = document.createElement('span');
    child.setAttribute('id', 'a:b');
    container.appendChild(child);
    expect(ElementUtil.getElementById(container, 'a:b')).to.equal(child);
  });
});

describe('ElementUtil.getElementByName', () => {
  it('finds an element via [name] attribute filter', () => {
    const container = document.createElement('section');
    const child = document.createElement('span');
    child.setAttribute('name', 'foo');
    container.appendChild(child);
    expect(ElementUtil.getElementByName(container, 'foo')).to.equal(child);
  });
});

import { expect } from '@open-wc/testing';
import { getElementById, getElementByName, hasTag } from '../../main/frontend/internal/ElementUtil';

describe('ElementUtil', () => {
  it('hasTag matches the tag name case-insensitively', () => {
    const div = document.createElement('div');
    expect(hasTag(div, 'div')).to.be.true;
    expect(hasTag(div, 'DIV')).to.be.true;
    expect(hasTag(div, 'span')).to.be.false;
    expect(hasTag(document.createTextNode('x'), 'div')).to.be.false;
  });

  it('getElementById finds an element in the light DOM', () => {
    const container = document.createElement('div');
    const child = document.createElement('span');
    child.id = 'foo';
    container.appendChild(child);
    expect(getElementById(container, 'foo')).to.equal(child);
    expect(getElementById(container, 'missing')).to.equal(null);
  });

  it('getElementById finds an element in the shadow root', () => {
    const host = document.createElement('div');
    const root = host.attachShadow({ mode: 'open' });
    const child = document.createElement('span');
    child.id = 'bar';
    root.appendChild(child);
    expect(getElementById(host, 'bar')).to.equal(child);
  });

  it('getElementByName finds an element by its name attribute', () => {
    const container = document.createElement('div');
    const input = document.createElement('input');
    input.setAttribute('name', 'field');
    container.appendChild(input);
    expect(getElementByName(container, 'field')).to.equal(input);
    expect(getElementByName(container, 'nope')).to.equal(null);
  });
});

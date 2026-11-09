import { expect } from '@open-wc/testing';
import { createPropertyPathExpression } from '../../../../../../main/frontend/internal/client/flow/util/PropertyPathExpression';

describe('PropertyPathExpression', () => {
  const event = { detail: { value: 'v', nothing: null }, zero: 0 };
  const element = { $server: { id: 7 } };

  function evaluate(expression: string): unknown {
    const read = createPropertyPathExpression(expression);
    expect(read, expression).to.not.be.undefined;
    return read!(event, element);
  }

  it('reads a path from the event or the element', () => {
    expect(evaluate('event.detail.value')).to.equal('v');
    expect(evaluate('event.zero')).to.equal(0);
    expect(evaluate('element.$server.id')).to.equal(7);
    expect(evaluate('event.detail.missing')).to.be.undefined;
    expect(evaluate('event.detail.nothing')).to.be.null;
  });

  it('returns the event or the element itself', () => {
    expect(evaluate('event')).to.equal(event);
    expect(evaluate('element')).to.equal(element);
  });

  it('reads inherited properties and getters', () => {
    const domEvent = new CustomEvent('custom-event', { detail: 'd' });
    const read = createPropertyPathExpression('event.type.length')!;
    expect(read(domEvent, null)).to.equal('custom-event'.length);
    expect(createPropertyPathExpression('event.detail')!(domEvent, null)).to.equal('d');
  });

  it('throws a TypeError when reading past null or undefined, as the compiled expression does', () => {
    expect(() => evaluate('event.detail.nothing.value')).to.throw(TypeError);
    expect(() => evaluate('event.missing.value')).to.throw(TypeError);
  });

  [
    'event.x * 2',
    'event.x()',
    "event['x']",
    'event.x[0]',
    'event.detail == 2',
    'events.x',
    'window.x',
    'this.x',
    'event.',
    'event..x',
    'event.1x',
    ' event.x',
    'event.x;',
    ''
  ].forEach((expression) => {
    it(`does not take "${expression}" as a path`, () => {
      expect(createPropertyPathExpression(expression)).to.be.undefined;
    });
  });
});

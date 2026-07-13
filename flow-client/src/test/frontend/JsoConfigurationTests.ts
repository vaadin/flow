import { expect } from '@open-wc/testing';
import {
  getAtmosphereVersion,
  getConfigBoolean,
  getConfigError,
  getConfigString,
  getConfigStringArray,
  getConfigValueMap,
  getVaadinVersion
} from '../../main/frontend/internal/JsoConfiguration';

function config(values: Record<string, unknown>) {
  return { getConfig: (name: string) => values[name] };
}

describe('JsoConfiguration', () => {
  it('getConfigString stringifies the value, or null when absent', () => {
    expect(getConfigString(config({ a: 'x' }), 'a')).to.equal('x');
    expect(getConfigString(config({ a: 42 }), 'a')).to.equal('42');
    expect(getConfigString(config({}), 'missing')).to.equal(null);
  });

  it('getConfigBoolean returns the boolean, or false when absent', () => {
    expect(getConfigBoolean(config({ b: true }), 'b')).to.be.true;
    expect(getConfigBoolean(config({ b: false }), 'b')).to.be.false;
    expect(getConfigBoolean(config({}), 'b')).to.be.false;
  });

  it('getConfigValueMap, getConfigStringArray and getConfigError pass the raw value through', () => {
    const map = { k: 1 };
    const arr = ['a', 'b'];
    const err = { caption: 'c' };
    expect(getConfigValueMap(config({ m: map }), 'm')).to.equal(map);
    expect(getConfigStringArray(config({ a: arr }), 'a')).to.equal(arr);
    expect(getConfigError(config({ e: err }), 'e')).to.equal(err);
  });

  it('version getters read from versionInfo, or null when absent', () => {
    const c = config({ versionInfo: { vaadinVersion: '99', atmosphereVersion: '3' } });
    expect(getVaadinVersion(c)).to.equal('99');
    expect(getAtmosphereVersion(c)).to.equal('3');
    expect(getVaadinVersion(config({}))).to.equal(null);
    expect(getAtmosphereVersion(config({}))).to.equal(null);
  });
});

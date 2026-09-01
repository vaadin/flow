import { expect } from '@open-wc/testing';
import {
  getAtmosphereJSVersion,
  getAtmosphereVersion,
  getConfigBoolean,
  getConfigError,
  getConfigInteger,
  getConfigString,
  getConfigStringArray,
  getConfigValueMap,
  getUIDL,
  getVaadinVersion
} from '../../../../../main/frontend/internal/client/bootstrap/JsoConfiguration';

function config(values: Record<string, unknown>) {
  return { getConfig: (name: string) => values[name] };
}

// Beyond the Java suite: com.vaadin.client.bootstrap.JsoConfiguration has no test
// class of its own.
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

  it('getConfigInteger truncates toward zero, and returns null when absent', () => {
    expect(getConfigInteger(config({ i: 7 }), 'i')).to.equal(7);
    // The value the server writes is a JSON number, but Java's Integer.valueOf
    // accepts what the JSNI hands it, so a numeric string reads the same way.
    expect(getConfigInteger(config({ i: '7' }), 'i')).to.equal(7);
    // Integer.valueOf(int) truncates, and toward zero rather than downward.
    expect(getConfigInteger(config({ i: 7.9 }), 'i')).to.equal(7);
    expect(getConfigInteger(config({ i: -7.9 }), 'i')).to.equal(-7);
    expect(getConfigInteger(config({}), 'i')).to.equal(null);
  });

  it('getConfigValueMap, getConfigStringArray and getConfigError pass the raw value through, or null when absent', () => {
    const map = { k: 1 };
    const arr = ['a', 'b'];
    const err = { caption: 'c' };
    expect(getConfigValueMap(config({ m: map }), 'm')).to.equal(map);
    expect(getConfigStringArray(config({ a: arr }), 'a')).to.equal(arr);
    expect(getConfigError(config({ e: err }), 'e')).to.equal(err);
    // Java hands out a null JSO for an absent value, not an undefined one.
    expect(getConfigValueMap(config({}), 'm')).to.equal(null);
    expect(getConfigError(config({}), 'e')).to.equal(null);
  });

  it('version getters read from versionInfo, or null when absent', () => {
    const c = config({ versionInfo: { vaadinVersion: '99', atmosphereVersion: '3' } });
    expect(getVaadinVersion(c)).to.equal('99');
    expect(getAtmosphereVersion(c)).to.equal('3');
    expect(getVaadinVersion(config({}))).to.equal(null);
    expect(getAtmosphereVersion(config({}))).to.equal(null);
  });

  it('getAtmosphereJSVersion reads the loaded push library, or null when it is absent', () => {
    const win = window as unknown as { vaadinPush?: unknown };
    const saved = win.vaadinPush;
    try {
      expect(getAtmosphereJSVersion()).to.equal(null);
      win.vaadinPush = { atmosphere: { version: '3.0.0' } };
      expect(getAtmosphereJSVersion()).to.equal('3.0.0');
    } finally {
      win.vaadinPush = saved;
    }
  });

  it('getUIDL reads the initial UIDL from the configuration', () => {
    const uidl = { syncId: 0 };
    expect(getUIDL(config({ uidl }))).to.equal(uidl);
    expect(getUIDL(config({}))).to.equal(null);
  });
});

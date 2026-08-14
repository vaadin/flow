import { expect } from '@open-wc/testing';
import sinon from 'sinon';
import { Console, isLocalStorageFlagEnabled } from '../../main/frontend/internal/Console';

type Level = 'debug' | 'log' | 'warn' | 'error';
const LEVELS: Level[] = ['debug', 'log', 'warn', 'error'];

describe('Console', () => {
  const KEY = 'vaadin.browserLog';
  let saved: string | null;
  let stubs: Record<Level, sinon.SinonStub>;

  beforeEach(() => {
    saved = window.localStorage.getItem(KEY);
    stubs = {
      debug: sinon.stub(console, 'debug'),
      log: sinon.stub(console, 'log'),
      warn: sinon.stub(console, 'warn'),
      error: sinon.stub(console, 'error')
    };
  });

  afterEach(() => {
    // The production-mode flag is module state shared by all engine logging.
    Console.setProductionMode(false);
    LEVELS.forEach((level) => stubs[level].restore());
    if (saved === null) {
      window.localStorage.removeItem(KEY);
    } else {
      window.localStorage.setItem(KEY, saved);
    }
  });

  describe('isLocalStorageFlagEnabled', () => {
    it('is true only when the flag is exactly "true"', () => {
      window.localStorage.setItem(KEY, 'true');
      expect(isLocalStorageFlagEnabled()).to.be.true;

      window.localStorage.setItem(KEY, 'false');
      expect(isLocalStorageFlagEnabled()).to.be.false;

      window.localStorage.removeItem(KEY);
      expect(isLocalStorageFlagEnabled()).to.be.false;
    });
  });

  describe('production mode suppression', () => {
    it('logs to the browser console when not in production mode', () => {
      window.localStorage.removeItem(KEY);

      LEVELS.forEach((level) => Console[level](`${level} message`));

      LEVELS.forEach((level) => {
        expect(stubs[level].calledOnceWithExactly(`${level} message`), level).to.be.true;
      });
    });

    it('logs nothing in production mode', () => {
      window.localStorage.removeItem(KEY);
      Console.setProductionMode(true);

      LEVELS.forEach((level) => Console[level](`${level} message`));

      LEVELS.forEach((level) => {
        expect(stubs[level].called, level).to.be.false;
      });
    });

    it('logs in production mode when the vaadin.browserLog flag is set', () => {
      window.localStorage.setItem(KEY, 'true');
      Console.setProductionMode(true);

      LEVELS.forEach((level) => Console[level](`${level} message`));

      LEVELS.forEach((level) => {
        expect(stubs[level].calledOnceWithExactly(`${level} message`), level).to.be.true;
      });
    });

    it('starts logging again when production mode is turned off', () => {
      window.localStorage.removeItem(KEY);
      Console.setProductionMode(true);
      Console.warn('suppressed');
      Console.setProductionMode(false);
      Console.warn('logged');

      expect(stubs.warn.calledOnceWithExactly('logged')).to.be.true;
    });
  });
});

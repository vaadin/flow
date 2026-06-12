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
import sinon from 'sinon';

import { Console } from '../../main/frontend/internal/client/Console';

type ConsoleStub = {
  debug: sinon.SinonStub;
  log: sinon.SinonStub;
  warn: sinon.SinonStub;
  error: sinon.SinonStub;
};

const LOCAL_STORAGE_KEY = 'vaadin.browserLog';

describe('Console', () => {
  let consoleStub: ConsoleStub;

  beforeEach(() => {
    consoleStub = {
      debug: sinon.stub(globalThis.console, 'debug'),
      log: sinon.stub(globalThis.console, 'log'),
      warn: sinon.stub(globalThis.console, 'warn'),
      error: sinon.stub(globalThis.console, 'error')
    };
    Console.setProductionMode(false);
    localStorage.removeItem(LOCAL_STORAGE_KEY);
  });

  afterEach(() => {
    consoleStub.debug.restore();
    consoleStub.log.restore();
    consoleStub.warn.restore();
    consoleStub.error.restore();
    Console.setProductionMode(false);
    localStorage.removeItem(LOCAL_STORAGE_KEY);
  });

  describe('when not in production mode', () => {
    it('logs all four levels', () => {
      Console.debug('d');
      Console.log('l');
      Console.warn('w');
      Console.error('e');

      expect(consoleStub.debug.calledOnceWith('d')).to.be.true;
      expect(consoleStub.log.calledOnceWith('l')).to.be.true;
      expect(consoleStub.warn.calledOnceWith('w')).to.be.true;
      expect(consoleStub.error.calledOnceWith('e')).to.be.true;
    });
  });

  describe('when in production mode', () => {
    beforeEach(() => {
      Console.setProductionMode(true);
    });

    it('suppresses log calls by default', () => {
      Console.debug('d');
      Console.log('l');
      Console.warn('w');
      Console.error('e');

      expect(consoleStub.debug.called).to.be.false;
      expect(consoleStub.log.called).to.be.false;
      expect(consoleStub.warn.called).to.be.false;
      expect(consoleStub.error.called).to.be.false;
    });

    it('logs when the vaadin.browserLog localStorage flag is set to "true"', () => {
      localStorage.setItem(LOCAL_STORAGE_KEY, 'true');

      Console.debug('d');
      Console.error('e');

      expect(consoleStub.debug.calledOnceWith('d')).to.be.true;
      expect(consoleStub.error.calledOnceWith('e')).to.be.true;
    });

    it('still suppresses logs when the flag value is not exactly "true"', () => {
      localStorage.setItem(LOCAL_STORAGE_KEY, '1');

      Console.debug('d');

      expect(consoleStub.debug.called).to.be.false;
    });
  });

  describe('reportStacktrace', () => {
    it('logs the exception regardless of production mode', () => {
      Console.setProductionMode(true);
      const err = new Error('boom');

      Console.reportStacktrace(err);

      expect(consoleStub.error.calledOnceWith(err)).to.be.true;
    });
  });
});

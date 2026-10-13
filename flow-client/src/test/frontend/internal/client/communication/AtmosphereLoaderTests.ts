import { expect } from '@open-wc/testing';
import {
  getLoadedAtmosphere,
  loadAtmosphere,
  setAtmosphereModule
} from '../../../../../main/frontend/internal/client/communication/AtmosphereLoader';

const stubUrl = '/src/test/frontend/stubVaadinPush.js';

describe('AtmosphereLoader', () => {
  afterEach(() => {
    setAtmosphereModule(null);
  });

  it('reuses the load in progress instead of loading again', async () => {
    const first = loadAtmosphere(stubUrl);
    const second = loadAtmosphere('/does-not-exist.js');
    expect(await second).to.equal(await first);
    expect(getLoadedAtmosphere()).to.have.property('isStub', true);
  });

  it('forgets a failed load so that the next call tries again', async () => {
    let failure: unknown = null;
    await loadAtmosphere('/does-not-exist.js').catch((error: unknown) => {
      failure = error;
    });
    expect(failure).to.not.equal(null);
    expect(getLoadedAtmosphere()).to.equal(null);

    expect(await loadAtmosphere(stubUrl)).to.have.property('isStub', true);
  });

  it('ignores a module that settles after it has been replaced', async () => {
    let resolveStale: (module: unknown) => void = () => {};
    let rejectReplaced: (error: unknown) => void = () => {};
    setAtmosphereModule(
      new Promise((resolve) => {
        resolveStale = resolve;
      })
    );
    const current = { current: true };
    setAtmosphereModule(Promise.resolve(current));
    await Promise.resolve();
    resolveStale({ stale: true });
    await Promise.resolve();
    expect(getLoadedAtmosphere()).to.equal(current);

    const replaced = new Promise((_resolve, reject) => {
      rejectReplaced = reject;
    });
    replaced.catch(() => {});
    setAtmosphereModule(replaced);
    setAtmosphereModule(Promise.resolve(current));
    rejectReplaced(new Error('stale failure'));
    await Promise.resolve();
    expect(await loadAtmosphere(stubUrl)).to.equal(current);
  });
});

if (intern.environment === 'node') {
  // Fill in global APIs possibly missing in Node
  /* global require, global */

  if (!global.fetch) {
    const {fetch, Request, Response, Headers} = require('node-fetch');
    Object.assign(global, {fetch, Request, Response, Headers});
  }
}

intern.registerPlugin('sinon', async() => {
  const chai = intern.getPlugin('chai');

  let sinon;
  if (intern.environment === 'node') {
    /* global require */
    sinon = require('sinon');
    chai.use(require('sinon-chai'));
  } else {
    /* global window */
    window.chai = chai;
    await intern.loadScript('node_modules/sinon/pkg/sinon.js');
    await intern.loadScript('node_modules/sinon-chai/lib/sinon-chai.js');
    sinon = window.sinon;
    delete window.sinon;
    delete window.chai;
  }
  return {sinon};
});

intern.registerPlugin('fetchMock', async() => {
  if (intern.environment === 'node') {
    /* global require */
    const fetchMock = require('fetch-mock');
    return {fetchMock};
  } else {
    /* global window, fetchMock */
    await intern.loadScript('node_modules/fetch-mock/dist/es5/client-bundle.js');
    const fetchMockLocal = fetchMock;
    fetchMock = undefined;
    return {fetchMock: fetchMockLocal};
  }
});

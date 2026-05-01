/* No // comments: this file is concatenated into an inline <script> that may collapse to one line. */
window.Vaadin.Flow.ready = async function ({ timeout = 30000 } = {}) {
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
  const deadline = Date.now() + timeout;

  const isIdle = () => {
    if (document.readyState !== 'complete') return false;
    if (window.Vaadin.Flow.devServerIsNotLoaded) return false;
    const clients = window.Vaadin.Flow.clients;
    if (!clients) return false;
    const probes = Object.values(clients).filter((c) => typeof c.isActive === 'function');
    if (probes.length === 0) return false;
    return probes.every((c) => !c.isActive());
  };

  while (!isIdle()) {
    if (Date.now() >= deadline) {
      throw new Error('Vaadin.Flow.ready timed out after ' + timeout + 'ms');
    }
    await sleep(50);
  }
};

window.Vaadin.Flow.whenReady = function (callback) {
  window.Vaadin.Flow.ready()
    .catch((e) => console.warn(e.message))
    .then(callback);
};

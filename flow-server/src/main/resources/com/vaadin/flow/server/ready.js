/**
 * Waits until Vaadin is idle, for use by testing tools.
 *
 * Vaadin is idle when the document has finished loading, the dev server (if
 * any) has loaded and no Flow client on the page reports that it is active,
 * i.e. no server round trip or deferred client work is in progress. A page
 * without Flow clients is idle as soon as it has loaded.
 *
 * The check is re-run whenever a client reports that its activity may have
 * changed and when the document ready state changes, with a 500 ms interval
 * as a fallback.
 *
 * The page shown while the dev server is starting sets window.Vaadin.Flow.ready
 * to false instead, so callers should check that it is a function first.
 *
 * @param options optional; options.timeout is the maximum time to wait,
 *     in milliseconds; defaults to 30000
 * @returns a promise that resolves when Vaadin is idle, or rejects with an
 *     Error if it is still busy when the timeout expires
 */
window.Vaadin.Flow.ready = function (options) {
  const timeout = options && options.timeout !== undefined ? options.timeout : 30000;
  const isIdle = () => {
    const flow = window.Vaadin && window.Vaadin.Flow;
    if (document.readyState !== 'complete' || !flow || flow.devServerIsNotLoaded) {
      return false;
    }
    const clients = flow.clients || {};
    return Object.keys(clients).every((id) => typeof clients[id].isActive !== 'function' || !clients[id].isActive());
  };
  return new Promise((resolve, reject) => {
    if (isIdle()) {
      resolve();
      return;
    }
    let safetyNet;
    let deadline;
    const stop = () => {
      window.removeEventListener('vaadin-flow-activity-changed', check);
      document.removeEventListener('readystatechange', check);
      clearInterval(safetyNet);
      clearTimeout(deadline);
    };
    const check = () => {
      if (isIdle()) {
        stop();
        resolve();
      }
    };
    window.addEventListener('vaadin-flow-activity-changed', check);
    document.addEventListener('readystatechange', check);
    safetyNet = setInterval(check, 500);
    deadline = setTimeout(() => {
      stop();
      reject(new Error('Vaadin.Flow.ready timed out after ' + timeout + 'ms'));
    }, timeout);
  });
};

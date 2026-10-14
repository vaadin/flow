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

const style = document.createElement('style');
style.textContent = '@keyframes element-rendered { } button { animation: element-rendered; }';
document.head.appendChild(style);
window.layout = window.layout || {};

function reportResult(component, result) {
  console.log('Result debug: ' + component + ' time: ' + result);
  component.dispatchEvent(new CustomEvent('componentready', { detail: { result: result } }));
}

window.benchmark = {};
window.benchmark.start = 0;

window.benchmark.whenRendered = (component) => {
  return new Promise((resolve) => {
    let readyTimer;
    const listener = () => {
      const endTime = Date.now();
      readyTimer && clearTimeout(readyTimer);
      readyTimer = setTimeout(() => {
        const thisReadyTimer = readyTimer;
        requestIdleCallback(() => {
          if (thisReadyTimer === readyTimer) {
            component.removeEventListener('animationstart', listener);
            resolve({
              component: component,
              endTime: endTime
            });
          }
        });
        // The timeout needs to be large enough so everything gets rendered
        // but small enough so the tests won't take forever. This resolves with
        // the timestamp of the listener's last invocation.
        // Timeout is not added to the resulted timestamp.
      }, 1000);
    };

    component.addEventListener('animationstart', listener);
  });
};

/**
 * Mark benchmark started by resetting start timer.
 */
window.benchmark.start = () => {
  localStorage.setItem('benchmark.start', Date.now());
};

/**
 * Marks the end timestamp when the component is fully rendered and reports the
 * test result if benchmark is started.
 */
window.benchmark.measureRender = (component) => {
  if (!localStorage.getItem('benchmark.start') || Number.isNaN(parseFloat(localStorage.getItem('benchmark.start')))) {
    return;
  }
  window.benchmark.whenRendered(component).then((result) => {
    let start = parseFloat(localStorage.getItem('benchmark.start'));
    reportResult(result.component, result.endTime - start);
    localStorage.removeItem('benchmark.start');
  });
};

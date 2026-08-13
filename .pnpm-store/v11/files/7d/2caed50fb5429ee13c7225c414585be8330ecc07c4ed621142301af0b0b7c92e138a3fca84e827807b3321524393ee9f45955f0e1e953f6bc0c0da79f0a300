var ConnectionState = /* @__PURE__ */ ((ConnectionState2) => {
  ConnectionState2["CONNECTED"] = "connected";
  ConnectionState2["LOADING"] = "loading";
  ConnectionState2["RECONNECTING"] = "reconnecting";
  ConnectionState2["CONNECTION_LOST"] = "connection-lost";
  return ConnectionState2;
})(ConnectionState || {});
class ConnectionStateStore {
  #stateChangeListeners = /* @__PURE__ */ new Set();
  #connectionState;
  #loadingCount = 0;
  constructor(initialState) {
    this.#connectionState = initialState;
    if (navigator.serviceWorker) {
      navigator.serviceWorker.addEventListener("message", this.#serviceWorkerMessageListener);
      void navigator.serviceWorker.ready.then((registration) => {
        registration.active?.postMessage({
          method: "Vaadin.ServiceWorker.isConnectionLost",
          id: "Vaadin.ServiceWorker.isConnectionLost"
        });
      });
    }
  }
  addStateChangeListener(listener) {
    this.#stateChangeListeners.add(listener);
  }
  removeStateChangeListener(listener) {
    this.#stateChangeListeners.delete(listener);
  }
  loadingStarted() {
    this.state = "loading" /* LOADING */;
    this.#loadingCount += 1;
  }
  loadingFinished() {
    this.#decreaseLoadingCount("connected" /* CONNECTED */);
  }
  loadingFailed() {
    this.#decreaseLoadingCount("connection-lost" /* CONNECTION_LOST */);
  }
  #decreaseLoadingCount(finalState) {
    if (this.#loadingCount > 0) {
      this.#loadingCount -= 1;
      if (this.#loadingCount === 0) {
        this.state = finalState;
      }
    }
  }
  get state() {
    return this.#connectionState;
  }
  set state(newState) {
    if (newState !== this.#connectionState) {
      const prevState = this.#connectionState;
      this.#connectionState = newState;
      this.#loadingCount = 0;
      for (const listener of this.#stateChangeListeners) {
        listener(prevState, this.#connectionState);
      }
    }
  }
  get online() {
    return this.#connectionState === "connected" /* CONNECTED */ || this.#connectionState === "loading" /* LOADING */;
  }
  get offline() {
    return !this.online;
  }
  #serviceWorkerMessageListener = (event) => {
    if (typeof event.data === "object" && event.data.id === "Vaadin.ServiceWorker.isConnectionLost") {
      if (event.data.result === true) {
        this.state = "connection-lost" /* CONNECTION_LOST */;
      }
      navigator.serviceWorker.removeEventListener("message", this.#serviceWorkerMessageListener);
    }
  };
}
function isLocalhost(hostname) {
  if (hostname === "localhost") {
    return true;
  }
  if (hostname === "[::1]") {
    return true;
  }
  return /^127\.\d+\.\d+\.\d+$/u.test(hostname);
}
const $wnd = window;
if (!$wnd.Vaadin?.connectionState) {
  let online;
  if (isLocalhost(window.location.hostname)) {
    online = true;
  } else {
    online = navigator.onLine;
  }
  $wnd.Vaadin ??= {};
  $wnd.Vaadin.connectionState = new ConnectionStateStore(
    online ? "connected" /* CONNECTED */ : "connection-lost" /* CONNECTION_LOST */
  );
}
export {
  ConnectionState,
  ConnectionStateStore,
  isLocalhost
};
//# sourceMappingURL=ConnectionState.js.map

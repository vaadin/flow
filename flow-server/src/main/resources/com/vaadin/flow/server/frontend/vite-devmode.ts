// @ts-ignore
if (import.meta.hot) {
  // @ts-ignore
  const hot = import.meta.hot;

  const isLiveReloadDisabled = () => {
    // Checks if live reload is disabled in the debug window
    return sessionStorage.getItem('vaadin.live-reload.active') === 'false';
  };

  const preventViteReload = (payload: any) => {
    // Changing the path prevents Vite from reloading
    payload.path = '/_fake/path.html';
  };

  let pendingNavigationTo: string | undefined = undefined;
  let redirectPending: boolean = false;

  window.addEventListener('vaadin-router-go', (routerEvent: any) => {
    pendingNavigationTo = routerEvent.detail.pathname + routerEvent.detail.search;
  });

  // Listen for server-initiated redirects via Page.setLocation()
  window.addEventListener('vaadin-redirect-pending', () => {
    redirectPending = true;
  });

  // Register a close event listener and store a Promise on the WebSocket.
  // The Promise resolves with the close code when our listener runs.
  // This allows vite:ws:disconnect to await the close code even though
  // Vite's close listener runs before ours (due to registration order).
  hot.on('vite:ws:connect', (payload: any) => {
    const ws = payload.webSocket;

    // Create Promise with resolver scoped to this closure.
    // Store on WebSocket so vite:ws:disconnect can access it.
    (ws as any)._closeCodePromise = new Promise<number>((resolve) => {
      ws.addEventListener('close', (event: any) => {
        resolve(event.code);
      });
    });
  });

  // Async handler that waits for the close listener to run.
  // Vite's close handler calls notifyListeners which awaits this handler.
  // By awaiting the closeCodePromise, we ensure our close listener has
  // run and we can check the close code before Vite checks willUnload.
  hot.on('vite:ws:disconnect', async (payload: any) => {
    const ws = payload.webSocket;
    const closeCodePromise = (ws as any)?._closeCodePromise;

    if (closeCodePromise) {
      const closeCode = await closeCodePromise;

      // Close code 1008 (VIOLATED_POLICY) indicates authenticated HTTP session invalidation
      // that usually corresponds also to a server-initiated redirect.
      if (closeCode === 1008) {
        redirectPending = true;
        // Dispatch beforeunload to set Vite's internal willUnload flag,
        // which prevents Vite from reloading the page after reconnecting.
        window.dispatchEvent(new Event('beforeunload'));
      }
    }
  });

  hot.on('vite:beforeFullReload', (payload: any) => {
    if (isLiveReloadDisabled()) {
      preventViteReload(payload);
    }
    // Prevent reload when a server-initiated redirect is pending
    if (redirectPending) {
      preventViteReload(payload);
      return;
    }
    if (pendingNavigationTo) {
      // Force reload with the new URL
      location.href = pendingNavigationTo;
      preventViteReload(payload);
    }
  });

  hot.on('sw-js-update', async () => {
    if (isLiveReloadDisabled()) {
      return;
    }

    if (!('serviceWorker' in navigator)) {
      return;
    }

    const registration = await navigator.serviceWorker.getRegistration();
    if (registration) {
      await registration.update();
    }
  });
}

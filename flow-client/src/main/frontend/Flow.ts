import { ConnectionIndicator } from './ConnectionIndicator';
import { ConnectionState, ConnectionStateChangeListener, ConnectionStateStore } from './ConnectionState';

export interface FlowConfig {
  imports?: () => void;
}

interface AppConfig {
  productionMode: boolean;
  appId: string;
  uidl: object;
  clientRouting: boolean;
}

interface AppInitResponse {
  appConfig: AppConfig;
  pushScript?: string;
}

interface Router {
  render: (ctx: NavigationParameters, shouldUpdateHistory: boolean) => Promise<void>;
}

interface HTMLRouterContainer extends HTMLElement {
  onBeforeEnter?: (ctx: NavigationParameters, cmd: PreventAndRedirectCommands, router: Router) => void | Promise<any>;
  onBeforeLeave?: (ctx: NavigationParameters, cmd: PreventCommands, router: Router) => void | Promise<any>;
  serverConnected?: (cancel: boolean, url?: NavigationParameters) => void;
}

interface FlowRoute {
  action: (params: NavigationParameters) => Promise<HTMLRouterContainer>;
  path: string;
}

interface FlowRoot {
  $: any;
  $server: any;
}

export interface NavigationParameters {
  pathname: string;
  search: string;
}

export interface PreventCommands {
  prevent: () => any;
}

export interface PreventAndRedirectCommands extends PreventCommands {
  redirect: (route: string) => any;
}

// flow uses body for keeping references
const flowRoot: FlowRoot = window.document.body as any;
const $wnd = (window as any) as {
  Vaadin: {
    Flow: any;
    TypeScript: any;
    connectionState: ConnectionStateStore;
  };
} & EventTarget;

/**
 * Client API for flow UI operations.
 */
export class Flow {
  config: FlowConfig;
  response?: AppInitResponse = undefined;
  pathname = '';

  // @ts-ignore
  container: HTMLRouterContainer;

  // flag used to inform Testbench whether a server route is in progress
  private isActive = false;

  private baseRegex = /^\//;
  private appShellTitle: string;

  constructor(config?: FlowConfig) {
    flowRoot.$ = flowRoot.$ || [];
    this.config = config || {};

    // TB checks for the existence of window.Vaadin.Flow in order
    // to consider that TB needs to wait for `initFlow()`.
    $wnd.Vaadin = $wnd.Vaadin || {};
    $wnd.Vaadin.Flow = $wnd.Vaadin.Flow || {};
    $wnd.Vaadin.Flow.clients = {
      TypeScript: {
        isActive: () => this.isActive
      }
    };

    // Regular expression used to remove the app-context
    const elm = document.head.querySelector('base');
    this.baseRegex = new RegExp(
      '^' +
        // IE11 does not support document.baseURI
        (document.baseURI || (elm && elm.href) || '/').replace(/^https?:\/\/[^/]+/i, '')
    );
    this.appShellTitle = document.title;
    // Put a vaadin-connection-indicator in the dom
    this.addConnectionIndicator();
  }

  /**
   * Return a `route` object for vaadin-router in an one-element array.
   *
   * The `FlowRoute` object `path` property handles any route,
   * and the `action` returns the flow container without updating the content,
   * delaying the actual Flow server call to the `onBeforeEnter` phase.
   *
   * This is a specific API for its use with `vaadin-router`.
   */
  get serverSideRoutes(): [FlowRoute] {
    return [
      {
        path: '(.*)',
        action: this.action
      }
    ];
  }

  loadingStarted() {
    // Make Testbench know that server request is in progress
    this.isActive = true;
    $wnd.Vaadin.connectionState.loadingStarted();
  }

  loadingFinished() {
    // Make Testbench know that server request has finished
    this.isActive = false;
    $wnd.Vaadin.connectionState.loadingFinished();
  }

  private get action(): (params: NavigationParameters) => Promise<HTMLRouterContainer> {
    // Return a function which is bound to the flow instance, thus we can use
    // the syntax `...serverSideRoutes` in vaadin-router.
    // @ts-ignore
    return async (params: NavigationParameters) => {
      // Store last action pathname so as we can check it in events
      this.pathname = params.pathname;

      if ($wnd.Vaadin.connectionState.online) {
        // @ts-ignore
        try {
          await this.flowInit();
        } catch (error) {
          if (error instanceof FlowUiInitializationError) {
            // error initializing Flow: assume connection lost
            $wnd.Vaadin.connectionState.state = ConnectionState.CONNECTION_LOST;
            return this.offlineStubAction();
          } else {
            throw error;
          }
        }
      } else {
        // insert an offline stub
        return this.offlineStubAction();
      }

      // When an action happens, navigation will be resolved `onBeforeEnter`
      this.container.onBeforeEnter = (ctx, cmd) => this.flowNavigate(ctx, cmd);
      // For covering the 'server -> client' use case
      this.container.onBeforeLeave = (ctx, cmd) => this.flowLeave(ctx, cmd);
      return this.container;
    };
  }

  // Send a remote call to `JavaScriptBootstrapUI` to check
  // whether navigation has to be cancelled.
  private async flowLeave(
    // @ts-ignore
    ctx: NavigationParameters,
    cmd?: PreventCommands
  ): Promise<any> {
    // server -> server, viewing offline stub, or browser is offline
    const connectionState = $wnd.Vaadin.connectionState;
    if (this.pathname === ctx.pathname || !this.isFlowClientLoaded() || connectionState.offline) {
      return Promise.resolve({});
    }
    // 'server -> client'
    return new Promise((resolve) => {
      this.loadingStarted();
      // The callback to run from server side to cancel navigation
      this.container.serverConnected = (cancel) => {
        resolve(cmd && cancel ? cmd.prevent() : {});
        this.loadingFinished();
      };

      // Call server side to check whether we can leave the view
      flowRoot.$server.leaveNavigation(this.getFlowRoute(ctx));
    });
  }

  // Send the remote call to `JavaScriptBootstrapUI` to render the flow
  // route specified by the context
  private async flowNavigate(ctx: NavigationParameters, cmd?: PreventAndRedirectCommands): Promise<HTMLElement> {
    if (this.response) {
      return new Promise((resolve) => {
        this.loadingStarted();
        // The callback to run from server side once the view is ready
        this.container.serverConnected = (cancel, redirectContext?: NavigationParameters) => {
          if (cmd && cancel) {
            resolve(cmd.prevent());
          } else if (cmd && cmd.redirect && redirectContext) {
            resolve(cmd.redirect(redirectContext.pathname));
          } else {
            this.container.style.display = '';
            resolve(this.container);
          }
          this.loadingFinished();
        };

        // Call server side to navigate to the given route
        flowRoot.$server.connectClient(
          this.container.localName,
          this.container.id,
          this.getFlowRoute(ctx),
          this.appShellTitle
        );
      });
    } else {
      // No server response => offline or erroneous connection
      return Promise.resolve(this.container);
    }
  }

  private getFlowRoute(context: NavigationParameters | Location): string {
    return (context.pathname + (context.search || '')).replace(this.baseRegex, '');
  }

  // import flow client modules and initialize UI in server side.
  private async flowInit(serverSideRouting = false): Promise<AppInitResponse> {
    // Do not start flow twice
    if (!this.isFlowClientLoaded()) {
      // show flow progress indicator
      this.loadingStarted();

      // Initialize server side UI
      this.response = await this.flowInitUi(serverSideRouting);

      // Enable or disable server side routing
      this.response.appConfig.clientRouting = !serverSideRouting;

      const { pushScript, appConfig } = this.response;

      if (typeof pushScript === 'string') {
        await this.loadScript(pushScript);
      }
      const { appId } = appConfig;

      // Load bootstrap script with server side parameters
      const bootstrapMod = await import('./FlowBootstrap');
      await bootstrapMod.init(this.response);

      // Load custom modules defined by user
      if (typeof this.config.imports === 'function') {
        this.injectAppIdScript(appId);
        await this.config.imports();
      }

      // Load flow-client module
      const clientMod = await import('./FlowClient');
      await this.flowInitClient(clientMod);

      if (!serverSideRouting) {
        // we use a custom tag for the flow app container
        const tag = `flow-container-${appId.toLowerCase()}`;
        this.container = flowRoot.$[appId] = document.createElement(tag);
        this.container.id = appId;

        // It might be that components created from server expect that their content has been rendered.
        // Appending eagerly the container we avoid these kind of errors.
        // Note that the client router will move this container to the outlet if the navigation succeed
        this.container.style.display = 'none';
        document.body.appendChild(this.container);
      }

      // hide flow progress indicator
      this.loadingFinished();
    }
    return this.response!;
  }

  private async loadScript(url: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.onload = () => resolve();
      script.onerror = reject;
      script.src = url;
      document.body.appendChild(script);
    });
  }

  private injectAppIdScript(appId: string) {
    const appIdWithoutHashCode = appId.substring(0, appId.lastIndexOf('-'));
    const scriptAppId = document.createElement('script');
    scriptAppId.type = 'module';
    scriptAppId.setAttribute('data-app-id', appIdWithoutHashCode);
    document.body.append(scriptAppId);
  }

  // After the flow-client javascript module has been loaded, this initializes flow UI
  // in the browser.
  private async flowInitClient(clientMod: any): Promise<void> {
    clientMod.init();
    // client init is async, we need to loop until initialized
    return new Promise((resolve) => {
      const intervalId = setInterval(() => {
        // client `isActive() == true` while initializing or processing
        const initializing = Object.keys($wnd.Vaadin.Flow.clients)
          .filter((key) => key !== 'TypeScript')
          .reduce((prev, id) => prev || $wnd.Vaadin.Flow.clients[id].isActive(), false);
        if (!initializing) {
          clearInterval(intervalId);
          resolve();
        }
      }, 5);
    });
  }

  // Returns the `appConfig` object
  private async flowInitUi(serverSideRouting: boolean): Promise<AppInitResponse> {
    // appConfig was sent in the index.html request
    const initial = $wnd.Vaadin && $wnd.Vaadin.TypeScript && $wnd.Vaadin.TypeScript.initial;
    if (initial) {
      $wnd.Vaadin.TypeScript.initial = undefined;
      return Promise.resolve(initial);
    }

    // send a request to the `JavaScriptBootstrapHandler`
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      const httpRequest = xhr as any;
      const currentPath = location.pathname || '/';
      const requestPath =
        `${currentPath}?v-r=init` + (serverSideRouting ? `&location=${encodeURI(this.getFlowRoute(location))}` : '');

      httpRequest.open('GET', requestPath);

      httpRequest.onerror = () =>
        reject(
          new FlowUiInitializationError(
            `Invalid server response when initializing Flow UI.
        ${httpRequest.status}
        ${httpRequest.responseText}`
          )
        );

      httpRequest.onload = () => {
        const contentType = httpRequest.getResponseHeader('content-type');
        if (contentType && contentType.indexOf('application/json') !== -1) {
          resolve(JSON.parse(httpRequest.responseText));
        } else {
          httpRequest.onerror();
        }
      };
      httpRequest.send();
    });
  }

  // Create shared connection state store and connection indicator
  private addConnectionIndicator() {
    // add connection indicator to DOM
    ConnectionIndicator.create();

    // Listen to browser online/offline events and update the loading indicator accordingly.
    // Note: if flow-client is loaded, it instead handles the state transitions.
    $wnd.addEventListener('online', () => {
      if (!this.isFlowClientLoaded()) {
        // Send an HTTP HEAD request for sw.js to verify server reachability.
        // We do not expect sw.js to be cached, so the request goes to the
        // server rather than being served from local cache.
        // Require network-level failure to revert the state to CONNECTION_LOST
        // (HTTP error code is ok since it still verifies server's presence).
        $wnd.Vaadin.connectionState.state = ConnectionState.RECONNECTING;
        const http = new XMLHttpRequest();
        const serverRoot = location.pathname || '/';
        http.open('HEAD', serverRoot + (serverRoot.endsWith('/') ? '' : ' /') + 'sw.js');
        http.onload = () => {
          $wnd.Vaadin.connectionState.state = ConnectionState.CONNECTED;
        };
        http.onerror = () => {
          $wnd.Vaadin.connectionState.state = ConnectionState.CONNECTION_LOST;
        };
        http.send();
      }
    });
    $wnd.addEventListener('offline', () => {
      if (!this.isFlowClientLoaded()) {
        $wnd.Vaadin.connectionState.state = ConnectionState.CONNECTION_LOST;
      }
    });
  }

  private async offlineStubAction() {
    const offlineStub = document.createElement('iframe') as HTMLRouterContainer;
    const offlineStubPath = './offline-stub.html';
    offlineStub.setAttribute('src', offlineStubPath);
    offlineStub.setAttribute('style', 'width: 100%; height: 100%; border: 0');
    this.response = undefined;

    let onlineListener: ConnectionStateChangeListener | undefined;
    const removeOfflineStubAndOnlineListener = () => {
      if (onlineListener !== undefined) {
        $wnd.Vaadin.connectionState.removeStateChangeListener(onlineListener);
        onlineListener = undefined;
      }
    };

    offlineStub.onBeforeEnter = (ctx, _cmds, router) => {
      onlineListener = () => {
        if ($wnd.Vaadin.connectionState.online) {
          removeOfflineStubAndOnlineListener();
          router.render(ctx, false);
        }
      };
      $wnd.Vaadin.connectionState.addStateChangeListener(onlineListener);
    };
    offlineStub.onBeforeLeave = (_ctx, _cmds, _router) => {
      removeOfflineStubAndOnlineListener();
    };
    return offlineStub;
  }

  private isFlowClientLoaded(): boolean {
    return this.response !== undefined;
  }
}

class FlowUiInitializationError extends Error {
  constructor(message: any) {
    super(message);
  }
}

export interface FlowConfig {
  imports ?: () => void;
}

interface AppConfig {
  productionMode: boolean,
  appId: string,
  uidl: object
}

interface AppInitResponse {
  appConfig: AppConfig;
}

interface HTMLRouterContainer extends HTMLElement {
  onBeforeEnter ?: (ctx: NavigationParameters, cmd: NavigationCommands) => Promise<any>;
}

interface FlowVirtualChild extends HTMLElement {
  serverConnected ?: (cancel: boolean) => void;
}

interface FlowRoute {
  action : (params: NavigationParameters) => Promise<HTMLRouterContainer>;
  path: string;
}

interface FlowRoot {
  $: any ;
  $server: any;
}

export interface NavigationParameters {
  pathname: string;
}

export interface NavigationCommands {
  prevent: () => any;
}

/**
 * Client API for flow UI operations.
 */
export class Flow {
  config: FlowConfig;
  response ?: AppInitResponse;

  // flow uses body for keeping references
  flowRoot : FlowRoot = document.body as any;

  /**
   * Flow virtual child is the element connected with `wrapperElement` in JavaScriptBootstrapUI.
   *
   * NOTE: this element must stay the same throughout navigations so that the server side will be
   * able to reuse elements/layouts without constantly detaching from the old wrapper and attaching to the new wrapper.
   */
  // @ts-ignore
  flowVirtualChild : FlowVirtualChild;

  /**
   * Flow router container is the element which should be returned in `action` when integrating with `vaadin-router`.
   * The container has `onBeforeEnter` method which will ask flow server for the server views and pack them into `flowVirtualChild`.
   *
   * NOTE: This container should be different for each call from `vaadin-router`
   * so that the `vaadin-router` thinks this is a new element and replaces the old content with this.
   */
  private flowRouterContainer? : HTMLRouterContainer;

  constructor(config?: FlowConfig) {
    this.flowRoot.$ = this.flowRoot.$ || {};
    this.config = config || {};
  }

  /**
   * This should initialize flow in full page mode when implementing
   * https://github.com/vaadin/flow/issues/6256
   * @deprecated
   */
  async start(): Promise<AppInitResponse> {
    return this.flowInit();
  }

  /**
   * Go to a route defined in server.
   *
   * This is a generic API for non `vaadin-router` applications.
   */
  get navigate(): (params: NavigationParameters) => Promise<HTMLElement> {
    // Return a function which is bound to the flow instance
    return async (params: NavigationParameters) => {
      await this.flowInit();
      return this.flowNavigate(params);
    }
  }

  /**
   * Return a `route` object for vaadin-router
   *
   * It returns a `FlowRoute` object whose `path` property handles any route,
   * and the `action` returns the flow container without updating the content,
   * delaying the actual Flow server call to the `onBeforeEnter` phase.
   *
   * This is a `vaadin-router` specific API.
   */
  get route(): FlowRoute {
    return {
      path: '(.*)',
      action: this.action
    }
  }

  private get action(): (params: NavigationParameters) => Promise<HTMLRouterContainer> {
    // Return a function which is bound to the flow instance, thus we can use
    // the syntax `flow.route` in vaadin-router.
    // @ts-ignore
    return async (params: NavigationParameters) => {
      const response = await this.flowInit();
      const id = `flow-router-container-${response.appConfig.appId.toLowerCase()}`;
      this.flowRouterContainer = document.createElement(id);
      this.flowRouterContainer.id = id;
      this.flowRouterContainer.onBeforeEnter = (ctx, cmd) => this.onBeforeEnter(ctx, cmd);
      return this.flowRouterContainer;
    }
  }

  private async onBeforeEnter(ctx: NavigationParameters, cmd: NavigationCommands) {
    const result = await this.flowNavigate(ctx, cmd)
    if (this.flowRouterContainer && result instanceof HTMLElement) {
      this.flowRouterContainer.appendChild(result);
    }
    return result;
  }

  // Send the remote call to `JavaScriptBootstrapUI` to render the flow
  // route specified by `routePath`
  private async flowNavigate(ctx: NavigationParameters, cmd?: NavigationCommands): Promise<HTMLElement> {
    return new Promise(resolve => {
      // The callback to run from server side once the view is ready
      this.flowVirtualChild.serverConnected = cancel =>
        resolve(cmd && cancel ? cmd.prevent() : this.flowVirtualChild);

      // Call server side to navigate to the given route
      this.flowRoot.$server.connectClient(this.flowVirtualChild.localName, this.flowVirtualChild.id, ctx.pathname);
    });
  }

  // import flow client modules and initialize UI in server side.
  private async flowInit(): Promise<AppInitResponse> {
    // Do not start flow twice
    if (!this.response) {
      // Initialize server side UI
      this.response = await this.flowInitUi();

      // Load bootstrap script with server side parameters
      const bootstrapMod = await import('./FlowBootstrap');
      await bootstrapMod.init(this.response);

      // Load flow-client module
      const clientMod = await import('./FlowClient');
      await this.flowInitClient(clientMod);

      // Load custom modules defined by user
      if (this.config.imports) {
        await this.config.imports();
      }

      const id: string = this.response.appConfig.appId;
      const tag: string = `flow-virtual-child-${id.toLowerCase()}`;

      // we use a custom tag for the flow app container
      this.flowVirtualChild = this.flowRoot.$[id] = document.createElement(tag);
      this.flowVirtualChild.id = id;
    }
    return this.response;
  }

  // After the flow-client javascript module has been loaded, this initializes flow UI
  // in the browser.
  private async flowInitClient(clientMod: any): Promise<void> {
    clientMod.init();
    // client init is async, we need to loop until initialized
    return new Promise(resolve => {
      const $wnd = window as any;
      const intervalId = setInterval(() => {
        // client `isActive() == true` while initializing
        const initializing = Object.keys($wnd.Vaadin.Flow.clients)
          .reduce((prev, id) => prev || $wnd.Vaadin.Flow.clients[id].isActive(), false);
        if (!initializing) {
          clearInterval(intervalId);
          resolve();
        }
      }, 5);
    });
  }

  // Send the remote call to `JavaScriptBootstrapHandler` in order to init
  // server session and UI, and get the `appConfig` and `uidl`
  private async flowInitUi(): Promise<AppInitResponse> {
    return new Promise((resolve, reject) => {
      const httpRequest = new (window as any).XMLHttpRequest();
      httpRequest.open('GET', 'VAADIN/?v-r=init');
      httpRequest.onload = () => {
        if (httpRequest.getResponseHeader('content-type') === 'application/json') {
          resolve(JSON.parse(httpRequest.responseText));
        } else {
          reject(new Error(
            `Invalid server response when initializing Flow UI.
            ${httpRequest.status}
            ${httpRequest.responseText}`));
        }
      };
      httpRequest.send();
    });
  }

}


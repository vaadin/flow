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

  // @ts-ignore
  container : HTMLRouterContainer;

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
      delete this.container.onBeforeEnter;
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
      await this.flowInit();
      this.container.onBeforeEnter = (ctx, cmd) => this.onBeforeEnter(ctx, cmd);
      return this.container;
    }
  }

  private onBeforeEnter(ctx: NavigationParameters, cmd: NavigationCommands) {
    return this.flowNavigate(ctx, cmd);
  }

  // Send the remote call to `JavaScriptBootstrapUI` to render the flow
  // route specified by `routePath`
  private async flowNavigate(ctx: NavigationParameters, cmd?: NavigationCommands): Promise<HTMLElement> {
    return new Promise(resolve => {
      // The callback to run from server side once the view is ready
      this.container.serverConnected = cancel =>
        resolve(cmd && cancel ? cmd.prevent() : this.container);

      // Call server side to navigate to the given route
      this.flowRoot.$server.connectClient(this.container.localName, this.container.id, ctx.pathname);
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

      const id = this.response.appConfig.appId;
      // we use a custom tag for the flow app container
      const tag = `flow-container-${id.toLowerCase()}`;

      this.container = this.flowRoot.$[id] = document.createElement(tag);
      this.container.id = id;
      window.console.log("Created container for the flow UI with " + tag);
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


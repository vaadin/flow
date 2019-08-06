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

/**
 * Client API for flow UI operations.
 */
export class Flow {
    config ?: FlowConfig;
    response ?: AppInitResponse;

    constructor(config?: FlowConfig) {
        if (config) {
            this.config = config;
        }
    }

    /**
     * Load flow client module and initialize UI in server side.
     */
    async start(): Promise<AppInitResponse> {
        // Do not start flow twice
        if (!this.response) {
            // Initialize server side UI
            this.response = await this.__initFlowUi();

            // Load bootstrap script with server side parameters
            const bootstrapMod = await import('./FlowBootstrap');
            await bootstrapMod.init(this.response);

            // Load flow-client module
            const clientMod = await import('./FlowClient');
            await this.__initFlowClient(clientMod);

            // // Load custom modules defined by user
            if (this.config && this.config.imports) {
                await this.config.imports();
            }
        }
        return this.response;
    }

    /**
     * Go to a route defined in server.
     */
    navigate(): Promise<void> {
        return Promise.resolve();
    }

    async __initFlowClient(clientMod: any): Promise<void> {
        clientMod.init();
        // client init is async, we need to loop until initialized
        return new Promise((resolve) => {
            const $wnd = (window as any);
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

    async __initFlowUi(): Promise<AppInitResponse> {
        return new Promise((resolve, reject) => {
            const httpRequest = new (window as any).XMLHttpRequest();
            httpRequest.open('GET', 'VAADIN/?v-r=init');
            httpRequest.onload = () => {
                if (httpRequest.getResponseHeader('content-type') === 'application/json') {
                    resolve(JSON.parse(httpRequest.responseText));
                } else {
                    reject(httpRequest);
                }
            };
            httpRequest.send();
        });
    }
}

export interface FlowSettings {
    imports ?: () => void;
}

class Flow {

    config ?: FlowSettings;

    constructor(config?: FlowSettings) {
        if (config) {
            this.config = config;
        }
    }

    start(): Promise<void> {
        return Promise.resolve();
    }

    navigate(): Promise<void> {
        return Promise.resolve();
    }
} 

export { Flow };


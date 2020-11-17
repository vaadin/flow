export interface ConnectionIndicatorConfiguration {
    firstDelay?: number;
    secondDelay?: number;
    thirdDelay?: number;
    applyDefaultTheme?: boolean;
}

const $wnd = window as any;

/**
 * Configures window.Vaadin.loadingIndicator. If not available globally, schedules a repeating
 * task until it becomes available.
 *
 * @param conf All defined fields are applied to the connection indicator configuration.
 * @returns The updated configuration of the connection indicator.
 */
export async function setConnectionIndicatorConfiguration(conf: ConnectionIndicatorConfiguration): Promise<ConnectionIndicatorConfiguration> {
    return new Promise((resolve, reject) => {
        const updateConfiguration = () => {
            try {
                validateDelay(conf.firstDelay);
                validateDelay(conf.secondDelay);
                validateDelay(conf.thirdDelay);
            } catch (error) {
                reject(error);
            }
            if ($wnd.Vaadin?.loadingIndicator) {
                for (const [property, value] of Object.entries(conf)) {
                    if (value !== undefined) {
                        $wnd.Vaadin.loadingIndicator[property] = value;
                    }
                }
                return true;
            } else {
                return false;
            }
        };
        let attempts = 0;
        const interval = setInterval(() => {
            if (updateConfiguration()) {
                clearInterval(interval);
                resolve({
                    firstDelay: $wnd.Vaadin.loadingIndicator.firstDelay,
                    secondDelay: $wnd.Vaadin.loadingIndicator.secondDelay,
                    thirdDelay: $wnd.Vaadin.loadingIndicator.thirdDelay,
                    applyDefaultTheme: $wnd.Vaadin.loadingIndicator.applyDefaultTheme
                });
            } else {
                attempts += 1;
                if (attempts >= 10) {
                    throw new ConfigurationError('window.Vaadin.loadingIndicator not defined');
                }
            }
        }, 100);
    });
}

function validateDelay(delay?: number) {
    if (delay !== undefined && delay! < 0) {
        throw new ConfigurationError('delays must be positive');
    }
}


/**
 * An exception that gets thrown for illegal configuration values.
 */
export class ConfigurationError extends Error {
    /**
     * @param message the `message` property value
     */
    constructor(message: string) {
        super(message);
    }
}
export interface ConnectionIndicatorConfiguration {
  onlineText?: string;
  offlineText?: string;
  firstDelay?: number;
  secondDelay?: number;
  thirdDelay?: number;
  applyDefaultTheme?: boolean;
  reconnectModal?: boolean;
  reconnectingText?: string;
}

const $wnd = window as any;

/**
 * Configures window.Vaadin.connectionIndicator. If not available globally, schedules a repeating
 * task until it becomes available.
 *
 * @param conf All defined fields are applied to the connection indicator configuration.
 * @returns The updated configuration of the connection indicator.
 */
export async function setConnectionIndicatorConfiguration(conf: ConnectionIndicatorConfiguration):
  Promise<ConnectionIndicatorConfiguration> {
  return new Promise((resolve, reject) => {
    const updateConfiguration = () => {
      try {
        validateDelay(conf.firstDelay);
        validateDelay(conf.secondDelay);
        validateDelay(conf.thirdDelay);
      } catch (error) {
        reject(error);
      }
      if ($wnd.Vaadin?.connectionIndicator) {
        setValidatedConnectionIndicatorConfiguration(conf);
        return true;
      } else {
        return false;
      }
    };
    let attempts = 0;
    const interval = setInterval(() => {
      if (updateConfiguration()) {
        clearInterval(interval);
        resolve(getConnectionIndicatorConfiguration());
      } else {
        attempts += 1;
        if (attempts >= 10) {
          throw new ConfigurationError('window.Vaadin.connectionIndicator not defined');
        }
      }
    }, 100);
  });
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

function validateDelay(delay?: number) {
  if (delay !== undefined && delay! < 0) {
    throw new ConfigurationError('delays must be positive');
  }
}

/**
 * Concrete values for each ConnectionIndicatorConfiguration to enable reflection over the field names.
 */

/* tslint:disable: max-classes-per-file */
class ConnectionIndicatorConfigurationImpl implements ConnectionIndicatorConfiguration {
  onlineText? = '';
  offlineText? = '';
  firstDelay? = 0;
  secondDelay? = 0;
  thirdDelay? = 0;
  applyDefaultTheme? = false;
  reconnectModal? = false;
  reconnectingText? = '';
}

function setValidatedConnectionIndicatorConfiguration(conf: ConnectionIndicatorConfiguration) {
  for (const property of Object.getOwnPropertyNames(new ConnectionIndicatorConfigurationImpl())) {
    const value = (conf as any)[property];
    if (value !== undefined) {
      $wnd.Vaadin.connectionIndicator[property] = value;
    }
  }
}

function getConnectionIndicatorConfiguration(): ConnectionIndicatorConfiguration {
  const conf = {};
  for (const property of Object.getOwnPropertyNames(new ConnectionIndicatorConfigurationImpl())) {
    (conf as any)[property] = $wnd.Vaadin.connectionIndicator[property];
  }
  return conf;
}

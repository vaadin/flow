/* tslint:disable: max-classes-per-file */

import {addConnectionIndicator} from "./ConnectionIndicator";

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
export function setConnectionIndicatorConfiguration(conf: ConnectionIndicatorConfiguration):
  ConnectionIndicatorConfiguration {
  // ensure the connection indicator is in the DOM and accessible via window.Vaadin.connectionIndicator
  addConnectionIndicator();
  validateDelay(conf.firstDelay);
  validateDelay(conf.secondDelay);
  validateDelay(conf.thirdDelay);
  setValidatedConnectionIndicatorConfiguration(conf);
  return getConnectionIndicatorConfiguration();
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

/* tslint:disable: max-classes-per-file */

import {addConnectionIndicator} from "./ConnectionIndicator";

export interface ConnectionIndicatorConfiguration {
  /**
   * The message shown when the connection goes to connected state.
   */
  onlineText?: string;

  /**
   * The message shown when the connection goes to lost state.
   */
  offlineText?: string;

  /**
   * The delay before showing the loading indicator, in ms.
   */
  firstDelay?: number;

  /**
   * The delay before the loading indicator goes into "second" state, in ms.
   */
  secondDelay?: number;

  /**
   * The delay before the loading indicator goes into "third" state, in ms.
   */
  thirdDelay?: number;

  /**
   * The duration for which the connection state change message is visible,
   * in ms.
   */
  expandedDuration?: number;

  /**
   * Whether to apply the default indicator theme. Set to false if providing custom theme.
   */
  applyDefaultTheme?: boolean;

  /**
   * Whether the connection state message should be modal.
   */
  reconnectModal?: boolean;

  /**
   * The message shown when the connection goes to reconnecting state.
   */
  reconnectingText?: string;
}

const $wnd = window as any;

/**
 * Configures window.Vaadin.connectionIndicator.
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
 * Note: the below values are not the defaults (defaults are defined in ConnectionIndicator.ts).
 */
class ConcreteConnectionIndicatorConfiguration implements ConnectionIndicatorConfiguration {
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
  for (const property of Object.getOwnPropertyNames(new ConcreteConnectionIndicatorConfiguration())) {
    const value = (conf as any)[property];
    if (value !== undefined) {
      $wnd.Vaadin.connectionIndicator[property] = value;
    }
  }
}

function getConnectionIndicatorConfiguration(): ConnectionIndicatorConfiguration {
  const conf = {};
  for (const property of Object.getOwnPropertyNames(new ConcreteConnectionIndicatorConfiguration())) {
    (conf as any)[property] = $wnd.Vaadin.connectionIndicator[property];
  }
  return conf;
}

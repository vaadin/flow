/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
/**
 * This plugin handles communicates to Java dev-mode handler status of compilation
 */
class BuildStatusPlugin {
  constructor(options = {}) {
    this.options = options;
  }

  apply(compiler) {
    const logger = compiler.getInfrastructureLogger('build-status');

    compiler.hooks.done.tap('done', (stats) => {
      // Defer notification and let the webpack-dev-server finish
      setTimeout(() => {
        const errors = stats.compilation.errors;
        const warnings = stats.compilation.warnings;
        if (errors.length > 0) {
          logger.info(
            `${humanReadable(errors.length, 'error')} and ${humanReadable(warnings.length, 'warning')} were reported.`
          );
          logger.info(': Failed to compile.');
        } else {
          if (warnings.length > 0) {
            logger.info(`${humanReadable(warnings.length, 'warning')} were reported.`);
          }
          logger.info(': Compiled.');
        }
      }, 0);
    });
  }
}

function humanReadable(count, label) {
  if (count % 10 == 1) {
    return `${count} ${label}`;
  } else {
    return `${count} ${label}s`;
  }
}

module.exports = BuildStatusPlugin;

const fs = require('node:fs');
const path = require('node:path');
const { findChrome } = require('../scripts/ci/check-tools.cjs');
const { xmlSafeText } = require('../scripts/ci/junit-text.cjs');

module.exports = (config) => {
  process.env.CHROME_BIN = findChrome();
  const results = path.join(__dirname, 'test-results');
  // Remove only this generated report to avoid publishing stale results after a launch failure.
  fs.rmSync(path.join(results, 'angular.xml'), { force: true });
  config.set({
    basePath: '',
    frameworks: ['jasmine'],
    plugins: [require('karma-jasmine'), require('karma-chrome-launcher'), require('karma-coverage'), require('karma-junit-reporter')],
    reporters: ['progress', 'junit'],
    browsers: ['ChromeHeadless'],
    singleRun: true,
    captureTimeout: 60000,
    browserDisconnectTimeout: 10000,
    browserNoActivityTimeout: 60000,
    browserDisconnectTolerance: 0,
    formatError: message => xmlSafeText(message),
    junitReporter: {
      outputDir: results, outputFile: 'angular.xml', useBrowserName: false,
      nameFormatter: (_browser, result) => xmlSafeText(`${result.suite.join(' ')} ${result.description}`),
      classNameFormatter: (_browser, result) => xmlSafeText(result.suite.join(' ')),
    },
    coverageReporter: {
      dir: path.join(__dirname, 'coverage', 'frontend'), subdir: '.',
      reporters: [{ type: 'lcovonly' }, { type: 'html' }, { type: 'text-summary' }],
    },
  });
};

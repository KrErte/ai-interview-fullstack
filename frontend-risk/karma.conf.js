// Karma configuration for `ng test` (Angular 19, @angular-devkit/build-angular:karma).
// `npm test`    -> watch mode in Chrome.
// `npm run test:ci` -> single run in ChromeHeadlessCI (headless, --no-sandbox so it
//                     works as root / in containers), exits non-zero on failure.
// Chrome binary: set CHROME_BIN to override; defaults to /usr/bin/google-chrome if present.
const fs = require('fs');
if (!process.env.CHROME_BIN && fs.existsSync('/usr/bin/google-chrome')) {
  process.env.CHROME_BIN = '/usr/bin/google-chrome';
}

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    client: {
      jasmine: {
        random: true
      },
      clearContext: false
    },
    jasmineHtmlReporter: {
      suppressAll: true
    },
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage/frontend-risk'),
      subdir: '.',
      reporters: [{ type: 'html' }, { type: 'text-summary' }]
    },
    reporters: ['progress', 'kjhtml'],
    browsers: ['Chrome'],
    customLaunchers: {
      ChromeHeadlessCI: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox', '--disable-gpu', '--disable-dev-shm-usage']
      }
    },
    restartOnFileChange: true
  });
};

# vaadin-dev-server

## Development

### Frontend

To install NPM dependencies:
```shell
npm install
```

To format code using Prettier:
```shell
npm run prettier
```

### Using the local dev tool files in a Flow application

In order to work iteratively on the dev tool UI, this project contains a frontend dev server setup that allows to use the local version of the dev tool files within an actual Flow app.
The dev server supports hot module reload (HMR), which allows making changes to the dev tool without requiring to rebuild / restart the Flow app, or having to reload the page in the browser.

First, start the Flow app that you want to develop the dev tool in.
Then patch the app to use the local files using:
```shell
npm run patch-app /path/to/flow-app
```

Then start the frontend dev server:
```shell
npm install
npm start
```

Open the Flow app in the browser and open the dev tool.
Then start making changes to the local dev tools files (e.g. change a CSS style).
Verify the changes are immediately reflected in the dev tool opened in the browser.

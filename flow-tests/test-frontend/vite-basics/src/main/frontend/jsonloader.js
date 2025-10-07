window.loadJson = (resultHandler) => {
  import('./my.json').then((result) => resultHandler(JSON.stringify(result.default)));
};

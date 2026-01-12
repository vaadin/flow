let res;
const promise = new Promise((resolve, reject) => {
  res = resolve;
});

setTimeout(() => {
  window.othervalue = 'This is the value set in other.js';
  res();
}, 500);

await promise;

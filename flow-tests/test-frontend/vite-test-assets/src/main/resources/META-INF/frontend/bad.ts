let hello: any; // Must be initialized

function foo(err: any) {
  // parameter declared but not read
  this.emit('end'); // `this` implicitly has type `any`
}

function func1(bar) {
  // Parameter 'bar' implicitly has an 'any' type.
  const baz = 'a'; // Local declared but never used

  return bar + 'a';
}

(window as any).bad = function () {
  return 'good';
};

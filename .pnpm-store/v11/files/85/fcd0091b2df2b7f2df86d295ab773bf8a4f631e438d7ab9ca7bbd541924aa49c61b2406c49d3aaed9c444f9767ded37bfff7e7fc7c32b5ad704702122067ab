var assert = require('assert');
var indent = require('..');
var string = 'hello\nworld';

describe('indent', function () {
  it('should indent a string by a number of spaces', function(){
    var str = indent(string, 1);
    assert.equal(str, ' hello\n world');
  });

  it('should indent a string by spaces', function(){
    var str = indent(string, '   ');
    assert.equal(str, '   hello\n   world');
  });

  it('should indent a string by a character', function(){
    var str = indent(string, '\t');
    assert.equal(str, '\thello\n\tworld');
  });

  it('should default to 2 spaces', function(){
    var str = indent(string);
    assert.equal(str, '  hello\n  world');
  });

  it('shouldn\'t indent blank lines', function(){
    var str = indent('hello\n\nworld');
    assert.equal(str, '  hello\n\n  world');
  });
});

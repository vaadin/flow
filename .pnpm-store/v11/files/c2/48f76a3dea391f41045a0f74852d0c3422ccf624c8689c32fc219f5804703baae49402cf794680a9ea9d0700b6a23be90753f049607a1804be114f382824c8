"use strict";

module.exports = function (grunt) {

  // Project configuration.
  grunt.initConfig({
    pkg: grunt.file.readJSON("package.json"),
    simplemocha: {
      options: {
        reporter: "spec",
        ui: "bdd"
      },
      all: ["test/**/*.js"]
    },
    eslint: {
      target: [
        "Gruntfile.js", "index.js", "lib/**/*.js",
        "scripts/*.js", "test/**/*.js"
      ]
    },
    watch: {
      all: {
        files: "<%= eslint.target %>",
        tasks: "default"
      }
    },
    notify_hooks: {
      options: {
        enabled: true
      }
    },
    notify: {
      watch: {
        options: {
          message: "✓ Everything passed!"
        }
      }
    },
    release: {
      options: {
        npm: true
      }
    }
  });

  grunt.loadNpmTasks("grunt-simple-mocha");
  grunt.loadNpmTasks("grunt-eslint");
  grunt.loadNpmTasks("grunt-contrib-watch");
  grunt.loadNpmTasks("grunt-notify");
  // Currently grunt-release can't be included on package.json as it
  // causes npm install to fail. We install the module via make setup
  // until the issue is fixed
  grunt.loadNpmTasks("grunt-release");


  //enable our hooks
  grunt.task.run("notify_hooks");

  grunt.registerTask("default", ["eslint", "simplemocha", "notify:watch"]);

};

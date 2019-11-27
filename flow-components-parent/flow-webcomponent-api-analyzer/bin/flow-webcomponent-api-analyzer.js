#!/usr/bin/env node
/*
 * Copyright 2000-2019 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
'use strict';
const gulp = require('gulp');
require('../gulpfile');

require('coa').Cmd()
  .name(process.argv[1])
  .title('flow-webcomponent-api-analyzer')
  .helpful()
  .opt()
    .name('package')
    .title('Bower package(s) to use. Multiple packages can be defined with: package="foo bar" or package=foo,bar')
    .long('package')
    .end()
  .opt()
    .name('Json directory')
    .title('Directory where the Json files are generated. (Default: generated_json/ )')
    .long('targetDir')
    .end()
  .opt()
    .name('Resources directory')
    .title('Directory where the Resource files are generated. (Default: dependencies/bower_components)')
    .long('resourcesDir')
    .end()
  .opt()
    .name('Skip inherited API')
    .title('Skip API that has been inherited from other elements. (Default: false)')
    .long('skipInheritedAPI')
    .end()
  .opt()
    .name('Create Flow Components')
    .title('Create Components to flow-components/json_metadata directory')
    .long('flow_components')
    .end()
  .opt()
    .name('Dependencies file name')
    .title('Sets the name of the dependencies file name, which is parsed for directory names that are used during the analysis. (Default: bower.json)')
    .long('dependenciesFile')
    .end()
  .act(function() {
    gulp.task('default')();
  })
  .end()
  .run(process.argv.slice(2));

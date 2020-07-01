/*
 * Copyright 2000-2020 Vaadin Ltd.
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

/* tslint:disable:max-classes-per-file */

const {suite, test, beforeEach} = intern.getInterface("tdd");
const {assert} = intern.getPlugin("chai");

suite("form/Index", () => {
  const $wnd = (window as any);

  beforeEach(() => {
    delete $wnd.Vaadin;
  });

  test("should add registration", async () => {
    await import("../../../main/resources/META-INF/resources/frontend/form");
    assert.isDefined($wnd.Vaadin);
    assert.isArray($wnd.Vaadin.registrations);
    assert.deepInclude($wnd.Vaadin.registrations, {is: "@vaadin/form"});
  });
});

# Contributing to Vaadin Flow

*There are many ways to participate to the Vaadin Flow project. You can ask questions and participate to discussion in [Discord](https://discord.com/channels/732335336448852018/774366844684468284), [forum](https://vaadin.com/forum), [fill bug reports](https://github.com/vaadin/flow/issues) and enhancement suggestions and contribute code. These instructions are for contributing code to Flow project itself.*

## TL;DR:
- [Always setup your project to follow the coding conventions](#project-setup)
- Always contribute on top of the `master` branch
- Always write a unit test for the code changes or explain why it is impossible. For browser features, write an integration (TestBench) test (too).
- [Write a good commit message](#describe-your-changes)
- Remember: small changes get in faster & solve only one problem per patch
- Never contribute enhancements / features without discussing it in a github issue first.
- You can ask for help or feedback in the github issue, draft PR or in [Discord](https://discord.com/channels/732335336448852018/774366844684468284)
- Threat people with the same respect you would wish them to give to you - it never hurt anyone to be polite.


# Project Setup

Specific details worth mentioning are:
- Line length is 80 characters
- Use spaces instead of tabs for indentation
- Use UTF-8 encoding
- Use unix-style line endings (\n)

Please follow the instructions below to set up your development environment to follow our coding conventions.

## IntelliJ IDEA

Import the project with `File`->`New`->`Project from existing sources...`.

Then setup the workspace settings:

1. Install the Eclipse Code Formatter plugin, then restart IDEA
1. Open Settings (Ctrl + Alt + S or CMD + ,)
1. On the Other Settings / Eclipse Code Formatter page
  1. Check Use the Eclipse code formatter
  1. In the  Supported file types section, check Enable Java
  1. In Eclipse Java Formatter config file, browse to your local copy of [VaadinJavaConventions.xml](https://github.com/vaadin/flow/blob/master/eclipse/VaadinJavaConventions.xml)
  1. Uncheck Optimize Imports
1. Go to Editor / Code Style, and set Right margin (columns) to 80
1. Go to Editor / Code Style / Java and on the Imports tab
  1. Make sure that Use single class import is checked
  1. Set both Class count to use import with ‘*’ and Names count to use static import with ‘*’ to 99
  1. On the Import Layout pane, make sure that Layout static imports separately is checked
  1. Remove all the packages in the list Packages to Use Import with '*'
  1. Organize Java imports to comply to the convention defined below.
1. Go to Editor / Copyright / Copyright Profiles and create a new profile named Vaadin, with the copyright text as defined below
1. Go one level higher, to Editor / Copyright, and set Default project copyright to the Vaadin profile you just created

[Note]  Although IDEA (since release 13) can import Eclipse formatter settings directly, it seems that for the current VaadinJavaConventions.xml, this doesn’t quite work seamlessly. Until this issues with this have been solved, it is not recommended to go down that path.

#### Import order
Java imports should be organized in the following order:
```java
import javax.*
import java.*
<blank line>
import all other imports
<blank line>
import com.google.gwt.*
import com.vaadin.*
<blank line>
import elemental.*
import static all other imports
```

#### Copyright notice
The following copyright should be applied to all source files:
```
Copyright 2000-$today.year Vaadin Ltd.

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```
To make IDEA apply it automatically, go to
`Settings`->`Editor`->`Copyright`->`Copyright Profiles` and add a profile for Vaadin.

## Eclipse

### Import the Project into the Workspace

1. Do *File* -> *Import* -> *General* -> *Existing Maven Project*
1. Select the *flow* folder (where you cloned the project)
1. Ensure all projects are checked
1. Click “finish” to complete the import
1. Disable HTML and XML validation in the workspace to avoid validating Bower dependencies
 1. Eclipse preferences -> *Validation*
 1. Uncheck *Build* for *HTML Syntax Validator*
 1. Uncheck *Build* for *XML Validator*

Note that the first compilation takes a while to finish as Maven downloads dependencies used in the projects.

### Set up workspace to follow coding conventions
The following preferences need to be set to keep the project consistent. You need to do this especially to be able to contribute changes to the project.

1. Open *Window* -> *Preferences* (Windows) or *Eclipse* -> *Preferences* (Mac)
1. On the Java / Code Style / Formatter page, import <flow-root>/eclipse/VaadinJavaConventions.xml
1. On the Java / Code Style / Organize Imports page, import <flow-root>/eclipse/flow.importorder
1. On the Java / Code Style / Code Templates page, edit the Comments / Files template to add [the copyright text below](#copyright-text-for-eclipse)
1. On the Editor / Save Actions tab, make sure that in case Format source code is active, the Format edited lines option is selected. Never use the Format all lines option, as that may introduce loads of unnecessary code changes, making code reviews a nightmare!
1. Go to *General* ->  *Workspace*
 1. Set *Text file encoding* to *UTF-8*
 1. Set *New text file line delimiter* to *Unix*
1. Go to XML -> XML Files -> Editor
 1. Ensure the settings are follows:
<pre><code>Line width: 72
Format comments: true
Join lines: true
Insert whitespace before closing empty end-tags: true
Indent-using spaces: true
Indentation size: 4
</code></pre>

#### Copyright text for Eclipse
```
Copyright 2000-${currentDate:date('yyyy')} Vaadin Ltd.

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```

## VSCode

Introductions for making contributions with VSCode are not done - to be contributed by someone who knows how to set it up to follow the same conventions as with IDEA / Eclipse above.

# Contributing a patch

All contributions should target the `master` branch. We will pick the changes 
to correct version branches from the `master`. **For enhancements and new features, you should always first discuss it in an github issue, to make sure it acceptable. The enhancement should have an Acceptance Criteria written to it that states what is needed to be done.**

We like quality patches that solve problems. A quality patch follows good coding practices - it’s easy to read and understand. For more complicated fixes or features, the change should be broken down into several smaller easy to understand patches. Most of this is really just what we consider to be common sense and best development practices.

In other words: 

 * Describe your changes: what did you change, why did you change it, how did you change it? 
 * Separate your changes: one change per commit, many small changes are easier to review.
 * Include a test to prove your patch works, or a benchmark if it’s a performance improvement.
 * Style-check your changes: it’s okay to have a separate commit to fix style issues.
 * Ensure you have Contributor Agreement signed up. This can be signed digitally in the code review system.
 * Create a pull request; it will then be reviewed by the Framework team, who will provide actionable feedback in a timely fashion if necessary. **Remember to check the "Allow edits from maintainers" so we can rebase the PR or make small changes if necessary**.
 * Respond to review comments: review comments are meant to improve the quality of the code by pointing out defects or readability issues.
 * Don't get discouraged - or impatient: reviewers are people too and will sometimes forget. Give them a friendly poke if you feel the PR has been forgotten about. 
 * Most PRs take a few iterations of review before they are merged. 

### We encourage you to get in touch

Commenting on the issue you want to work on or getting in touch with us on [Discord](https://discord.com/channels/732335336448852018/774366844684468284) before starting. We’re more than happy to help you get started, and we’re happy to engage in conversation about feature suggestions and bug fixes. We welcome contributors and contributions and we’re here to help. But please don't expect us to answer immediately, as we have other work to do too.

Getting in touch with us early will help in getting the fix or implementation right and reduces time in review.

### Describe your changes

Properly formed Git commit subject line should always be able to complete the following sentence:

```If applied, this commit will <your subject line here>```

#### Describe your problem

Whether your patch is a one-line bug fix or 5000 lines of a new feature, there must be an underlying problem that motivated you to do this work. Convince the reviewer that there is a problem worth fixing and that it makes sense for them to read past the first paragraph. This is often already described in bug/enhancement issue, but also summarise it in your commit message.

#### Describe user-visible impact

Straight up crashes and lockups are pretty convincing, but not all bugs are that blatant. Even if the problem was spotted during code review, describe the impact you think it can have on users. 

#### Describe the solution

Once the problem is established, describe what you are actually doing about it in technical detail.  It's important to describe the change in plain English for the reviewer to verify that the code is behaving as you intend it to.

#### Solve only one problem per patch

If your description starts to get long, that's a sign that you probably need to split up your patch. See “Only one logical change per patch”.

Describe your changes in imperative mood, e.g. "make xyzzy do frotz". If the patch fixes a logged bug entry, refer to that bug entry by number or URL. 

However, try to make your explanation understandable without external resources.  In addition to giving a URL to a ticket or bug description, summarise the relevant points of the discussion that led to the patch as submitted.

### Separate your changes

Separate all enhancements, fixes and new features into different pull requests.

For example, if your changes include both bug fixes and performance enhancements, separate those changes into two or more patches. If your changes include an API update, and a new component which uses that new API, separate those into two patches.

On the other hand, if you make a single change to numerous files, group those changes into a single patch.  Thus a single logical change is contained within a single patch.

The point to remember is that each patch should make an easily understood change that can be verified by reviewers.  Each patch should be justifiable on its own merits.

If one patch depends on another patch in order for a change to be complete, that is OK.  Simply note "this patch depends on patch X" in your patch description.

When dividing your change into a series of patches, take special care to ensure that the project builds and runs properly after each patch in the series.  Developers using "git bisect" to track down a problem can end up splitting your patch series at any point; they will not thank you if you introduce bugs in the middle. Compilation failures are especially annoying to deal with. 

### Style-check your changes

Check your patch for basic style violations. There should be none [if you have setup your project correctly following the instructions](#project-setup).
Patches causing unnecessary style/whitespace changes are messy and will likely be bounced back. 

**If you are touching old files and want to update them to current style conventions, please do so in a separate commit/PR. It is usually best to have this commit be the first in the series.**

### Writing a good commit message

Here is an example:

    feat: Create a Valo icon font for icons in Valo
    
    Valo uses only a handful of icons from Font Awesome. This change introduces a separate icon font for valo (9KB instead of 80KB) and decouples Valo from Font Awesome to enable updating Font Awesome without taking Valo into account.
    
    This change also makes it easy to not load Font Awesome when using Valo by setting $v-font-awesome:false
    
    For backwards compatibility, Font Awesome is loaded by default.
    
    Fixes #18472

#### Example breakdown - subject line

    feat: Create a Valo icon font for icons in Valo

Start with a good subject message in imperative form with 50 chars or less. A properly formed Git commit subject line should always be able to complete the following sentence:

If applied, this commit will <your subject line here>
    
Pending if type of changes you are doing, the subject line should start with either `feat/fix/chore/refactor`. In case there are breaking changes, use ! after the prefix, like `refactor!:`. In case you don't know what to write there, let the reviewer do it when merging the PR.

#### Describe the problem:

    Valo uses only a handful of icons from Font Awesome.

#### Describe the user impact & describe what was done to solve the problem:

    This change introduces a separate icon font for valo (9KB instead of 80KB) and decouples Valo from Font Awesome to enable updating Font Awesome without taking Valo into account.
    
    This change also makes it easy to not load Font Awesome when using Valo by setting $v-font-awesome:false
    
    For backwards compatibility, Font Awesome is loaded by default

#### Reference the issue

Reference an issue number using [the magic words](https://docs.github.com/en/free-pro-team@latest/github/managing-your-work-on-github/linking-a-pull-request-to-an-issue) to close the issue:
```
Fixes #18472
```
If the issue is not closed by this PR, you can still refer to it with e.g. `Part of #1234`.
In case the issue is in another repository, you can link to it with the syntax: `Part of vaadin/spring#1234` where the first part is for the organization, the second for the repository followed by the issue (or PR) number there.

### Include a test

Ideally, we would like all patches to include automated tests. Unit tests are preferred. If there’s a change to UI Code, we would additionally prefer an integration test.

Our integration tests use TestBench that requires a license. If you don't have any, it's ok: we can make do with a Test UI class that contains a test case as well a clear instructions for how to perform the test.
This also goes for features that are hard to test automatically.

After submitting a pull request, our CI system will trigger the verification build automatically, including integration tests and you will be able to see the whole build output and results.

Test cases should succeed with the patch and fail without the patch. That way, it’s clear to everyone that the test does in fact test what it is supposed to test. 

If the patch is a performance improvement, please include some benchmark data that tells us how much the performance is improved. You should also include the test code or UI class you used to benchmark. 

If you can clearly prove that the patch works, it dramatically increases the odds of it being included in a quick and timely fashion.

### Respond to review comments

Your pull request will almost certainly get comments from reviewers on ways in which the patch can be improved.  You must respond to those comments; ignoring reviewers is a good way to get ignored in return.  Review comments or questions that do not lead to a code change should almost certainly bring about a comment or changelog entry so that the next reviewer better understands what is going on.

Be sure to tell the reviewers what changes you are making. Respond politely to comments and address the problems they have pointed out. 

If there is feedback that is blocking merging of the pull request, and there is no response from the author in a reasonable time, we may reject it. You are then of course free to resubmit the pull request. The rejection is done not out of spite, but to keep the queue of incoming pull requests manageable and to prevent the queue from spiraling out of control. 

### Don't get discouraged - or impatient.

After you have submitted your change, be patient and wait.  Reviewers are busy people and may not get to your patch right away. Ideally, we try to get a response within one business day.

You should receive comments within a week or so; if that does not happen, make sure that you have sent your patches to the right place.  Wait for a minimum of one week before resubmitting or pinging reviewers - possibly longer during busy times like merge windows for minor or major release versions. 

## Submitting the patches

### Obtain a current source tree

The Flow repository can be cloned using `git clone git@github.com:vaadin/flow.git` or using your favorite Git tool.

Remember to do `git checkout master` and `git pull` to make sure you are creating your commits on top of a recent enough version. 

https://robots.thoughtbot.com/keeping-a-github-fork-updated has instructions on how to keep your local fork up to date.

### Creating a pull request in GitHub

All our projects accept contributions as GitHub pull requests. The first time you create a pull request, you will be asked to electronically sign a contribution agreement.

https://yangsu.github.io/pull-request-tutorial/ has instructions on how to create a pull request.

Please note that PR should target the `master` branch. 

**Remember to check the "Allow edits from maintainers" so we can rebase the PR or make small changes if necessary**.

# Tips for contributing code

## Running tests
The unit tests for the projects can be run using
<pre><code>mvn test</code></pre> or by selecting the test class in IDE and running it there.

IT tests can be run with
<pre><code>mvn verify</code></pre> or by starting the Jetty for the IT module with `jetty:run` and then runnign the test class in IDE.

To run IT tests locally, you'll need a [Testbench](https://vaadin.com/testbench) license and a Chrome browser installed (currently this is the only browser that IT tests are run in).
If you don't have the license, it's ok, our CI system will run those tests for you after you create a pull request. 
Refer to the [contribution guide](/CONTRIBUTING.md) for details. 

When running IT tests locally, by default, local Chrome is used to run tests, make sure it's installed.

## Building a package
The distribution package is built and installed into the local Maven repository by doing

1. mvn install

## Running SuperDevMode
Some flow internals use GWT in the client code. superDevMode allows to reload GWT changes on the fly, but it requires some setup first.

To start superDevMode do the following:

1. Get flow source code
1. If you are planning to launch the mode for the external application based on flow, first make sure that flow source code is of the same version as the application uses.
If it's not true, either update the application dependencies or check out the corresponding flow tag and rebuild both flow and the application.
1. Navigate to flow-client package in flow project
1. Run `mvn -Psdm clean install gwt:compile gwt:run-codeserver -DskipTests`
1. Start the application server
1. Open the application page and use the bookmarks to control dev mode
If you have no bookmarks, navigate to http://localhost:9876 to setup them.

Alternatively, in Eclipse run .launch files from flow-client/eclipse in the order:

1. Compile ClientEngine.launch
2. Super Dev Mode.launch

> NOTE! SuperDevMode should be compiled before the application server is launched,
> also, Flow version should match with the application one
> as else the application won't be able to run SDM and you will receive the
> exception `Can't find any GWT Modules on this page.`

More info about SuperDevMode: http://www.gwtproject.org/articles/superdevmode.html

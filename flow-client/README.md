# Vaadin Flow Client

This is the client part of flow. It is composed by two parts

   - The flow client protocol, developed in java and compiled with GWT.
   - An npm package that can be used as a JS library to use flow UI in any client app.

## Building the project

`mvn clean package` will do everything, and will leave a `.jar` file
with everything set in the `target` folder.

## Installing the `.jar` 

Run `mvn install` or just `mvn install -DskipTests` 

## Running the npm module

1. Install dependencies

Run either `npm install` or `mvn compile`

2. Compile Code and Run tests

Run either `npm test` or `mvn test`

3. Debug Tests

Run `npm run debug` then point your browser to http://localhost:9000/__intern/ to debug tests.

## Flow Express Build mode

Since Vaadin 24.0, the Flow application can be run in so called Express 
Build mode, where the frontend files are pre-compiled and bundled into {project.root}/src/main/dev-bundle directory. To make your changes in `flow-client` module be included into the Flow test modules, you have to delete this directory, so the Flow will re-compile the bundle and take into account your changes in `flow-client`:
   - remove dev-bundle in the current folder: `rm -rf src/main/dev-bundle`
   - remove dev-bundle in the sub-folders: find . -type d -name dev-bundle -delete

## Debugging

In short debugging is building flow client JS in pretty/detailed mode and then adding it to your project and then using it for debugging in the browser.
1. `git clone git@github.com:vaadin/flow.git`
2. `git checkout <appropriate branch/tag>`
3. Build the project: `mvn clean install -Dgwt.module.style=DETAILED -DskipTests` in the `flow-client` folder.
   - `-Dgwt.module.style=DETAILED` - this can be on of the followings: `DETAILED` or `PRETTY`
   - `-DskipTests` - skipping tests is recommended, because tests are not needed for debugging and make the build process slower
   - Do not mind the many ERRORS happening while building the JAR, eventually build can be SUCCESS still,
   - the built JAR should be at the /target directory e.g. `target/flow-client-X.Y-SNAPSHOT.jar`
4. Make sure your project is using the non-obfuscated `flow-client.jar`:
   - Please make sure that your build tool config file (`pom.xml` or `gradle.build`) contains the proper information:
     - `flow-client` dependency in an application project points to the built version (e.g.: be sure it is `X.Y-SNAPSHOT`),
   - OR just uncompress the built JAR file and then copy the `FlowClient.js` from it to your proper directory 
(where it is served to frontend) in e.g.: `frontend/generated/jar-resources`
     - this option will make sure you use the non-obfuscated JS file (the other option can cause still some optimization)
5. Then you can debug the JS code in your browser's dev tools (e.g.sources tab: `VAADIN/generated/jar-resources/FlowClient.js`)

#### Note for debugging:
   - you shall add the `flow-client` dependency if it is not present.
     - For example, your project may use Vaadin `24.2.0`, but you rebuild the flow client at version `24.2-SNAPSHOT`. 
In this case, you should probably add the `flow-client` dependency to overwrite the version the project is bringing in. 
   - Delete the dev-bundle (`src/main/dev-bundle`) directory to be sure the project will not use a previously bundled version of the client
   - clean the application project (`mvn vaadin:clean-frontend` or `mvn:flow-clean-frontend`) is also recommanded
to be sure that you are using the rebuilt version of the Flow client.

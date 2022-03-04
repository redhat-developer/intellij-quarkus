# Quarkus Tools for IntelliJ
[plugin-repo]: https://plugins.jetbrains.com/plugin/13234-quarkus
[plugin-version-svg]: https://img.shields.io/jetbrains/plugin/v/13234-quarkus.svg
[plugin-downloads-svg]: https://img.shields.io/jetbrains/plugin/d/13234-quarkus.svg

![Java CI with Gradle](https://github.com/redhat-developer/intellij-quarkus/workflows/Java%20CI%20with%20Gradle/badge.svg)
![Validate against IJ versions](https://github.com/redhat-developer/intellij-quarkus/workflows/Validate%20against%20IJ%20versions/badge.svg)
[![JetBrains plugins][plugin-version-svg]][plugin-repo]
[![JetBrains plugins][plugin-downloads-svg]][plugin-repo]
[![codecov](https://codecov.io/gh/redhat-developer/intellij-quarkus/branch/master/graph/badge.svg)](https://codecov.io/gh/redhat-developer/intellij-quarkus)


## Description

This JetBrains IntelliJ plugin provides support for Quarkus development via a 
[Quarkus language server](https://github.com/redhat-developer/quarkus-ls/tree/master/microprofile.ls).

### application.properties support

### Code completion (Ctrl + ENTER)

![](images/quarkus-tools.png)

### Property documentation

Select a property and press Ctrl+Q

![](images/quarkus-tools1.png)

### Goto property definition

Select a property and press Ctrl+B

![](images/quarkus-tools2.gif)

### Syntax validation

Wrong property key names or values are reported

![](images/quarkus-tools3.png)
![](images/quarkus-tools4.png)

### Property hover in Java files

Property managed in your code through @ConfigProperty:
  * code completion in application.properties is adjusted accordingly if you define new properties
  * hover over the property field will display the current value

![](images/quarkus-tools5.gif)

### CodeLens / Inlay

When editing a resource class while the current Quarkus application is running in development mode (`./mvnw compile quarkus:dev` or `./gradlew quarkusDev`), there are now CodeLenses that provide the URL for the GET endpoints.

This feature takes into account the path name and the HTTP server port from your application.properties file in order to create the URL.

Clicking on the CodeLens URL will open the URL in your default browser.

![](images/quarkus-tools15.gif)

### MicroProfile Health 

Syntax validation is being performed for Java files using the MicroProfile Health assets:

![](images/quarkus-tools6.gif)

### MicroProfile Fault Tolerance

MicroProfile Health related properties are supported in `application.properties` completion
and syntax validation as soon as you use MicroProfile related annotation in your Java
source files

![](images/quarkus-tools7.gif)

Methods referenced in `@Fallback(fallbackMethod)` are validated:

![](images/quarkus-tools16.gif)

Activating code completion on the `@Fallback(fallbackMethod)` value will get the list of available methods being proposed:

![](images/quarkus-tools17.gif)


### MicroProfile Rest Client

MicroProfile Rest Client references are checked against valid injections

![](images/quarkus-tools8.gif)

### MicroProfile LRA

MicroProfile LRA related properties references are supported in `application.properties` completion
and syntax validation as soon as you use MicroProfile LRA in your Quarkus application

![](images/quarkus-tools9.png)

### MicroProfile OpenAPI

MicroProfile OpenAPI related properties references are supported in `application.properties` completion
and syntax validation as soon as you use MicroProfile OpenAPI in your Quarkus application

![](images/quarkus-tools10.png)

### MicroProfile Metrics

MicroProfile Metrics related properties references are supported in `application.properties` completion
and syntax validation as soon as you use MicroProfile Metrics in your Quarkus application

![](images/quarkus-tools10.png)

Invalid use of the `@Gauge` annotation is reported as well:

![](images/quarkus-tools18.png)

### MicroProfile OpenTracing

MicroProfile OpenTracing related properties references are supported in `application.properties` completion
and syntax validation as soon as you use MicroProfile OpenTracing in your Quarkus application

![](images/quarkus-tools11.png)

### application.yaml

### Code completion (Ctrl+ENTER)

![](images/quarkus-tools12.png)

### Property documentation

Select a property and press Ctrl+Q

![](images/quarkus-tools13.png)

### Syntax validation

Wrong property key names or values are reported

![](images/quarkus-tools14.png)


## Quarkus project wizards
  * Generate a Quarkus Maven project, based on https://code.quarkus.io/
    - Call `File -> New -> Module -> Quarkus`
  * ~~Add Quarkus extensions to current Maven-based Quarkus project~~
    - NYI

## Quarkus `application.properties` Features
  * Completion support for Quarkus properties
  * Hover support for Quarkus properties
  * Validation support for Quarkus properties 
  * Support for Quarkus profiles
  * Outline support (flat or tree view)

## Quarkus run/debug command

It is possible to run or debug a Quarkus application from the IDE configurations.
Select the `Run -> Edit Configurations...` menu and click the `Add new...` link:

![](images/1.10.0/quarkus-tools1.png)

Select the `Quarkus` configuration type:

![](images/1.10.0/quarkus-tools2.png)

The first module of the project is selected. For single Quarkus projects, it is ok. If you have a multi module Quarkus project, you can change the module to test.
You can also specify a Quarkus profile to use (dev is the default) and environment variables (using the notation VARNAME=varvalue, separated by semicolon)

The click the `OK` button. The configuration will now appear on the top right side of the window:

![](images/1.10.0/quarkus-tools3.png)

If you click the Run icon, it will start the Quarkus application in dev mode. If you click the Debug button, it will start the Quarkus application in dev mode and connect a local Java debugger to the running application.

![](images/1.10.0/quarkus-tools4.png)

## Quarkus code snippets

This plugin provides several code snippets, available when editing Java files:

  * **qrc** - Create a new Quarkus resource class
  * **qrm** - Create a new Quarkus resource method
  * **qtrc** - Create a new Quarkus test resource class
  * **qntrc** - Create a new Quarkus native test resource class

When editing `application.properties` files, you have access to:

  * **qds** - Configure a Quarkus datasource
  * **qj** - Configure a Jaeger tracer


## Requirements

  * Java JDK (or JRE) 8 or more recent

    ​    
## Contributing

This is an open source project open to anyone. Contributions are extremely welcome!

### Building

Project is managed by Gradle. So building is quite easy.

#### Building the plugin distribution file

Run the following command:

```sh
./gradlew buildPlugin
```
The plugin distribution file is located in ```build/distributions```.

#### Testing

You can also easily test the plugin. Just run the following command:

```sh
./gradlew runIde
```

#### Unit test infrastructure

The IntelliJ SDK does not provide helpers to create Maven or Gradle based project.
But the Maven and Gradle plugins have some test class helpers but they are not part
of the IntelliJ SDK or the respective plugin distributions.

So we extracted these classes into the ```intellij-community``` folder. But as these
classes are highly linked to the version of the IntelliJ SDK used to build, there is
a script to copy them from the GitHub intellij-community repository.

This script is ```pulltest.sh```

If the version of the IntelliJ SDK used to build is changed (see gradle.properties), you must
update the branch in ```pulltest.sh``` and run the script again.

#### UI testing

You can perform UI testing by running the following command:

```sh
./gradlew clean runIdeForUiTests -PideaVersion=IC-2020.2& ./gradlew integrationTest 
```

Data and Telemetry
==================
The JetBrains IntelliJ Quarkus Tools plugin collects anonymous [usage data](USAGE_DATA.md) and sends it to Red Hat servers to help improve our products and services. Read our [privacy statement](https://developers.redhat.com/article/tool-data-collection) to learn more. This extension respects the Red Hat Telemetry setting which you can learn more about at [https://github.com/redhat-developer/intellij-redhat-telemetry#telemetry-reporting](https://github.com/redhat-developer/intellij-redhat-telemetry#telemetry-reporting)

## Feedback

File a bug in [GitHub Issues](https://github.com/redhat-developer/intellij-quarkus/issues).

## License

Eclipse Public License 2.0.
See [LICENSE](LICENSE) file.

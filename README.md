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
  
### MicroProfile Health 

Syntax validation is being performed for Java files using the MicroProfile Health assets:

![](images/quarkus-tools6.gif)

### MicroProfile Fault Tolerance

MicroProfile Health related properties are supported in `application.properties` completion
and syntax validation as soon as you use MicroProfile related annotation in your Java
source files

![](images/quarkus-tools7.gif)

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

### MicroProfile OpenTracing

MicroProfile OpenTracing related properties references are supported in `application.properties` completion
and syntax validation as soon as you use MicroProfile OpenTracing in your Quarkus application

![](images/quarkus-tools11.png)

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

## ~~Quarkus debug command~~
  NYI
  
  Launches the Maven quarkus:dev plugin and automatically attaches a debugger

  TBD
  
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

## Feedback

File a bug in [GitHub Issues](https://github.com/redhat-developer/intellij-quarkus/issues).

## License

Eclipse Public License 2.0.
See [LICENSE](LICENSE) file.
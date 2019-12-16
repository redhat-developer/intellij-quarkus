# Quarkus Tools for IntelliJ
[plugin-repo]: https://plugins.jetbrains.com/plugin/13234-quarkus
[plugin-version-svg]: https://img.shields.io/jetbrains/plugin/v/13234-quarkus.svg
[plugin-downloads-svg]: https://img.shields.io/jetbrains/plugin/d/13234-quarkus.svg

[![Build Status](https://travis-ci.com/redhat-developer/intellij-openshift-connector.svg?branch=master)](https://travis-ci.com/redhat-developer/intellij-quarkus)
[![JetBrains plugins][plugin-version-svg]][plugin-repo]
[![JetBrains plugins][plugin-downloads-svg]][plugin-repo]


## Description

This JetBrains IntelliJ plugin provides support for Quarkus development via a 
[Quarkus language server](https://github.com/redhat-developer/quarkus-ls/tree/master/quarkus.ls).

![](images/propertiesSupport.png)

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
  
## ~~Quarkus code snippets~~
NYI

This extension provides several code snippets, available when editing Java files:

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
# Quarkus Tools for IntelliJ - User Guide

Welcome to the Quarkus Tools for IntelliJ user guide! This guide provides comprehensive documentation for all features of the plugin.

## Overview

[Quarkus Tools for IntelliJ](https://plugins.jetbrains.com/plugin/13234-quarkus-tools) is a powerful plugin that provides comprehensive support for [Quarkus](https://quarkus.io/) and [Qute](https://quarkus.io/guides/qute-reference) development in IntelliJ IDEA.

The plugin leverages:
- [MicroProfile Language Server](https://github.com/eclipse/lsp4mp/tree/master/microprofile.ls) for Quarkus/MicroProfile support
- [Qute Language Server](https://github.com/redhat-developer/quarkus-ls/tree/master/qute.ls) for Qute template support
- [LSP4IJ](https://github.com/redhat-developer/lsp4ij) to integrate these language servers into IntelliJ IDEA

## Quarkus/MicroProfile Support

### Getting Started
- [Create a Quarkus Application](./quarkus/Wizard.md) - Use the wizard to bootstrap your Quarkus project

### Development Features
- [Quarkus Editing Support](./quarkus/EditingSupport.md)
  - Code completion, validation, and hover in `application.properties` and `microprofile-config.properties`
  - Java file support for MicroProfile annotations
  - Quick fixes and code actions
  - Support for Quarkus profiles (`%dev`, `%prod`, etc.)

- [Quarkus Debugging Support](quarkus/RunningSupport.md)
  - Debug Quarkus applications with hot reload
  - DevServices integration
  - Live coding support

## Qute Template Support

### Core Features
- [Qute Editing Support](./qute/EditingSupport.md)
  - Syntax coloration and highlighting
  - Expression completion and validation
  - Directive support (`{#for}`, `{#if}`, etc.)
  - Quick fixes for common errors
  - Alternative expression syntax support
  - Qute REST support

- [Qute Debugging Support](./qute/DebuggingSupport.md)
  - Breakpoints in Qute templates
  - Variable inspection
  - Expression evaluation

### Framework Extensions
- [Renarde Support](./qute/RenardeSupport.md) - Specific support for the Renarde web framework
- [Roq Support](./qute/RoqSupport.md) - Support for Roq static site generator
- [Web Bundler Support](./qute/WebBundler.md) - Asset management and bundling

## Additional Resources

- [Plugin on JetBrains Marketplace](https://plugins.jetbrains.com/plugin/13234-quarkus-tools)
- [GitHub Repository](https://github.com/redhat-developer/intellij-quarkus)
- [Quarkus Official Documentation](https://quarkus.io/)
- [Qute Reference Guide](https://quarkus.io/guides/qute-reference)

## Getting Help

- Report issues on [GitHub Issues](https://github.com/redhat-developer/intellij-quarkus/issues)
- Check the [README](../README.md) for additional examples and features

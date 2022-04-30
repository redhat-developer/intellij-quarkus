rm -rf intellij-community-tmp 2>/dev/null
git clone --single-branch --branch 203.5981 https://github.com/JetBrains/intellij-community intellij-community-tmp
mkdir -p intellij-community/java/compiler/tests/com/intellij/compiler/artifacts
cp -p intellij-community-tmp/java/compiler/tests/com/intellij/compiler/artifacts/ArtifactsTestUtil.java intellij-community/java/compiler/tests/com/intellij/compiler/artifacts
mkdir -p intellij-community/platform/testFramework/extensions/src/com/intellij/testFramework/assertions
cp -p intellij-community-tmp/platform/testFramework/extensions/src/com/intellij/testFramework/assertions/Assertions.java intellij-community/platform/testFramework/extensions/src/com/intellij/testFramework/assertions
cp -p intellij-community-tmp/platform/testFramework/extensions/src/com/intellij/testFramework/assertions/JdomAssert.kt intellij-community/platform/testFramework/extensions/src/com/intellij/testFramework/assertions
cp -p intellij-community-tmp/platform/testFramework/extensions/src/com/intellij/testFramework/assertions/StringAssertEx.kt intellij-community/platform/testFramework/extensions/src/com/intellij/testFramework/assertions
cp -p intellij-community-tmp/platform/testFramework/extensions/src/com/intellij/testFramework/assertions/snapshot.kt intellij-community/platform/testFramework/extensions/src/com/intellij/testFramework/assertions
cp -p intellij-community-tmp/platform/testFramework/extensions/src/com/intellij/testFramework/assertions/PathAssertEx.kt intellij-community/platform/testFramework/extensions/src/com/intellij/testFramework/assertions
mkdir -p intellij-community/platform/lang-impl/testSources/com/intellij/openapi/roots/ui/configuration
cp intellij-community-tmp/platform/lang-impl/testSources/com/intellij/openapi/roots/ui/configuration/SdkTestCase.kt intellij-community/platform/lang-impl/testSources/com/intellij/openapi/roots/ui/configuration
mkdir -p intellij-community/platform/external-system-impl/testSrc/com/intellij/openapi/externalSystem/test
cp -p intellij-community-tmp/platform/external-system-impl/testSrc/com/intellij/openapi/externalSystem/test/ExternalSystemImportingTestCase.java intellij-community/platform/external-system-impl/testSrc/com/intellij/openapi/externalSystem/test
cp -p intellij-community-tmp/platform/external-system-impl/testSrc/com/intellij/openapi/externalSystem/test/ExternalSystemTestCase.java intellij-community/platform/external-system-impl/testSrc/com/intellij/openapi/externalSystem/test
mkdir -p intellij-community/platform/external-system-impl/testSrc/com/intellij/openapi/externalSystem/service/execution
cp intellij-community-tmp/platform/external-system-impl/testSrc/com/intellij/openapi/externalSystem/service/execution/TestUnknownSdkResolver.kt intellij-community/platform/external-system-impl/testSrc/com/intellij/openapi/externalSystem/service/execution
mkdir -p intellij-community/plugins/maven/src/test/java/org/jetbrains/idea/maven
cp intellij-community-tmp/plugins/maven/src/test/java/org/jetbrains/idea/maven/MavenImportingTestCase.java intellij-community/plugins/maven/src/test/java/org/jetbrains/idea/maven
cp intellij-community-tmp/plugins/maven/src/test/java/org/jetbrains/idea/maven/MavenTestCase.java intellij-community/plugins/maven/src/test/java/org/jetbrains/idea/maven
cp intellij-community-tmp/plugins/maven/src/test/java/org/jetbrains/idea/maven/NullMavenConsole.java intellij-community/plugins/maven/src/test/java/org/jetbrains/idea/maven
mkdir -p intellij-community/plugins/gradle/testSources/org/jetbrains/plugins/gradle/importing
cp -p intellij-community-tmp/plugins/gradle/testSources/org/jetbrains/plugins/gradle/importing/GradleImportingTestCase.java intellij-community/plugins/gradle/testSources/org/jetbrains/plugins/gradle/importing
mkdir -p intellij-community/plugins/gradle/tooling-extension-impl/testSources/org/jetbrains/plugins/gradle/tooling/builder
cp -p intellij-community-tmp/plugins/gradle/tooling-extension-impl/testSources/org/jetbrains/plugins/gradle/tooling/builder/AbstractModelBuilderTest.java intellij-community/plugins/gradle/tooling-extension-impl/testSources/org/jetbrains/plugins/gradle/tooling/builder
cp -p intellij-community-tmp/plugins/gradle/tooling-extension-impl/testSources/org/jetbrains/plugins/gradle/tooling/VersionMatcherRule.java intellij-community/plugins/gradle/tooling-extension-impl/testSources/org/jetbrains/plugins/gradle/tooling


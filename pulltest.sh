rm -rf intellij-community-tmp 2>/dev/null 
git clone --single-branch --branch 191.6183 https://github.com/JetBrains/intellij-community intellij-community-tmp
mkdir -p intellij-community/java/compiler/tests/com/intellij/compiler/artifacts
cp -p intellij-community-tmp/java/compiler/tests/com/intellij/compiler/artifacts/ArtifactsTestUtil.java intellij-community/java/compiler/tests/com/intellij/compiler/artifacts
mkdir -p intellij-community/platform/external-system-impl/testSrc/com/intellij/openapi/externalSystem/test
cp -p intellij-community-tmp/platform/external-system-impl/testSrc/com/intellij/openapi/externalSystem/test/ExternalSystemImportingTestCase.java intellij-community/platform/external-system-impl/testSrc/com/intellij/openapi/externalSystem/test
cp -p intellij-community-tmp/platform/external-system-impl/testSrc/com/intellij/openapi/externalSystem/test/ExternalSystemTestCase.java intellij-community/platform/external-system-impl/testSrc/com/intellij/openapi/externalSystem/test
mkdir -p intellij-community/plugins/gradle/testSources/org/jetbrains/plugins/gradle/importing
cp -p intellij-community-tmp/plugins/gradle/testSources/org/jetbrains/plugins/gradle/importing/GradleImportingTestCase.java intellij-community/plugins/gradle/testSources/org/jetbrains/plugins/gradle/importing
mkdir -p intellij-community/plugins/gradle/tooling-extension-impl/testSources/org/jetbrains/plugins/gradle/tooling/builder
cp -p intellij-community-tmp/plugins/gradle/tooling-extension-impl/testSources/org/jetbrains/plugins/gradle/tooling/builder/AbstractModelBuilderTest.java intellij-community/plugins/gradle/tooling-extension-impl/testSources/org/jetbrains/plugins/gradle/tooling/builder
cp -p intellij-community-tmp/plugins/gradle/tooling-extension-impl/testSources/org/jetbrains/plugins/gradle/tooling/VersionMatcherRule.java intellij-community/plugins/gradle/tooling-extension-impl/testSources/org/jetbrains/plugins/gradle/tooling
rm -rf intellij-community-tmp 2>/dev/null
package com.redhat.devtools.intellij.quarkus.lsp4ij.server;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * {@link ProcessStreamConnectionProvider} to start a language server written in Java.
 */
public class JavaProcessStreamConnectionProvider extends ProcessStreamConnectionProvider {

    /**
     * Initialize Java commands with the 'java' path and debug port if  it is filled.
     *
     * @param debugPort the debug port and null otherwise.
     * @return the initialized Java commands.
     */
    protected static List<String> createJavaCommands(String debugPort) {
        List<String> commands = new ArrayList<>();
        commands.add(computeJavaPath());
        if (debugPort != null) {
            commands.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + debugPort);
        }
        return commands;
    }

    private static String computeJavaPath() {
        String javaPath = "java";
        boolean existsInPath = Stream.of(System.getenv("PATH").split(Pattern.quote(File.pathSeparator))).map(Paths::get)
                .anyMatch(path -> Files.exists(path.resolve("java")));
        if (!existsInPath) {
            File f = new File(System.getProperty("java.home"),
                    "bin/java" + (OS.current() == OS.WINDOWS ? ".exe" : ""));
            javaPath = f.getAbsolutePath();
        }
        return javaPath;
    }

}

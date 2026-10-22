/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.microprofile.psi.internal.quarkus.flow;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProject;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves Quarkus Flow workflow files (YAML/JSON, under {@code src/main/resources/flow} by
 * default) for a given {@code @Identifier(...)} value.
 *
 * <p>
 * Workflow files are <b>not</b> identified by their file name: the Quarkus Flow runtime scans
 * every workflow file under the configured directory and derives the identifier from the
 * {@code document.namespace}/{@code document.name}/{@code document.version} fields found in the
 * file itself.
 * </p>
 *
 * @see <a href="https://docs.quarkiverse.io/quarkus-flow/dev/workflow-definitions.html">Quarkus Flow workflow definitions</a>
 */
public class PsiFlowUtils {

	private PsiFlowUtils() {
	}

	/**
	 * Returns the workflow file matching the given {@code @Identifier} value for the given Java
	 * module, or <code>null</code> if none matches.
	 *
	 * @param javaProject the Java module, must not be null.
	 * @param identifier  the {@code @Identifier(...)} value, must not be null.
	 * @return the matching workflow file, or <code>null</code>.
	 */
	public static @Nullable VirtualFile findWorkflowFile(@NotNull Module javaProject, @NotNull String identifier) {
		PsiMicroProfileProject mpProject = PsiMicroProfileProjectManager.getInstance(javaProject.getProject())
				.getMicroProfileProject(javaProject);

		String flowDirName = mpProject.getProperty(FlowConstants.FLOW_DEFINITIONS_DIR_PROPERTY,
				FlowConstants.DEFAULT_FLOW_DEFINITIONS_DIR);
		VirtualFile flowDir = findFlowDir(javaProject, flowDirName);
		if (flowDir == null || !flowDir.exists()) {
			return null;
		}

		boolean followSymlinks = Boolean.parseBoolean(
				mpProject.getProperty(FlowConstants.FLOW_FOLLOW_SYMLINKS_PROPERTY, "false"));
		List<WorkflowInfo> candidates = new ArrayList<>();
		collectWorkflowFiles(flowDir, followSymlinks, candidates);
		if (candidates.isEmpty()) {
			return null;
		}

		boolean classStrategy = FlowConstants.NAMING_STRATEGY_CLASS.equalsIgnoreCase(
				mpProject.getProperty(FlowConstants.FLOW_NAMING_STRATEGY_PROPERTY, FlowConstants.NAMING_STRATEGY_SPEC));
		if (classStrategy) {
			for (WorkflowInfo info : candidates) {
				if (identifier.equals(info.getClassIdentifier())) {
					return info.file;
				}
			}
			return null;
		}
		return findBySpecIdentifier(identifier, candidates);
	}

	/**
	 * Matches the {@code namespace:name} / {@code namespace:name:version} "spec" naming strategy.
	 * When the identifier has no version, the candidate with the highest semver version wins -
	 * mirroring the Quarkus Flow runtime's "versionless bean resolves to the highest version"
	 * behavior.
	 */
	private static @Nullable VirtualFile findBySpecIdentifier(String identifier, List<WorkflowInfo> candidates) {
		int firstColon = identifier.indexOf(':');
		if (firstColon < 0) {
			return null;
		}
		String namespace = identifier.substring(0, firstColon);
		String remainder = identifier.substring(firstColon + 1);
		int secondColon = remainder.indexOf(':');
		String name = secondColon < 0 ? remainder : remainder.substring(0, secondColon);
		String version = secondColon < 0 ? null : remainder.substring(secondColon + 1);

		WorkflowInfo best = null;
		for (WorkflowInfo info : candidates) {
			if (!namespace.equals(info.namespace) || !name.equals(info.name)) {
				continue;
			}
			if (version != null) {
				if (version.equals(info.version)) {
					return info.file;
				}
			} else if (best == null || compareVersions(info.version, best.version) > 0) {
				best = info;
			}
		}
		return best != null ? best.file : null;
	}

	private static @Nullable VirtualFile findFlowDir(@NotNull Module javaProject, @NotNull String flowDirName) {
		for (VirtualFile resourcesDir : ModuleRootManager.getInstance(javaProject)
				.getSourceRoots(JavaResourceRootType.RESOURCE)) {
			VirtualFile flowDir = resourcesDir.findFileByRelativePath(flowDirName);
			if (flowDir != null && flowDir.exists()) {
				return flowDir;
			}
		}
		return null;
	}

	private static void collectWorkflowFiles(@NotNull VirtualFile dir, boolean followSymlinks,
			@NotNull List<WorkflowInfo> out) {
		for (VirtualFile child : dir.getChildren()) {
			if (!followSymlinks && child.is(VFileProperty.SYMLINK)) {
				// Skipped by default to prevent duplicates, same as the Quarkus Flow runtime.
				continue;
			}
			if (child.isDirectory()) {
				collectWorkflowFiles(child, followSymlinks, out);
			} else if (hasSupportedExtension(child.getName())) {
				WorkflowInfo info = parseDocumentHeader(child);
				if (info != null) {
					out.add(info);
				}
			}
		}
	}

	private static boolean hasSupportedExtension(String fileName) {
		String lower = fileName.toLowerCase(Locale.ROOT);
		for (String extension : FlowConstants.SUPPORTED_EXTENSIONS) {
			if (lower.endsWith(extension)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private static @Nullable WorkflowInfo parseDocumentHeader(@NotNull VirtualFile file) {
		try (InputStream input = file.getInputStream()) {
			Object loaded = new Yaml().load(input);
			if (!(loaded instanceof Map)) {
				return null;
			}
			Object documentNode = ((Map<String, Object>) loaded).get("document");
			if (!(documentNode instanceof Map)) {
				return null;
			}
			Map<String, Object> document = (Map<String, Object>) documentNode;
			Object namespace = document.get("namespace");
			Object name = document.get("name");
			if (namespace == null || name == null) {
				return null;
			}
			Object version = document.get("version");
			return new WorkflowInfo(file, namespace.toString(), name.toString(),
					version != null ? version.toString() : null);
		} catch (Exception e) {
			// Not a parsable/well-formed workflow file
			return null;
		}
	}

	/**
	 * Compares two dotted, numeric version strings (e.g. {@code "1.2.0"}); a higher version wins.
	 * Falls back to 0 for non-numeric/missing parts rather than failing, since this is only used to
	 * pick the "latest" among otherwise-matching candidates.
	 */
	private static int compareVersions(@Nullable String v1, @Nullable String v2) {
		if (v1 == null) {
			return v2 == null ? 0 : -1;
		}
		if (v2 == null) {
			return 1;
		}
		String[] parts1 = v1.split("\\.");
		String[] parts2 = v2.split("\\.");
		int length = Math.max(parts1.length, parts2.length);
		for (int i = 0; i < length; i++) {
			int cmp = Long.compare(parseVersionPart(parts1, i), parseVersionPart(parts2, i));
			if (cmp != 0) {
				return cmp;
			}
		}
		return 0;
	}

	private static long parseVersionPart(String[] parts, int index) {
		if (index >= parts.length) {
			return 0;
		}
		try {
			return Long.parseLong(parts[index].replaceAll("[^0-9].*$", ""));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private record WorkflowInfo(VirtualFile file, String namespace, String name, String version) {

			String getClassIdentifier() {
				String versionSegment = version != null ? "v" + version.replace('.', '_') + "." : "";
				return namespace + "." + versionSegment + toPascalCase(name);
			}

			private static String toPascalCase(String value) {
				StringBuilder result = new StringBuilder();
				boolean upperNext = true;
				for (char c : value.toCharArray()) {
					if (c == '-' || c == '_' || c == ' ') {
						upperNext = true;
						continue;
					}
					result.append(upperNext ? Character.toUpperCase(c) : c);
					upperNext = false;
				}
				return result.toString();
			}
		}

}

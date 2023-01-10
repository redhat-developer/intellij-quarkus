/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiRecordComponent;
import com.intellij.psi.impl.java.stubs.PsiModifierListStub;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.AbstractTypeResolver;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.ClassFileTypeResolver;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.ITypeResolver;
import com.redhat.devtools.intellij.qute.psi.internal.template.JavaTypesSearch;
import com.redhat.devtools.intellij.qute.psi.internal.template.QuarkusIntegrationForQute;
import com.redhat.devtools.intellij.qute.psi.internal.template.QuteSupportForTemplateGenerateMissingJavaMemberHandler;
import com.redhat.devtools.intellij.qute.psi.internal.template.TemplateDataSupport;
import com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.qute.psi.utils.QuteReflectionAnnotationUtils;
import com.redhat.qute.commons.DocumentFormat;
import com.redhat.qute.commons.GenerateMissingJavaMemberParams;
import com.redhat.qute.commons.QuteJavadocParams;
import org.eclipse.lsp4j.Location;

import com.redhat.qute.commons.InvalidMethodReason;
import com.redhat.qute.commons.JavaFieldInfo;
import com.redhat.qute.commons.JavaMethodInfo;
import com.redhat.qute.commons.JavaTypeInfo;
import com.redhat.qute.commons.ProjectInfo;
import com.redhat.qute.commons.QuteJavaDefinitionParams;
import com.redhat.qute.commons.QuteJavaTypesParams;
import com.redhat.qute.commons.QuteProjectParams;
import com.redhat.qute.commons.QuteResolvedJavaTypeParams;
import com.redhat.qute.commons.ResolvedJavaTypeInfo;
import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.DataModelProject;
import com.redhat.qute.commons.datamodel.DataModelTemplate;
import com.redhat.qute.commons.datamodel.QuteDataModelProjectParams;
import com.redhat.qute.commons.usertags.QuteUserTagParams;
import com.redhat.qute.commons.usertags.UserTagInfo;
import org.eclipse.lsp4j.WorkspaceEdit;

import static com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils.findType;

/**
 * Qute support for Template file.
 * 
 * @author Angelo ZERR
 *
 */
public class QuteSupportForTemplate {

	private static final Logger LOGGER = Logger.getLogger(QuteSupportForTemplate.class.getName());

	private static final String JAVA_LANG_OBJECT = "java.lang.Object";

	private static final QuteSupportForTemplate INSTANCE = new QuteSupportForTemplate();

	public static QuteSupportForTemplate getInstance() {
		return INSTANCE;
	}

	/**
	 * Returns the project information for the given project Uri.
	 * 
	 * @param params  the project information parameters.
	 * @param utils   the JDT LS utility.
	 * @param monitor the progress monitor.
	 * 
	 * @return the project information for the given project Uri and null otherwise.
	 */
	public ProjectInfo getProjectInfo(QuteProjectParams params, IPsiUtils utils, ProgressIndicator monitor) {
		Module javaProject = getJavaProjectFromTemplateFile(params.getTemplateFileUri(), utils);
		if (javaProject == null) {
			return null;
		}

		utils = utils.refine(javaProject);

		return PsiQuteProjectUtils.getProjectInfo(javaProject);
	}

	/**
	 * Collect data model templates from the given project Uri. A data model
	 * template can be declared with:
	 * 
	 * <ul>
	 * <li>@CheckedTemplate support: collect parameters for Qute Template by
	 * searching @CheckedTemplate annotation.</li>
	 * <li>Template field support: collect parameters for Qute Template by searching
	 * Template instance declared as field in Java class.</li>
	 * <li>Template extension support: see
	 * https://quarkus.io/guides/qute-reference#template_extension_methods</li>
	 * </ul>
	 * 
	 * @param params  the project uri.
	 * @param utils   JDT LS utilities
	 * @param monitor the progress monitor
	 * 
	 * @return data model templates from the given project Uri.
	 */
	public DataModelProject<DataModelTemplate<DataModelParameter>> getDataModelProject(
			QuteDataModelProjectParams params, IPsiUtils utils, ProgressIndicator monitor) {
		String projectUri = params.getProjectUri();
		Module javaProject = getJavaProjectFromProjectUri(projectUri, utils);
		if (javaProject == null) {
			return null;
		}

		utils = utils.refine(javaProject);

		return QuarkusIntegrationForQute.getDataModelProject(javaProject, utils, monitor);
	}

	/**
	 * Collect user tags from the given project Uri.
	 * 
	 * @param params  the project uri.
	 * @param utils   JDT LS utilities
	 * @param monitor the progress monitor
	 * 
	 * @return user tags from the given project Uri.
	 */
	public List<UserTagInfo> getUserTags(QuteUserTagParams params, IPsiUtils utils, ProgressIndicator monitor)
			{
		String projectUri = params.getProjectUri();
		Module javaProject = getJavaProjectFromProjectUri(projectUri, utils);
		if (javaProject == null) {
			return null;
		}
		return QuarkusIntegrationForQute.getUserTags(javaProject, monitor);
	}

	/**
	 * Returns Java types for the given pattern which belong to the given project
	 * Uri.
	 * 
	 * @param params  the java types parameters.
	 * @param utils   the JDT LS utility.
	 * @param monitor the progress monitor.
	 * 
	 * @return list of Java types.
	 */
	public List<JavaTypeInfo> getJavaTypes(QuteJavaTypesParams params, IPsiUtils utils, ProgressIndicator monitor) {
		String projectUri = params.getProjectUri();
		Module javaProject = getJavaProjectFromProjectUri(projectUri, utils);
		if (javaProject == null) {
			return null;
		}

		utils = utils.refine(javaProject);

		return new JavaTypesSearch(params.getPattern(), javaProject).search(monitor);
	}

	/**
	 * Returns the Java definition of the given Java type, method, field, method
	 * parameter, method invocation parameter and null otherwise.
	 * 
	 * @param params  the Java element information.
	 * @param utils   the JDT LS utility.
	 * @param monitor the progress monitor.
	 * 
	 * @return the Java definition of the given Java type, method, field, method
	 *         parameter, method invocation parameter and null otherwise.
	 */
	public Location getJavaDefinition(QuteJavaDefinitionParams params, IPsiUtils utils, ProgressIndicator monitor) {
		String projectUri = params.getProjectUri();
		Module javaProject = getJavaProjectFromProjectUri(projectUri, utils);
		if (javaProject == null) {
			return null;
		}

		utils = utils.refine(javaProject);

		PsiClass type = getTypeFromParams(params.getSourceType(), params.getProjectUri(), javaProject, monitor);
		if (type == null) {
			return null;
		}

		String parameterName = params.getSourceParameter();
		boolean dataMethodInvocation = parameterName != null && params.isDataMethodInvocation();

		String fieldName = params.getSourceField();
		if (fieldName != null) {
			PsiField field = type.findFieldByName(fieldName, true);
			if (field == null || !field.isValid()) {
				// The field doesn't exist
				return null;
			}

			if (dataMethodInvocation) {
				// returns the location of "data" method invocation with the given parameter
				// name
				return TemplateDataSupport.getDataMethodInvocationLocation(field, parameterName, utils, monitor);
			}
			// returns field location
			return utils.toLocation(field);
		}

		String sourceMethod = params.getSourceMethod();
		if (sourceMethod != null) {
			PsiMethod method = findMethod(type, sourceMethod);
			if (method == null || !method.isValid()) {
				// The method doesn't exist
				return null;
			}

			if (parameterName != null) {
				if (dataMethodInvocation) {
					// returns the location of "data" method invocation with the given parameter
					// name
					return TemplateDataSupport.getDataMethodInvocationLocation(method, parameterName, utils, monitor);
				}
				PsiParameter[] parameters = method.getParameterList().getParameters();
				for (PsiParameter parameter : parameters) {
					if (parameterName.equals(parameter.getName())) {
						// returns the method parameter location
						return utils.toLocation(parameter);
					}
				}
				return null;
			}
			// returns method location
			return utils.toLocation(method);
		}
		// returns Java type location
		return utils.toLocation(type);
	}

	private PsiMethod findMethod(PsiClass type, String sourceMethod) {
		// For the moment we search method only by name
		// FIXME:use method signature to retrieve the proper method (see findMethodOLD)
		PsiMethod[] methods = type.getMethods();
		for (PsiMethod method : methods) {
			if (sourceMethod.equals(method.getName())) {
				return method;
			}
		}
		return null;
	}

	/**
	 * Returns the resolved type (fields and methods) for the given Java type.
	 * 
	 * @param params  the Java type to resolve.
	 * @param utils   the JDT LS utility.
	 * @param monitor the progress monitor.
	 * 
	 * @return the resolved type (fields and methods) for the given Java type.
	 */
	public ResolvedJavaTypeInfo getResolvedJavaType(QuteResolvedJavaTypeParams params, IPsiUtils utils,
			ProgressIndicator monitor) {
		if (monitor.isCanceled()) {
			throw new ProcessCanceledException();
		}
		String projectUri = params.getProjectUri();
		Module javaProject = getJavaProjectFromProjectUri(projectUri, utils);
		if (javaProject == null) {
			return null;
		}

		utils = utils.refine(javaProject);

		String typeName = params.getClassName();

		// ex : org.acme.Item, java.util.List, ...
		PsiClass type = findType(typeName, javaProject, monitor);
		if (type == null) {
			return null;
		}

		ITypeResolver typeResolver = createTypeResolver(type, javaProject);

		// 1) Collect fields
		List<JavaFieldInfo> fieldsInfo = new ArrayList<>();

		// Standard fields
		PsiField[] fields = type.getFields();
		for (PsiField field : fields) {
			if (isValidField(field, type)) {
				// Only public fields are available
				JavaFieldInfo info = new JavaFieldInfo();
				info.setSignature(typeResolver.resolveFieldSignature(field));
				fieldsInfo.add(info);
			}
		}

		// Record fields
		if (type.isRecord()) {
			for (PsiRecordComponent field : type.getRecordComponents()) {
				// All record components are valid
				JavaFieldInfo info = new JavaFieldInfo();
				info.setSignature(typeResolver.resolveFieldSignature(field));
				fieldsInfo.add(info);
			}
		}

		// 2) Collect methods
		List<JavaMethodInfo> methodsInfo = new ArrayList<>();
		Map<String, InvalidMethodReason> invalidMethods = new HashMap<>();
		PsiMethod[] methods = type.getMethods();
		for (PsiMethod method : methods) {
			if (isValidMethod(method, type)) {
				try {
					InvalidMethodReason invalid = getValidMethodForQute(method, typeName);
					if (invalid != null) {
						invalidMethods.put(method.getName(), invalid);
					} else {
						JavaMethodInfo info = new JavaMethodInfo();
						info.setSignature(typeResolver.resolveMethodSignature(method));
						methodsInfo.add(info);
					}
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE,
							"Error while getting method signature of '" + method.getName() + "'.", e);
				}
			}
		}

		String typeSignature = AbstractTypeResolver.resolveJavaTypeSignature(type);
		if (typeSignature != null) {
			ResolvedJavaTypeInfo resolvedType = new ResolvedJavaTypeInfo();
			resolvedType.setBinary(type instanceof PsiCompiledElement);
			resolvedType.setSignature(typeSignature);
			resolvedType.setFields(fieldsInfo);
			resolvedType.setMethods(methodsInfo);
			resolvedType.setInvalidMethods(invalidMethods);
			resolvedType.setExtendedTypes(typeResolver.resolveExtendedType());
			resolvedType.setJavaTypeKind(PsiTypeUtils.getJavaTypeKind(type));
			QuteReflectionAnnotationUtils.collectAnnotations(resolvedType, type, typeResolver, javaProject);
			return resolvedType;
		}
		return null;
	}

	private static boolean isValidField(PsiField field, PsiClass type) {
		if (type.isEnum()) {
			return true;
		}
		return field.getModifierList().hasExplicitModifier(PsiModifier.PUBLIC);
	}

	private static boolean isSynthetic(PsiMember member) {
		var modifiers = member.getModifierList();
		var result = false;

		if (modifiers instanceof StubBasedPsiElementBase) {
			PsiModifierListStub stub = (PsiModifierListStub) ((StubBasedPsiElementBase<?>) modifiers).getGreenStub();
			result = (stub.getModifiersMask() & 0x00001000) == 0x00001000;
		}
		return result;
	}

	private static boolean isValidMethod(PsiMethod method, PsiClass type) {
		try {
			if (method.isConstructor() || !method.isValid() || isSynthetic(method)) {
				return false;
			}
			if (!type.isInterface() && !method.getModifierList().hasExplicitModifier(PsiModifier.PUBLIC)) {
				return false;
			}

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error while checking if '" + method.getName() + "' is valid.", e);
			return false;
		}
		return true;
	}

	/**
	 * Returns the reason
	 * 
	 * @param method
	 * @param typeName
	 * 
	 * @return
	 * 
	 * @see <a href="https://github.com/quarkusio/quarkus/blob/ce19ff75e9f732ff731bb30c2141b44b42c66050/independent-projects/qute/core/src/main/java/io/quarkus/qute/ReflectionValueResolver.java#L176">https://github.com/quarkusio/quarkus/blob/ce19ff75e9f732ff731bb30c2141b44b42c66050/independent-projects/qute/core/src/main/java/io/quarkus/qute/ReflectionValueResolver.java#L176</a>
	 */
	private static InvalidMethodReason getValidMethodForQute(PsiMethod method, String typeName) {
		if (JAVA_LANG_OBJECT.equals(typeName)) {
			return InvalidMethodReason.FromObject;
		}
		try {
			if ("void".equals(method.getReturnType().getCanonicalText(true))) {
				return InvalidMethodReason.VoidReturn;
			}
			if (method.getModifierList().hasExplicitModifier(PsiModifier.STATIC)) {
				return InvalidMethodReason.Static;
			}
		} catch (RuntimeException e) {
			LOGGER.log(Level.SEVERE, "Error while checking if '" + method.getName() + "' is valid.", e);
		}
		return null;
	}

	private static Module getJavaProjectFromProjectUri(String projectName, IPsiUtils utils) {
		if (projectName == null) {
			return null;
		}
		return ModuleManager.getInstance(utils.getProject()).findModuleByName(projectName);
	}

	public static Module getJavaProjectFromTemplateFile(String templateFileUri, IPsiUtils utils) {
		try {
			templateFileUri = templateFileUri.replace("vscode-notebook-cell", "file");
			VirtualFile file = utils.findFile(templateFileUri);
			Module module = utils.getModule(file);
			if (file == null || module == null) {
				// The uri doesn't belong to an Eclipse project
				return null;
			}
			// The uri belong to an Eclipse project
			if (ModuleType.get(module) != JavaModuleType.getModuleType()) {
				// The uri doesn't belong to a Java project
				return null;
			}

			String projectName = module.getName();
			return module;
		} catch (IOException e) {
			return null;
		}
	}

	private static PsiClass[] findImplementedInterfaces(PsiClass type, ProgressIndicator progressMonitor) {
		return type.getInterfaces();
	}


	public static ITypeResolver createTypeResolver(PsiMember member, Module javaProject) {
		/*ITypeResolver typeResolver = !member.isBinary()
				? new CompilationUnitTypeResolver((ICompilationUnit) member.getAncestor(IJavaElement.COMPILATION_UNIT))
				: new ClassFileTypeResolver((IClassFile) member.getAncestor(IJavaElement.CLASS_FILE));*/
		ITypeResolver typeResolver = new ClassFileTypeResolver(member instanceof PsiClass ?
				(PsiClass) member : member.getContainingClass(), javaProject);
		return typeResolver;
	}

	/**
	 * Returns the workspace edit to generate the given java member for the given
	 * type.
	 *
	 * @param params  the parameters needed to resolve the workspace edit
	 * @param utils   the jdt utils
	 * @param monitor the progress monitor
	 * @return the workspace edit to generate the given java member for the given
	 *         type
	 */
	public WorkspaceEdit generateMissingJavaMember(GenerateMissingJavaMemberParams params, IPsiUtils utils,
												   ProgressIndicator monitor) {
		return QuteSupportForTemplateGenerateMissingJavaMemberHandler.handleGenerateMissingJavaMember(params, utils,
				monitor);
	}

	/**
	 * Returns the formatted Javadoc for the member specified in the parameters.
	 *
	 * @param params  the parameters used to specify the member whose documentation
	 *                should be found
	 * @param utils   the JDT utils
	 * @param monitor the progress monitor
	 * @return the formatted Javadoc for the member specified in the parameters
	 */
	public String getJavadoc(QuteJavadocParams params, IPsiUtils utils, ProgressIndicator monitor) {
		try {
			String projectUri = params.getProjectUri();
			Module javaProject = getJavaProjectFromProjectUri(projectUri, utils);
			if (javaProject == null) {
				return null;
			}

			utils = utils.refine(javaProject);

			PsiClass type = getTypeFromParams(params.getSourceType(), params.getProjectUri(), javaProject, monitor);
			if (type == null) {
				return null;
			}
			return getJavadoc(type, params.getDocumentFormat(), params.getMemberName(), params.getSignature(), utils,
					monitor, new HashSet<>());
		} catch (Exception e) {
			LOGGER.log(Level.WARNING,
					"Error while collecting Javadoc for " + params.getSourceType() + "#" + params.getMemberName(), e);
			return null;
		}
	}

	private String getJavadoc(PsiClass type, DocumentFormat documentFormat, String memberName, String signature,
							  IPsiUtils utils, ProgressIndicator monitor, Set<PsiClass> visited) {
		if (visited.contains(type)) {
			return null;
		}
		visited.add(type);
		if (monitor.isCanceled()) {
			throw new ProcessCanceledException();
		}

		ITypeResolver typeResolver = createTypeResolver(type, utils.getModule());

		// 1) Check the fields for the member

		// Standard fields
		PsiField[] fields = type.getFields();
		for (PsiField field : fields) {
			if (isValidField(field, type)
					&& memberName.equals(field.getName())
					&& signature.equals(typeResolver.resolveFieldSignature(field))) {
				String javadoc = utils.getJavadoc(field, documentFormat);
				if (javadoc != null) {
					return javadoc;
				}
			}
		}

		// Record fields
		if (type.isRecord()) {
			for (PsiRecordComponent field : type.getRecordComponents()) {
				// All record components are valid
				if (memberName.equals(field.getName())
						&& signature.equals(typeResolver.resolveFieldSignature(field))) {
					String javadoc = utils.getJavadoc(field, documentFormat);
					if (javadoc != null) {
						return javadoc;
					}
				}
			}
		}

		// 2) Check the methods for the member
		PsiMethod[] methods = type.getMethods();
		for (PsiMethod method : methods) {
			if (isValidMethod(method, type)) {
				try {
					InvalidMethodReason invalid = getValidMethodForQute(method, type.getQualifiedName());
					if (invalid == null && (signature.equals(typeResolver.resolveMethodSignature(method)))) {
						String javadoc = utils.getJavadoc(method, documentFormat);
						if (javadoc != null) {
							return javadoc;
						}
						// otherwise, maybe a supertype has it
					}
				} catch (Exception e) {
					LOGGER.log(Level.WARNING,
							"Error while getting method signature of '" + method.getName() + "'.", e);
				}
			}
		}

		// 3) Check the superclasses for the member

		// Collect type extensions
		List<PsiClass> extendedTypes = null;
		if (type.isInterface()) {
			PsiClass[] interfaces = findImplementedInterfaces(type, monitor);
			if (interfaces != null && interfaces.length > 0) {
				extendedTypes = Arrays.asList(interfaces);
			}
		} else {
			// ex : String implements CharSequence, ....
			PsiClass[] allSuperTypes = type.getSupers();
			extendedTypes = Arrays.asList(allSuperTypes);
		}

		if (extendedTypes != null) {
			for (PsiClass extendedType : extendedTypes) {
				String javadoc = getJavadoc(extendedType, documentFormat, memberName, signature, utils, monitor, visited);
				if (javadoc != null) {
					return javadoc;
				}
			}
		}

		return null;

	}

	private PsiClass getTypeFromParams(String typeName, String projectUri,Module javaProject, ProgressIndicator monitor) {
		PsiClass type = findType(typeName, javaProject, monitor);
		return type;
	}



}

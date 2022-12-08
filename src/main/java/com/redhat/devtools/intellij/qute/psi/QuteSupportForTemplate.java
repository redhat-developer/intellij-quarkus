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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiRecordComponent;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.AbstractTypeResolver;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.ClassFileTypeResolver;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.ITypeResolver;
import com.redhat.devtools.intellij.qute.psi.internal.template.JavaTypesSearch;
import com.redhat.devtools.intellij.qute.psi.internal.template.QuarkusIntegrationForQute;
import com.redhat.devtools.intellij.qute.psi.internal.template.TemplateDataSupport;
import com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils;
import com.redhat.devtools.intellij.qute.psi.utils.QuteReflectionAnnotationUtils;
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

import static com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils.findType;
import static com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils.getFullQualifiedName;

/**
 * Qute support for Template file.
 * 
 * @author Angelo ZERR
 *
 */
public class QuteSupportForTemplate {

	private static final Logger LOGGER = Logger.getLogger(QuteSupportForTemplate.class.getName());

	private static final String JAVA_LANG_ITERABLE = "java.lang.Iterable";

	private static final String JAVA_LANG_OBJECT = "java.lang.Object";

	private static final List<String> COMMONS_ITERABLE_TYPES = Arrays.asList("Iterable", JAVA_LANG_ITERABLE,
			"java.util.List", "java.util.Set");

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

		String sourceType = params.getSourceType();
		PsiClass type = findType(sourceType, javaProject, monitor);
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
		int index = typeName.indexOf('<');
		if (index != -1) {
			// ex : java.util.List<org.acme.Item>
			String iterableClassName = typeName.substring(0, index);
			PsiClass iterableType = findType(iterableClassName, javaProject, monitor);
			if (iterableType == null) {
				return null;
			}

			boolean iterable = isIterable(iterableType, monitor);
			if (!iterable) {
				return null;
			}

			String iterableOf = typeName.substring(index + 1, typeName.length() - 1);
			iterableOf = getFullQualifiedName(iterableOf, javaProject, monitor);
			iterableClassName = iterableType.getQualifiedName();
			typeName = iterableClassName + "<" + iterableOf + ">";
			return createIterableType(typeName, iterableClassName, iterableOf);
		} else if (typeName.endsWith("[]")) {
			// ex : org.acme.Item[]
			String iterableOf = typeName.substring(0, typeName.length() - 2);
			PsiClass iterableOfType = findType(iterableOf, javaProject, monitor);
			if (iterableOfType == null) {
				return null;
			}
			iterableOf = getFullQualifiedName(iterableOf, javaProject, monitor);
			typeName = iterableOf + "[]";
			return createIterableType(typeName, null, iterableOf);
		}

		// ex : org.acme.Item, java.util.List, ...
		PsiClass type = findType(typeName, javaProject, monitor);
		if (type == null) {
			return null;
		}

		ITypeResolver typeResolver = createTypeResolver(type);

		// 1) Collect fields
		List<JavaFieldInfo> fieldsInfo = new ArrayList<>();

		// Standard fields
		PsiField[] fields = type.getFields();
		for (PsiField field : fields) {
			if (isValidField(field)) {
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
			if (isValidMethod(method, type.isInterface())) {
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

		// Collect type extensions
		List<String> extendedTypes = null;
		if (type.isInterface()) {
			PsiClass[] interfaces = findImplementedInterfaces(type, monitor);
			if (interfaces != null && interfaces.length > 0) {
				extendedTypes = Stream.of(interfaces) //
						.map(interfaceType -> interfaceType.getQualifiedName()) //
						.collect(Collectors.toList());
			}
		} else {
			// ex : String implements CharSequence, ....
			PsiClass[] interfaces = findImplementedInterfacesAndSuper(type, monitor);
			if (interfaces != null && interfaces.length > 0) {
				extendedTypes = Stream.of(interfaces) //
						.map(superType -> superType.getQualifiedName()) //
						.collect(Collectors.toList());
			}
		}

		if (extendedTypes != null) {
			extendedTypes.remove(typeName);
		}

		String typeSignature = AbstractTypeResolver.resolveJavaTypeSignature(type);
		if (typeSignature != null) {
			ResolvedJavaTypeInfo resolvedType = new ResolvedJavaTypeInfo();
			resolvedType.setSignature(typeSignature);
			resolvedType.setFields(fieldsInfo);
			resolvedType.setMethods(methodsInfo);
			resolvedType.setInvalidMethods(invalidMethods);
			resolvedType.setExtendedTypes(extendedTypes);
			QuteReflectionAnnotationUtils.collectAnnotations(resolvedType, type, typeResolver, javaProject);
			return resolvedType;
		}
		return null;
	}

	private ResolvedJavaTypeInfo createIterableType(String className, String iterableClassName, String iterableOf) {
		ResolvedJavaTypeInfo resolvedClass = new ResolvedJavaTypeInfo();
		resolvedClass.setSignature(className);
		resolvedClass.setIterableType(iterableClassName);
		resolvedClass.setIterableOf(iterableOf);
		return resolvedClass;
	}

	private static boolean isIterable(PsiClass iterableType, ProgressIndicator monitor) {
		String iterableClassName = iterableType.getQualifiedName();
		// Fast test
		if (COMMONS_ITERABLE_TYPES.contains(iterableClassName)) {
			return true;
		}
		// Check if type implements "java.lang.Iterable"
		PsiClass[] interfaces = findImplementedInterfaces(iterableType, monitor);
		boolean iterable = interfaces == null ? false
				: Stream.of(interfaces)
						.anyMatch(interfaceType -> JAVA_LANG_ITERABLE.equals(interfaceType.getQualifiedName()));
		return iterable;
	}

	private static boolean isValidField(PsiField field) {
		return field.getModifierList().hasExplicitModifier(PsiModifier.PUBLIC);
	}

	private static boolean isValidMethod(PsiMethod method, boolean isInterface) {
		try {
			if (method.isConstructor() || !method.isValid()) {
				return false;
			}
			if (!isInterface && !method.getModifierList().hasExplicitModifier(PsiModifier.PUBLIC)) {
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

	private static void findImplementedInterfaces(PsiClass type, List<PsiClass> result, ProgressIndicator progressMonitor) {
		result.addAll(Arrays.asList(type.getInterfaces()));
		for(PsiClass parent : type.getSupers()) {
			findImplementedInterfaces(parent, result, progressMonitor);
		}
	}

	private static PsiClass[] findImplementedInterfaces(PsiClass type, ProgressIndicator progressMonitor) {
		List<PsiClass> result = new ArrayList<>();
		findImplementedInterfaces(type, result, progressMonitor);
		return result.toArray(new PsiClass[result.size()]);
	}

	private static PsiClass[] findImplementedInterfacesAndSuper(PsiClass type, ProgressIndicator progressMonitor) {
		List<PsiClass> result = new ArrayList<>();
		findImplementedInterfaces(type, result, progressMonitor);
		if (type.getSuperClass() != null) {
			result.add(type.getSuperClass());
		}
		return result.toArray(new PsiClass[result.size()]);
	}

	public static ITypeResolver createTypeResolver(PsiMember member) {
		/*ITypeResolver typeResolver = !member.isBinary()
				? new CompilationUnitTypeResolver((ICompilationUnit) member.getAncestor(IJavaElement.COMPILATION_UNIT))
				: new ClassFileTypeResolver((IClassFile) member.getAncestor(IJavaElement.CLASS_FILE));*/
		ITypeResolver typeResolver = new ClassFileTypeResolver(member.getContainingClass());
		return typeResolver;
	}

}

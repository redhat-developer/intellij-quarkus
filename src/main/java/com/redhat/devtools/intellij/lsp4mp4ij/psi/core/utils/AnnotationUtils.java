/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.util.Ranges;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java annotations utilities.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/utils/AnnotationUtils.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/utils/AnnotationUtils.java</a>
 *
 */
public class AnnotationUtils {

	private static final Logger log = LoggerFactory.getLogger(AnnotationUtils.class);

	/**
	 * Returns checks if the <code>annotatable</code> parameter is annotated with the given annotation.
	 *
	 * @param annotatable the class, field which can be annotated
	 * @param annotationName a non-null FQCN annotation to check against
	 * @return <code>true</code> if the <code>annotatable</code> parameter is annotated with the given annotation, <code>false</code> otherwise.
	 */
	public static boolean hasAnnotation(PsiElement annotatable, String annotationName) {
		return hasAnyAnnotation(annotatable, annotationName);
	}

	/**
	 * Returns checks if the <code>annotatable</code> parameter is annotated with ANY of the given annotations.
	 *
	 * @param annotatable the class, field which can be annotated
	 * @param annotationNames a non-null, non-empty array of FQCN annotations to check against
	 * @return <code>true</code> if the <code>annotatable</code> parameter is annotated with ANY of the given annotations, <code>false</code> otherwise.
	 */
	public static boolean hasAnyAnnotation(PsiElement annotatable, String... annotationNames) {
		return getFirstAnnotation(annotatable, annotationNames) != null;
	}

	/**
	 * Returns an {@link PsiAnnotation} of the first annotation in
	 * <code>annotationNames</code> that appears on the given annotatable.
	 *
	 * It returns the first in the <code>annotationNames</code> list, <b>not</b> the
	 * first in the order that the annotations appear on the annotatable. <br /> <br />
	 * e.g.
	 *
	 * <pre>
	 * &commat;Singleton &commat;Deprecated String myString;
	 * </pre>
	 *
	 * when given the <code>annotationNames</code> list <code>{"Potato", "Deprecated",
	 * "Singleton"}</code> will return the IAnnotation for <code>&commat;Deprecated</code>.
	 *
	 * @param annotatable     the annotatable to check for the annotations
	 * @param annotationNames the FQNs of the annotations to check for
	 * @return an {@link PsiAnnotation} of the first annotation in
	 * <code>annotationNames</code> that appears on the given annotatable
	 */
	public static PsiAnnotation getFirstAnnotation(PsiElement annotatable, String... annotationNames) {
		if (annotatable instanceof PsiAnnotationOwner) {
			return getFirstAnnotation(((PsiAnnotationOwner) annotatable).getAnnotations(), annotationNames);
		} else if (annotatable instanceof PsiModifierListOwner) {
			return getFirstAnnotation(((PsiModifierListOwner) annotatable).getAnnotations(), annotationNames);
		}
		return null;
	}

	private static PsiAnnotation getFirstAnnotation(PsiAnnotation[] annotations, String...annotationNames) {
		if (annotations == null || annotations.length == 0 || annotationNames == null || annotationNames.length == 0) {
			return null;
		}
		for (PsiAnnotation annotation : annotations) {
			for (String annotationName: annotationNames) {
				if (isMatchAnnotation(annotation, annotationName)) {
					return annotation;
				}
			}
		}
		return null;
	}

	public static List<PsiAnnotation> getAllAnnotations(PsiElement annotatable, String... annotationNames) {
		List<PsiAnnotation> all = new ArrayList<>();
		if (annotatable instanceof PsiAnnotationOwner) {
			collectAnnotations(((PsiAnnotationOwner) annotatable).getAnnotations(), all, annotationNames);
		} else if (annotatable instanceof PsiModifierListOwner) {
			collectAnnotations(((PsiModifierListOwner) annotatable).getAnnotations(), all, annotationNames);
		}
		return all;
	}

	private static void collectAnnotations(PsiAnnotation[] annotations, List<PsiAnnotation> all, String...annotationNames) {
		if (annotations == null || annotations.length == 0 || annotationNames == null || annotationNames.length == 0) {
			return;
		}
		for (PsiAnnotation annotation : annotations) {
			for (String annotationName: annotationNames) {
				if (isMatchAnnotation(annotation, annotationName)) {
					all.add(annotation);
				}
			}
		}
	}
	/**
	 * Returns the annotation from the given <code>annotatable</code> element with
	 * the given name <code>annotationName</code> and null otherwise.
	 *
	 * @param annotatable    the class, field which can be annotated.
	 * @param annotationName the annotation name
	 * @return the annotation from the given <code>annotatable</code> element with
	 *         the given name <code>annotationName</code> and null otherwise.
	 */
	public static PsiAnnotation getAnnotation(PsiElement annotatable, String annotationName) {
		if (annotatable instanceof PsiAnnotationOwner) {
			return getAnnotation(annotationName, ((PsiAnnotationOwner) annotatable).getAnnotations());
		} else if (annotatable instanceof PsiModifierListOwner) {
			return getAnnotation(annotationName, ((PsiModifierListOwner) annotatable).getAnnotations());
		}
		return null;
	}

	@Nullable
	private static PsiAnnotation getAnnotation(String annotationName, PsiAnnotation[] annotations) {
		for (PsiAnnotation annotation : annotations) {
			if (isMatchAnnotation(annotation, annotationName)) {
				return annotation;
			}
		}
		return null;
	}

	/**
	 * Returns true if the given annotation match the given annotation name and
	 * false otherwise.
	 *
	 * @param annotation     the annotation.
	 * @param annotationName the annotation name.
	 * @return true if the given annotation match the given annotation name and
	 *         false otherwise.
	 */
	public static boolean isMatchAnnotation(PsiAnnotation annotation, String annotationName) {
		if(annotation == null || annotation.getQualifiedName() == null){
			return false;
		}
		return annotationName.endsWith(annotation.getQualifiedName());
	}

	/**
	 * Returns the value of the given member name of the given annotation.
	 *
	 * @param annotation the annotation.
	 * @param memberName the member name.
	 * @return the value of the given member name of the given annotation.
	 */
	public static String getAnnotationMemberValue(PsiAnnotation annotation, String memberName) {
		PsiElement member = getAnnotationMemberValueExpression(annotation, memberName);
		if (member == null) {
			return null;
		}
		if (member instanceof PsiEnumConstant) {
			// ex : @ConfigRoot(phase = BUILD_AND_RUN_TIME_FIXED)
			// returns BUILD_AND_RUN_TIME_FIXED
			return ((PsiEnumConstant) member).getName();
		}
		if (member instanceof PsiReference reference) {
			// ex: @Path(MY_CONSTANTS) where MY_CONSTANTS is a Java field.
			member = reference.resolve();
		}
		if (member instanceof PsiEnumConstant) {
			// ex : @ConfigRoot(phase = io.quarkus.runtime.annotations.ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
			// returns BUILD_AND_RUN_TIME_FIXED
			return ((PsiEnumConstant) member).getName();
		}
		if (member instanceof PsiField field) {
			// ex: private static final String MY_CONSTANTS = "foo";
			member = field.getInitializer();
		}
		if (member == null) {
			return null;
		}
		String value = null;
		if (member instanceof PsiLiteralExpression literalExpression) {
			// ex : @Path("foo") --> foo
			value = literalExpression.getText();
		} else {
			value = member.getText();
		}
		if (value == null) {
			return null;
		}
		// Remove double quote if needed.
		if (value.length() > 1 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
			value = value.substring(1, value.length() - 1);
		}
		return value;
	}

	public static PsiAnnotationMemberValue getAnnotationMemberValueExpression(PsiAnnotation annotation, String memberName) {
		return annotation.findDeclaredAttributeValue(memberName);
	}

	/**
	 * Retrieve the value and range of an annotation member given a supported list
	 * of annotation members
	 *
	 * @param annotation            the annotation of the retrieved members
	 * @param annotationMemberNames the supported members of the annotation
	 * @param position              the hover position
	 * @param typeRoot              the java type root
	 * @param utils                 the utility to retrieve the member range
	 *
	 * @return an AnnotationMemberInfo object if the member exists, null otherwise
	 */
	public static AnnotationMemberInfo getAnnotationMemberAt(PsiAnnotation annotation, String[] annotationMemberNames,
															 Position position, PsiFile typeRoot, IPsiUtils utils) {
		String annotationSource = annotation.getText();
		TextRange r = annotation.getTextRange();
		String annotationMemberValue = null;
		for (String annotationMemberName : annotationMemberNames) {
			annotationMemberValue = getAnnotationMemberValue(annotation, annotationMemberName);
			if (annotationMemberValue != null) {
				// A regex is used to match the member and member value to find the position
				Pattern memberPattern = Pattern.compile(".*[^\"]\\s*(" + annotationMemberName + ")\\s*=.*",
						Pattern.DOTALL);
				Matcher match = memberPattern.matcher(annotationSource);
				if (match.matches()) {
					int offset = annotationSource.indexOf(annotationMemberValue, match.end(1));
					Range range = utils.toRange(typeRoot, r.getStartOffset() + offset, annotationMemberValue.length());

					if (!position.equals(range.getEnd()) && Ranges.containsPosition(range, position)) {
						return new AnnotationMemberInfo(annotationMemberValue, range);
					}
				}
			}
		}

		return null;

	}

}

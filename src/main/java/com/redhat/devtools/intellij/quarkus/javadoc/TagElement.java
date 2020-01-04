/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 * IBM Corporation - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.javadoc;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class TagElement {

    /**
     * Standard doc tag name (value {@value}).
     */
    public static final String TAG_AUTHOR = "@author"; //$NON-NLS-1$

    /**
     * Standard inline doc tag name (value {@value}).
     * <p>
     * Note that this tag first appeared in J2SE 5.
     * </p>
     * @since 3.1
     */
    public static final String TAG_CODE = "@code"; //$NON-NLS-1$

    /**
     * Standard doc tag name (value {@value}).
     */
    public static final String TAG_DEPRECATED = "@deprecated"; //$NON-NLS-1$

    /**
     * Standard inline doc tag name (value {@value}).
     */
    public static final String TAG_DOCROOT = "@docRoot"; //$NON-NLS-1$

    /**
     * Standard doc tag name (value {@value}).
     */
    public static final String TAG_EXCEPTION = "@exception"; //$NON-NLS-1$

    /**
     * Standard doc tag name (value {@value}).
     * @since 3.18
     */
    public static final String TAG_HIDDEN = "@hidden"; //$NON-NLS-1$

    /**
     * Standard doc tag name (value {@value}).
     * @since 3.18
     */
    public static final String TAG_INDEX = "@index"; //$NON-NLS-1$

    /**
     * Standard inline doc tag name (value {@value}).
     */
    public static final String TAG_INHERITDOC = "@inheritDoc"; //$NON-NLS-1$

    /**
     * Standard inline doc tag name (value {@value}).
     */
    public static final String TAG_LINK = "@link"; //$NON-NLS-1$

    /**
     * Standard inline doc tag name (value {@value}).
     */
    public static final String TAG_LINKPLAIN = "@linkplain"; //$NON-NLS-1$

    /**
     * Standard inline doc tag name (value {@value}).
     * <p>
     * Note that this tag first appeared in J2SE 5.
     * </p>
     * @since 3.1
     */
    public static final String TAG_LITERAL = "@literal"; //$NON-NLS-1$

    /**
     * Standard doc tag name (value {@value}).
     */
    public static final String TAG_PARAM = "@param"; //$NON-NLS-1$

    /**
     * Standard doc tag name (value {@value}).
     * @since 3.18
     */
    public static final String TAG_PROVIDES = "@provides"; //$NON-NLS-1$

    /**
     * Standard doc tag name (value {@value}).
     */
    public static final String TAG_RETURN = "@return"; //$NON-NLS-1$

    /**
     * Standard doc tag name (value {@value}).
     */
    public static final String TAG_SEE = "@see"; //$NON-NLS-1$

    /**
     * Standard doc tag name (value {@value}).
     */
    public static final String TAG_SERIAL = "@serial"; //$NON-NLS-1$

    /**
     * Standard doc tag name (value {@value}).
     */
    public static final String TAG_SERIALDATA= "@serialData"; //$NON-NLS-1$

    /**
     * Standard doc tag name (value {@value}).
     */
    public static final String TAG_SERIALFIELD= "@serialField"; //$NON-NLS-1$

    /**
     * Standard doc tag name (value {@value}).
     */
    public static final String TAG_SINCE = "@since"; //$NON-NLS-1$

    /**
     * Standard doc tag name (value {@value}).
     * @since 3.18
     */
    public static final String TAG_SUMMARY = "@summary"; //$NON-NLS-1$

    /**
     * Standard doc tag name (value {@value}).
     */
    public static final String TAG_THROWS = "@throws"; //$NON-NLS-1$

    /**
     * Standard doc tag name (value {@value}).
     * @since 3.18
     */
    public static final String TAG_USES = "@uses"; //$NON-NLS-1$

    /**
     * Standard inline doc tag name (value {@value}).
     */
    public static final String TAG_VALUE= "@value"; //$NON-NLS-1$

    /**
     * Standard doc tag name (value {@value}).
     */
    public static final String TAG_VERSION = "@version"; //$NON-NLS-1$

    /**
     * Javadoc tag name (value {@value}).
     * @since 3.18
     */
    public static final String TAG_API_NOTE = "@apiNote"; //$NON-NLS-1$

    /**
     * Javadoc tag name (value {@value}).
     * @since 3.18
     */
    public static final String TAG_IMPL_SPEC = "@implSpec"; //$NON-NLS-1$

    /**
     * Javadoc tag name (value {@value}).
     * @since 3.18
     */
    public static final String TAG_IMPL_NOTE = "@implNote"; //$NON-NLS-1$

}

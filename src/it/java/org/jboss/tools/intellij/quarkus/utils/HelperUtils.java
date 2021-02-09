/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.intellij.quarkus.utils;

import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Static helper utilities that assist and simplify data conversion and transformation
 *
 * @author zcervink@redhat.com
 */
public class HelperUtils {

    public static String listOfRemoteTextToString(List<RemoteText> data) {
        List<String> listOfStrings = data
                .stream()
                .map(RemoteText::getText)
                .collect(Collectors.toList());

        String concatString = String.join("", listOfStrings);

        return concatString;
    }
}
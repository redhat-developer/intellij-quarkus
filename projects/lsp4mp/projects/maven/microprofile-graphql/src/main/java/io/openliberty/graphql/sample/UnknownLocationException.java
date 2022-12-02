/******************************************************************************
* Copyright (c) 2019 IBM Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html

* Contributors:
* IBM Corporation - initial API and implementation
******************************************************************************/

package io.openliberty.graphql.sample;

public class UnknownLocationException extends Exception {
    private static final long serialVersionUID = 10067834876L;

    public UnknownLocationException(String location) {
        super(location);
    }
}

/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.projectWizard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QuarkusStream {
    public static record JavaCompatibility(String[] versions, String recommended) {
    }

    @JsonProperty("key")
    private String key;

    @JsonProperty("quarkusCoreVersion")
    private String version;

    @JsonProperty("recommended")
    private boolean recommended;

    @JsonProperty("status")
    private String status;

    @JsonProperty("javaCompatibility")
    private JavaCompatibility javaCompatibility;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPlatformKey() {
        String key = getKey();
        return key.substring(0, key.indexOf(':'));
    }

    public String getPlatformVersion() {
        String key = getKey();
        return key.substring(key.indexOf(':') + 1);
    }

    public JavaCompatibility getJavaCompatibility() {
        return javaCompatibility;
    }

    public void setJavaCompatibility(JavaCompatibility javaCompatibility) {
        this.javaCompatibility = javaCompatibility;
    }
}

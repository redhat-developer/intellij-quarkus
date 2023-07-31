/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat, Inc.
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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QuarkusExtension {
    @JsonProperty("category")
    private String category;

    @JsonProperty("description")
    private String description;

    @JsonProperty("id")
    private String id;

    @JsonProperty("labels")
    private List<String> labels = new ArrayList<>();

    @JsonProperty("name")
    private String name;

    @JsonProperty("shortName")
    private String shortName;

    @JsonProperty("order")
    private int order;

    @JsonProperty("status")
    private String status;

    @JsonProperty("tags")
    private List<String> tags = new ArrayList<String>();

    @JsonProperty("guide")
    private String guide;

    @JsonProperty("default")
    private boolean defaultExtension;

    @JsonProperty("shortId")
    private String  shortId;

    private boolean selected;

    @JsonProperty("providesExampleCode")
    private boolean providesExampleCode;

    @JsonProperty("platform")
    private boolean platform;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isDefaultExtension() {
        return defaultExtension;
    }

    public void setDefaultExtension(boolean defaultExtension) {
        this.defaultExtension = defaultExtension;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getGuide() {
        return guide;
    }

    public void setGuide(String guide) {
        this.guide = guide;
    }

    public String getShortId() {
        return shortId;
    }

    public void setShortId(String shortId) {
        this.shortId = shortId;
    }

    public boolean isProvidesExampleCode() {
        return providesExampleCode;
    }

    public void setProvidesExampleCode(boolean providesExampleCode) {
        this.providesExampleCode = providesExampleCode;
    }

    public boolean isPlatform() {
        return platform;
    }

    public void setPlatform(boolean platform) {
        this.platform = platform;
    }

    public String asLabel(boolean withName) {
        StringBuilder builder = new StringBuilder(withName?getName() + "":"");
        List<String> tags = getTags();
        if (!tags.isEmpty()) {
            if (withName) {
                builder.append(' ');
            }
            String labels = tags.stream().
                    filter(tag -> !"provides-example".equals(tag)).
                    collect(Collectors.joining(","));
            if (StringUtils.isNotBlank(labels)) {
                builder.append('[').append(labels).append(']');
            }
        } else {
            String st = getStatus();
            if (StringUtils.isNotBlank(st) && !"stable".equalsIgnoreCase(st)) {
                if (withName) {
                    builder.append(' ');
                }
                builder.append("[").append(st).append(']');
            }
        }
        return builder.toString();
    }

    public String asLabel() {
        return asLabel(true);
    }
}

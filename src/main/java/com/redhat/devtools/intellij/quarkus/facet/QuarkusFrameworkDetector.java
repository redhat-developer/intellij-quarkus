package com.redhat.devtools.intellij.quarkus.facet;

import com.intellij.facet.FacetType;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.framework.detection.FileContentPattern;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.patterns.ElementPattern;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;

public class QuarkusFrameworkDetector extends FacetBasedFrameworkDetector<QuarkusFacet, QuarkusFacetConfiguration> {

    public QuarkusFrameworkDetector() {
        super("quarkus", 1);
    }

    @NotNull
    @Override
    public FacetType<QuarkusFacet, QuarkusFacetConfiguration> getFacetType() {
        return FacetType.findInstance(QuarkusFacetType.class);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return StdFileTypes.XML;
    }

    @NotNull
    @Override
    public ElementPattern<FileContent> createSuitableFilePattern() {
        return FileContentPattern.fileContent().withName("DUMMY_WILL_NEVER_DETECT");
    }
}

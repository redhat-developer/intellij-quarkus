package com.redhat.devtools.intellij.lsp4ij.ui.components;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.profile.codeInspection.ui.ErrorsConfigurable;
import com.intellij.ui.HyperlinkLabel;
import com.redhat.devtools.intellij.lsp4mp4ij.MicroProfileBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;

/**
 * HyperlinkLabel that opens an inspection settings page
 */
public class InspectionHyperlink extends HyperlinkLabel {

    /**
     * Creates a new hyperlink to an inspection settings page
     * @param label the hyperlink label
     * @param inspectionGroupPath the group path to open in the inspections settings
     */
    public InspectionHyperlink(@NotNull @NlsContexts.LinkLabel String label, @NotNull String inspectionGroupPath) {
        super(label);
        addHyperlinkListener(e -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                @NotNull DataContext dataContext = DataManager.getInstance().getDataContext(this);
                @Nullable Settings settings = Settings.KEY.getData(dataContext);
                if (settings != null) {
                    @Nullable ErrorsConfigurable inspections = settings.find(ErrorsConfigurable.class);
                    if (inspections != null) {
                        settings.select(inspections).doWhenDone(new Runnable() {
                            @Override
                            public void run() {
                                inspections.selectInspectionGroup(new String[]{inspectionGroupPath});
                            }
                        });
                    }
                }
            }
        });
    }
}

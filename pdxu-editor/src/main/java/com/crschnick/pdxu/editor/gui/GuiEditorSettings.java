package com.crschnick.pdxu.editor.gui;

import com.crschnick.pdxu.app.core.settings.SettingsIO;
import com.crschnick.pdxu.app.gui.dialog.GuiDialogHelper;
import com.crschnick.pdxu.app.gui.dialog.GuiSettingsComponents;
import com.crschnick.pdxu.app.lang.PdxuI18n;
import com.crschnick.pdxu.editor.EditorSettings;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class GuiEditorSettings {

    public static void showEditorSettings() {
        Alert alert = GuiDialogHelper.createEmptyAlert();
        alert.getButtonTypes().add(ButtonType.APPLY);
        alert.getButtonTypes().add(ButtonType.CANCEL);
        alert.setTitle(PdxuI18n.get("EDITOR_SETTINGS"));

        EditorSettings s = EditorSettings.getInstance();
        Set<Runnable> applyFuncs = new HashSet<>();
        VBox vbox = new VBox(
                GuiSettingsComponents.section("GENERAL", applyFuncs,
                        s.externalEditor,
                        s.indentation,
                        s.warnOnTypeChange,
                        s.maxTooltipLines),
                new Separator(),
                GuiSettingsComponents.section("PERFORMANCE", applyFuncs,
                        s.enableNodeTags,
                        s.enableNodeJumps,
                        s.pageSize));
        vbox.setSpacing(10);
        var sp = new ScrollPane(vbox);
        sp.setFitToWidth(true);
        alert.getDialogPane().setContent(sp);
        sp.setPrefWidth(650);
        sp.setPrefHeight(600);
        vbox.getStyleClass().add("settings-content");
        sp.getStyleClass().add("settings-container");

        Optional<ButtonType> r = alert.showAndWait();
        if (r.isPresent() && r.get().equals(ButtonType.APPLY)) {
            applyFuncs.forEach(ru -> ru.run());
            SettingsIO.save(EditorSettings.getInstance());
        }
    }
}

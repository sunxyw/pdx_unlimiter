package com.crschnick.pdxu.editor.gui;

import com.crschnick.pdxu.app.PdxuApp;
import com.crschnick.pdxu.app.core.ErrorHandler;
import com.crschnick.pdxu.app.gui.GuiStyle;
import com.crschnick.pdxu.app.gui.GuiTooltips;
import com.crschnick.pdxu.app.gui.dialog.GuiDialogHelper;
import com.crschnick.pdxu.editor.EditorFilter;
import com.crschnick.pdxu.editor.EditorSettings;
import com.crschnick.pdxu.editor.EditorState;
import com.crschnick.pdxu.editor.adapter.EditorSavegameAdapter;
import com.crschnick.pdxu.editor.node.EditorNode;
import com.crschnick.pdxu.editor.node.EditorRealNode;
import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.SepiaTone;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Objects;

public class GuiEditor {

    public static Stage createStage(EditorState state) {
        Stage stage = new Stage();

        var icon = PdxuApp.getApp().getIcon();
        stage.getIcons().add(icon);
        var title = state.getFileName() + " - " + "Pdx-U Editor";
        stage.setTitle(title);
        state.dirtyProperty().addListener((c, o, n) -> {
            Platform.runLater(() -> stage.setTitle((n ? "*" : "") + title));
        });

        stage.setScene(new Scene(GuiEditor.create(state), 720, 600));
        GuiStyle.addStylesheets(stage.getScene());
        showMissingGameWarning(state);
        stage.show();
        return stage;
    }

    private static void showMissingGameWarning(EditorState state) {
        if (!state.isContextGameEnabled()) {
            GuiDialogHelper.showBlockingAlert(alert -> {
                alert.setAlertType(Alert.AlertType.WARNING);
                alert.getDialogPane().setMaxWidth(500);
                alert.setTitle("Missing game installation");
                alert.setHeaderText("No installation has been set for " + state.getFileContext().getGame().getInstallationName() +
                        ". You can still use the editor to edit " + state.getFileContext().getGame().getInstallationName() + " savegames, but many useful features will be disabled until a valid game installation has been set in the settings menu.");
            });
        }
    }

    private static Region create(EditorState state) {
        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("editor");
        var v = new VBox();
        v.setFillWidth(true);
        v.setPadding(new Insets(20, 20, 20, 20));
        v.getStyleClass().add("editor-nav-bar-container");
        v.getChildren().add(GuiEditorNavBar.createNavigationBar(state));
        var topBars = new VBox(
                GuiEditorMenuBar.createMenuBar(state),
                v);
        topBars.setFillWidth(true);
        layout.setTop(topBars);
        createNodeList(layout, state);
        layout.setBottom(createFilterBar(state.getFilter()));

        // Disable focus on startup
        layout.requestFocus();

        return layout;
    }

    private static void createNodeList(BorderPane pane, EditorState edState) {
        edState.getContent().shownNodesProperty().addListener((c, o, n) -> {
            Platform.runLater(() -> {
                var grid = createNodeList(edState, n);
                ScrollPane sp = new ScrollPane(grid);
                sp.sceneProperty().addListener((ch, ol, ne) -> {
                    if (ne == null) {
                        return;
                    }

                    if (edState.getNavigation().getCurrent().path().getPath().size() > 0) {
                        sp.setVvalue(edState.getContent().getScroll());
                        sp.vvalueProperty().addListener((sc, so, sn) -> {
                            edState.getContent().changeScroll(sn.doubleValue());
                        });
                    }
                });
                sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
                grid.prefWidthProperty().bind(sp.widthProperty());
                pane.setCenter(sp);
            });
        });
    }

    private static StackPane createGridElement(Node child, int row) {
        var s = new StackPane();
        s.getStyleClass().add("pane");
        if (row % 2 != 0) {
            s.getStyleClass().add("odd");
        }
        s.setAlignment(Pos.CENTER);
        if (child != null) {
            s.getChildren().add(child);
        }
        return s;
    }

    private static GridPane createNodeList(EditorState state, List<EditorNode> nodes) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add(GuiStyle.CLASS_EDITOR_GRID);
        var cc = new ColumnConstraints();
        cc.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(
                new ColumnConstraints(), new ColumnConstraints(), new ColumnConstraints(), new ColumnConstraints(), cc,
                new ColumnConstraints(), new ColumnConstraints());

        int offset = 0;
        if (state.getContent().canGoToPreviousPage()) {
            Button next = new JFXButton("Go to previous page (" + (state.getContent().getPage()) + ")");
            next.setOnAction(e -> {
                state.getContent().previousPage();
            });
            HBox box = new HBox(new FontIcon("mdi-arrow-up"), next, new FontIcon("mdi-arrow-up"));
            box.setAlignment(Pos.CENTER);
            grid.add(createGridElement(box, 0), 0, 0, 5, 1);
            offset = 1;
        }

        int nodeCount = Math.min(nodes.size(), EditorSettings.getInstance().pageSize.getValue());
        for (int i = offset; i < nodeCount + offset; i++) {
            var n = nodes.get(i - offset);
            var kn = createGridElement(new Label(n.getNavigationName()), i);
            kn.setAlignment(Pos.CENTER_LEFT);

            grid.add(createGridElement(GuiEditorTypes.createTypeNode(n), i), 0, i);
            grid.add(kn, 1, i);
            grid.add(createGridElement(new Label("="), i), 2, i);

            Region valueDisplay = GuiEditorNode.createValueDisplay(n, state);
            Node tag = null;
            if (n.isReal() && EditorSettings.getInstance().enableNodeTags.getValue()) {
                try {
                    tag = EditorSavegameAdapter.ALL.get(state.getFileContext().getGame())
                            .createNodeTag(state, (EditorRealNode) n, valueDisplay);
                } catch (Exception ex) {
                    ErrorHandler.handleException(ex);
                }
            }
            grid.add(createGridElement(Objects.requireNonNullElseGet(tag, Region::new), i), 3, i);
            grid.add(createGridElement(valueDisplay, i), 4, i);

            HBox actions = new HBox();
            actions.setFillHeight(true);
            actions.setAlignment(Pos.CENTER_RIGHT);
            if (n.isReal()) {
                if (EditorSettings.getInstance().enableNodeJumps.getValue()) {
                    try {
                    var pointer = EditorSavegameAdapter.ALL.get(state.getFileContext().getGame())
                            .createNodeJump(state, (EditorRealNode) n);
                        if (pointer != null) {
                            var b = new JFXButton();
                            b.setGraphic(new FontIcon());
                            b.getStyleClass().add("jump-to-def-button");
                            GuiTooltips.install(b, "Jump to " + pointer);
                            b.setOnAction(e -> state.getNavigation().navigateTo(pointer));
                            actions.getChildren().add(b);
                            b.prefHeightProperty().bind(actions.heightProperty());
                        }
                    } catch (Exception ex) {
                        ErrorHandler.handleException(ex);
                    }
                }

                Button edit = new JFXButton();
                edit.setGraphic(new FontIcon());
                edit.getStyleClass().add(GuiStyle.CLASS_EDIT);
                edit.setOnAction(e -> {
                    state.getExternalState().startEdit(state, (EditorRealNode) n);
                });
                GuiTooltips.install(edit, "Open in external text editor");
                actions.getChildren().add(edit);
                edit.prefHeightProperty().bind(actions.heightProperty());
            }
            grid.add(createGridElement(actions, i), 5, i);

            grid.add(createGridElement(new Region(), i), 6, i);
        }

        if (state.getContent().canGoToNextPage()) {
            Button next = new JFXButton("Go to next page (" + (state.getContent().getPage() + 2) + ")");
            next.setOnAction(e -> {
                state.getContent().nextPage();
            });
            HBox box = new HBox(new FontIcon("mdi-arrow-down"), next, new FontIcon("mdi-arrow-down"));
            box.setAlignment(Pos.CENTER);
            grid.add(createGridElement(box, nodeCount + offset), 0, nodeCount + offset, 6, 1);
        }

        return grid;
    }

    private static Region createFilterBar(EditorFilter edFilter) {
        HBox box = new HBox();
        box.getStyleClass().add(GuiStyle.CLASS_EDITOR_FILTER);
        box.setSpacing(8);

        {
            ToggleButton filterKeys = new ToggleButton();
            filterKeys.getStyleClass().add(GuiStyle.CLASS_KEY);
            filterKeys.setGraphic(new FontIcon());
            filterKeys.selectedProperty().bindBidirectional(edFilter.filterKeysProperty());
            GuiTooltips.install(filterKeys, "Include keys in search");
            box.getChildren().add(filterKeys);
        }

        {
            ToggleButton filterValues = new ToggleButton();
            filterValues.getStyleClass().add(GuiStyle.CLASS_VALUE);
            filterValues.setGraphic(new FontIcon());
            filterValues.selectedProperty().bindBidirectional(edFilter.filterValuesProperty());
            edFilter.filterValuesProperty().addListener((c, o, n) -> {
                filterValues.setSelected(n);
            });
            GuiTooltips.install(filterValues, "Include values in search");
            box.getChildren().add(filterValues);
        }

        box.getChildren().add(new Separator(Orientation.VERTICAL));

        HBox textBar = new HBox();
        {
            TextField filter = new TextField();
            {
                filter.focusedProperty().addListener((c, o, n) -> {
                    if (n) {
                        Platform.runLater(filter::selectAll);
                    }
                });
                filter.textProperty().addListener((c, o, n) -> {
                    filter.setEffect(new SepiaTone(0.7));
                });
                filter.setOnAction(e -> {
                    edFilter.filterStringProperty().set(filter.getText());
                    filter.setEffect(null);
                });
                textBar.getChildren().add(filter);
            }

            {
                Button search = new Button();
                search.setOnAction(e -> {
                    edFilter.filterStringProperty().set(filter.getText());
                    filter.setEffect(null);
                });
                search.setGraphic(new FontIcon());
                search.getStyleClass().add(GuiStyle.CLASS_FILTER);
                textBar.getChildren().add(search);
            }

            {
                Button clear = new Button();
                clear.setOnAction(e -> {
                    filter.setText("");
                    edFilter.filterStringProperty().set("");
                    filter.setEffect(null);
                });
                clear.setGraphic(new FontIcon());
                clear.getStyleClass().add(GuiStyle.CLASS_CLEAR);
                textBar.getChildren().add(clear);
            }
        }
        box.getChildren().add(textBar);

        box.getChildren().add(new Separator(Orientation.VERTICAL));

        {
            ToggleButton cs = new ToggleButton();
            cs.getStyleClass().add(GuiStyle.CLASS_CASE_SENSITIVE);
            cs.setGraphic(new FontIcon());
            cs.selectedProperty().bindBidirectional(edFilter.caseSensitiveProperty());
            GuiTooltips.install(cs, "Case sensitive");
            box.getChildren().add(cs);
        }

        Region spacer = new Region();
        box.getChildren().add(spacer);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        {
            Button filterDisplay = new JFXButton();
            edFilter.filterStringProperty().addListener((c, o, n) -> {
                Platform.runLater(() -> filterDisplay.setText(
                        n.equals("") ? "" : "Showing results for \"" + n + "\""));
            });
            filterDisplay.setAlignment(Pos.CENTER);
            box.getChildren().add(filterDisplay);
        }

        return box;
    }
}

package com.crschnick.pdxu.editor;

import com.crschnick.pdxu.editor.node.EditorNode;
import com.crschnick.pdxu.io.node.NodeMatcher;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.List;
import java.util.stream.Collectors;

public class EditorFilter {

    private final StringProperty filterString;
    private final BooleanProperty caseSensitive;
    private BooleanProperty filterKeys;
    private BooleanProperty filterValues;

    EditorFilter(EditorState state) {
        filterString = new SimpleStringProperty("");
        filterString.addListener((c, o, n) -> {
            if (filterKeys.get() || filterValues.get()) {
                state.onFilterChange();
            }
        });
        caseSensitive = new SimpleBooleanProperty();
        caseSensitive.addListener((c, o, n) -> state.onFilterChange());
        filterKeys = new SimpleBooleanProperty(true);
        filterKeys.addListener((c, o, n) -> state.onFilterChange());
        filterValues = new SimpleBooleanProperty(true);
        filterValues.addListener((c, o, n) -> state.onFilterChange());
    }

    private boolean contains(String s) {
        if (caseSensitive.get()) {
            return s.contains(filterString.get());
        } else {
            return s.toLowerCase().contains(filterString.get().toLowerCase());
        }
    }

    public List<EditorNode> filter(List<EditorNode> input) {
        var matcher = caseSensitive.get() ?
                new NodeMatcher.CaseSenstiveMatcher(filterString.get()) :
                new NodeMatcher.CaseInsenstiveMatcher(filterString.get());
        return input.stream().filter(n -> {
            if (!filterKeys.get() && !filterValues.get()) {
                return true;
            }

            if (filterString.get().length() == 0) {
                return true;
            }

            if (filterKeys.get() && n.filterKey(this::contains)) {
                return true;
            } else {
                return filterValues.get() && n.filterValue(matcher);
            }
        }).collect(Collectors.toList());
    }

    public StringProperty filterStringProperty() {
        return filterString;
    }

    public BooleanProperty caseSensitiveProperty() {
        return caseSensitive;
    }

    public BooleanProperty filterKeysProperty() {
        return filterKeys;
    }

    public BooleanProperty filterValuesProperty() {
        return filterValues;
    }
}

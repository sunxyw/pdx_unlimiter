package com.crschnick.pdx_unlimiter.app.editor;

import com.crschnick.pdx_unlimiter.core.parser.ArrayNode;
import com.crschnick.pdx_unlimiter.core.parser.KeyValueNode;
import com.crschnick.pdx_unlimiter.core.parser.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class EditorNode {

    public static List<EditorNode> create(EditorNode parent, List<Node> nodes) {
        int realIndex = 0;
        var result = new ArrayList<EditorNode>();
        for (int i = 0; i < nodes.size(); i++) {
            var n = nodes.get(i);
            if (n instanceof KeyValueNode &&
                    i + 1 < nodes.size() &&
                    nodes.get(i + 1) instanceof KeyValueNode) {
                var k = n.getKeyValueNode().getKeyName();
                int end = i;
                while (end + 1 < nodes.size() &&
                        nodes.get(end + 1) instanceof KeyValueNode &&
                        nodes.get(end + 1).getKeyValueNode().getKeyName().equals(k)) {
                    end++;
                }

                if (end > i) {
                    result.add(new CollectorNode(
                            parent,
                            k,
                            nodes.subList(i, end).stream()
                                    .map(node -> node.getKeyValueNode().getNode())
                                    .collect(Collectors.toList())));
                    i = end;
                    realIndex++;
                    continue;
                }
            }

            result.add(new SimpleNode(
                    parent,
                    n instanceof KeyValueNode ? n.getKeyValueNode().getKeyName() : null,
                    realIndex,
                    n instanceof KeyValueNode ? n.getKeyValueNode().getNode() : n));
            realIndex++;
        }

        return result;
    }

    private EditorNode directParent;
    protected String keyName;

    public EditorNode(EditorNode directParent, String keyName) {
        this.directParent = directParent;
        this.keyName = keyName;
    }

    public abstract String displayKeyName();

    public abstract String navigationName();

    public abstract boolean isReal();

    public abstract SimpleNode getRealParent();

    public abstract List<EditorNode> open();

    public abstract Node toWritableNode();

    public abstract void update(ArrayNode newNode);

    public EditorNode getDirectParent() {
        return directParent;
    }

    public Optional<String> getKeyName() {
        return Optional.ofNullable(keyName);
    }
}

package com.crschnick.pdx_unlimiter.core.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class Node {

    public static List<Node> copyOfArrayNode(Node node) {
        if (!(node instanceof ArrayNode)) {
            throw new NodeFormatException("Not an array node:\n" + node.toString());
        }

        ArrayNode a = (ArrayNode) node;
        return new ArrayList<>(a.getNodes());
    }

    public static void addNodeToArray(Node node, Node toAdd) {
        if (!(node instanceof ArrayNode)) {
            throw new NodeFormatException("Not an array node:\n" + node.toString());
        }

        ArrayNode a = (ArrayNode) node;
        a.addNode(toAdd);
    }

    public static void removeNodeFromArray(Node node, Node toRemove) {
        if (!(node instanceof ArrayNode)) {
            throw new NodeFormatException("Not an array node:\n" + node.toString());
        }

        ArrayNode a = (ArrayNode) node;
        a.removeNode(toRemove);
    }

    public static Optional<Node> getNodeForKeyIfExistent(Node node, String key) {
        var list = getNodesForKey(node, key);
        return list.size() == 0 ? Optional.empty() : Optional.of(list.get(0));
    }

    public static boolean getBoolean(Node node) {
        if (!(node instanceof ValueNode)) {
            throw new NodeFormatException("Not a value node:\n" + node.toString());
        }

        return (Boolean) ((ValueNode) node).getValue();
    }

    public static String getString(Node node) {
        if (!(node instanceof ValueNode)) {
            throw new NodeFormatException("Not a value node:\n" + node.toString());
        }

        return (String) ((ValueNode) node).getValue();
    }

    public static int getInteger(Node node) {
        if (!(node instanceof ValueNode)) {
            throw new NodeFormatException("Not a value node:\n" + node.toString());
        }

        if (((ValueNode) node).getValue() instanceof Long) {
            long v = (long) ((ValueNode) node).getValue();
            if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) {
                return (int) v;
            } else {
                throw new NodeFormatException("Cannot cast long to int");
            }
        }
        return (int) ((ValueNode) node).getValue();
    }

    public static long getLong(Node node) {
        if (!(node instanceof ValueNode)) {
            throw new NodeFormatException("Not a value node:\n" + node.toString());
        }

        if (((ValueNode) node).getValue() instanceof Long) {
            long v = (long) ((ValueNode) node).getValue();
            return v;
        }
        throw new NodeFormatException("Cannot cast to int");
    }

    public static double getDouble(Node node) {
        if (!(node instanceof ValueNode)) {
            throw new NodeFormatException("Not a value node:\n" + node.toString());
        }

        if (((ValueNode) node).getValue() instanceof Double) {
            double v = (double) ((ValueNode) node).getValue();
            return v;
        }

        if (((ValueNode) node).getValue() instanceof Long) {
            long v = (long) ((ValueNode) node).getValue();
            return v;
        }

        throw new NodeFormatException("Not a double value node:\n" + node.toString());
    }


    public static List<Node> getNodeArray(Node node) {
        if (!(node instanceof ArrayNode)) {
            throw new NodeFormatException("Not an array node:\n" + node.toString());
        }

        return ((ArrayNode) node).getNodes();
    }

    public static boolean hasKey(Node node, String key) {
        if (!(node instanceof ArrayNode)) {
            throw new NodeFormatException("Not an array node:\n" + node.toString());
        }

        var list = getNodesForKey(node, key);
        return list.size() > 0;
    }

    public static KeyValueNode getKeyValueNode(Node node) {
        if (!(node instanceof KeyValueNode)) {
            throw new NodeFormatException("Not a key-value node:\n" + node.toString());
        }

        return (KeyValueNode) node;
    }

    public static Node getNodeForKey(Node node, String key) {
        var list = getNodesForKey(node, key);
        if (list.size() > 1) {
            throw new NodeFormatException("Too many entries for key " + key + " for node:\n" + node.toString());
        }
        if (list.size() == 0) {
            throw new NodeFormatException("Invalid key " + key + " for node:\n" + node.toString());
        }
        return list.get(0);
    }

    public static KeyValueNode getKeyValueNodeForKey(Node node, String key) {
        var list = getKeyValueNodesForKey(node, key);
        if (list.size() > 1) {
            throw new NodeFormatException("Too many entries for key " + key + " for node:\n" + node.toString());
        }
        if (list.size() == 0) {
            throw new NodeFormatException("Invalid key " + key + " for node:\n" + node.toString());
        }
        return list.get(0);
    }

    public static List<KeyValueNode> getKeyValueNodesForKey(Node node, String key) {
        if (!(node instanceof ArrayNode)) {
            throw new NodeFormatException("Not an array node:\n" + node.toString());
        }

        List<KeyValueNode> nodes = new ArrayList<>();
        ArrayNode array = (ArrayNode) node;
        for (Node sub : array.getNodes()) {
            if (sub instanceof KeyValueNode) {
                KeyValueNode kvNode = (KeyValueNode) sub;
                if (key.equals("*") || kvNode.getKeyName().equals(key)) {
                    nodes.add(kvNode);
                }
            }
        }

        return nodes;
    }

    public static List<Node> getNodesForKey(Node node, String key) {
        List<Node> nodes = new ArrayList<>();
        if (!(node instanceof ArrayNode)) {
            throw new NodeFormatException("Not an array node:\n" + node.toString());
        }

        ArrayNode array = (ArrayNode) node;
        for (Node sub : array.getNodes()) {
            if (sub instanceof KeyValueNode) {
                KeyValueNode kvNode = (KeyValueNode) sub;
                if (key.equals("*") || kvNode.getKeyName().equals(key)) {
                    nodes.add(kvNode.getNode());
                }
            }
        }

        return nodes;
    }
}
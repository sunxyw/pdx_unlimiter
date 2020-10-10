package com.crschnick.pdx_unlimiter.eu4.parser;

import com.crschnick.pdx_unlimiter.eu4.format.Namespace;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Eu4IronmanParser extends GamedataParser {

    public static final byte[] EQUALS = new byte[]{1, 0};
    public static final byte[] OPEN_GROUP = new byte[]{3, 0};
    public static final byte[] CLOSE_GROUP = new byte[]{4, 0};
    public static final byte[] INTEGER = new byte[]{0x0c, 0};
    public static final byte[] INTEGER_UNSIGNED = new byte[]{0x14, 0};
    public static final byte[] STRING_1 = new byte[]{0x0F, 0};
    public static final byte[] STRING_2 = new byte[]{0x17, 0};
    public static final byte[] FLOAT = new byte[]{0x0D, 0};
    public static final byte[] DOUBLE = new byte[]{0x67, 0x01};
    public static final byte[] BOOL = new byte[]{0x0e, 0x00};
    public static final byte[] BOOL_TRUE = new byte[]{0x4b, 0x28};
    public static final byte[] BOOL_FALSE = new byte[]{0x4c, 0x28};
    public static final byte[] MAGIC = new byte[]{ 0x45, 0x55, 0x34, 0x62, 0x69, 0x6E};

    public Eu4IronmanParser(Namespace ns) {
        super(MAGIC, ns);
    }

    public void write(Node node, OutputStream out, boolean isRoot) throws IOException {
        if (node instanceof KeyValueNode) {
            ByteBuffer.wrap(((KeyValueNode) node).getKeyName().getBytes()).order(ByteOrder.LITTLE_ENDIAN).getShort();
            out.write(((KeyValueNode) node).getKeyName().getBytes());
            out.write(EQUALS);
            write(((KeyValueNode) node).getNode(), out, false);
        }

        if (node instanceof ArrayNode) {
            ArrayNode a = (ArrayNode) node;
            if (!isRoot) out.write(OPEN_GROUP);
            for (int i = 0; i < a.getNodes().size(); i++) {
                write(a.getNodes().get(i), out, false);
            }
            if (!isRoot) out.write(CLOSE_GROUP);
        }

        if (node instanceof ValueNode) {
            ValueNode v = (ValueNode) node;
            if (v.getValue() instanceof Boolean) {
                out.write((boolean) v.getValue() ? BOOL_TRUE : BOOL_FALSE);
            }
            if (v.getValue() instanceof Integer) {
                out.write(INTEGER);
                out.write(ByteBuffer.allocate(4).putInt((Integer) v.getValue()).order(ByteOrder.LITTLE_ENDIAN).array());
            }

            if (v.getValue() instanceof Float) {
                int i = (int) ((float) v.getValue() * 1000);
                out.write(FLOAT);
                out.write(ByteBuffer.allocate(4).putInt(i).order(ByteOrder.LITTLE_ENDIAN).array());
            }

            if (v.getValue() instanceof Double) {
                int i = (int) ((double) v.getValue() * Math.pow(2, 16));
                out.write(DOUBLE);
                out.write(ByteBuffer.allocate(8).putInt(i).order(ByteOrder.LITTLE_ENDIAN).array());
            }

            if (v.getValue() instanceof String) {
                out.write(STRING_1);
                out.write(((String) v.getValue()).getBytes());
            }
        }
    }

    @Override
    public List<Token> tokenize(InputStream stream) throws IOException {
        List<Token> tokens = new ArrayList<>();
        try {
            do {
                byte[] next = new byte[2];
                int read = stream.readNBytes(next, 0, 2);
                if (read < 2) {
                    break;
                }
                if (Arrays.equals(next, EQUALS)) {
                    tokens.add(new EqualsToken());
                }

                else if (Arrays.equals(next, STRING_1) || Arrays.equals(next, STRING_2)) {
                    byte[] length = new byte[4];
                    stream.readNBytes(length, 0, 2);
                    int lengthInt = ByteBuffer.wrap(length).order(ByteOrder.LITTLE_ENDIAN).getInt();

                    byte[] s = new byte[lengthInt];
                    stream.readNBytes(s, 0, lengthInt);
                    if (lengthInt > 0 && s[lengthInt - 1] == '\n') {
                        lengthInt--;
                    }
                    tokens.add(new ValueToken<String>(new String(s, 0, lengthInt)));
                }

                else if (Arrays.equals(next, INTEGER)) {
                    byte[] number = new byte[4];
                    stream.readNBytes(number, 0, 4);
                    int numberInt  = ByteBuffer.wrap(number).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    tokens.add(new ValueToken<Long>((long) numberInt));
                }

                else if (Arrays.equals(next, INTEGER_UNSIGNED)) {
                    byte[] number = new byte[8];
                    stream.readNBytes(number, 0, 4);
                    long numberInt  = ByteBuffer.wrap(number).order(ByteOrder.LITTLE_ENDIAN).getLong();
                    tokens.add(new ValueToken<Long>(numberInt));
                }

                else if (Arrays.equals(next, BOOL_TRUE)) {
                    tokens.add(new ValueToken<String>("yes"));
                }

                else if (Arrays.equals(next, BOOL_FALSE)) {
                    tokens.add(new ValueToken<String>("no"));
                }

                else if (Arrays.equals(next, BOOL)) {
                    byte[] number = new byte[1];
                    stream.readNBytes(number, 0, 1);
                    boolean b = ByteBuffer.wrap(number).get() != 0;
                    tokens.add(new ValueToken<String>(b ? "yes" : "no"));
                }

                else if (Arrays.equals(next, DOUBLE)) {
                    byte[] number = new byte[8];
                    stream.readNBytes(number, 0, 8);
                    long numberInt  = ByteBuffer.wrap(number).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    double value = (2 * (numberInt * Math.pow(2, -16)));
                    tokens.add(new ValueToken<Double>(value));
                }

                else if (Arrays.equals(next, FLOAT)) {
                    byte[] number = new byte[4];
                    stream.readNBytes(number, 0, 4);
                    int numberInt  = ByteBuffer.wrap(number).order(ByteOrder.LITTLE_ENDIAN).getInt();

                    float f  = ((float) numberInt) / 1000.0F;
                    tokens.add(new ValueToken<Float>(f));
                }

                else if (Arrays.equals(next, OPEN_GROUP)) {
                    tokens.add(new OpenGroupToken());
                }

                else if (Arrays.equals(next, CLOSE_GROUP)) {
                    tokens.add(new CloseGroupToken());
                }
                else {
                    byte[] number = new byte[4];
                    number[0] = next[0];
                    number[1] = next[1];
                    int numberInt  = ByteBuffer.wrap(number).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    String id = Integer.toString(numberInt);
                    tokens.add(new ValueToken<String>(id));
                }

            } while (true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tokens;
    }
}
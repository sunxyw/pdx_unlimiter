package com.crschnick.pdx_unlimiter.app.gui;

import com.crschnick.pdx_unlimiter.app.game.GameInstallation;
import com.crschnick.pdx_unlimiter.app.util.CascadeDirectoryHelper;
import com.crschnick.pdx_unlimiter.app.util.ColorHelper;
import com.crschnick.pdx_unlimiter.core.data.Ck3Tag;
import com.crschnick.pdx_unlimiter.core.savegame.SavegameInfo;
import javafx.scene.image.Image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.function.Function;

public class Ck3TagRenderer {

    private static final int IMG_SIZE = 256;

    private static final int PATTERN_COLOR_1 = 0x00FF0000;
    private static final int PATTERN_COLOR_2 = 0x00FFFF00;
    private static final int PATTERN_COLOR_3 = 0x00FFFFFF;

    private static final int EMBLEM_COLOR_1 = 0x000080;
    private static final int EMBLEM_COLOR_2 = 0x00FF00;
    private static final int EMBLEM_COLOR_3 = 0xFF0080;

    public static Image tagImage(SavegameInfo<Ck3Tag> info, Ck3Tag tag) {
        BufferedImage i = new BufferedImage(IMG_SIZE, IMG_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics g = i.getGraphics();
        Ck3Tag.CoatOfArms coa = tag.getPrimaryTitle().getCoatOfArms();

        pattern(g, coa, info);
        for (var emblem : coa.getEmblems()) {
            emblem(g, emblem, info);
        }

        applyMask(i, GameImage.CK3_TITLE_MASK);

        g.drawImage(ImageLoader.fromFXImage(GameImage.CK3_TITLE_FRAME),
                -9,
                -6,
                i.getWidth() + 17,
                i.getHeight() + 17,
                new java.awt.Color(0, 0, 0, 0),
                null);

        return ImageLoader.toFXImage(i);
    }

    private static void applyMask(BufferedImage awtImage, Image mask) {
        double xF = mask.getWidth() / awtImage.getWidth();
        double yF = mask.getHeight() / awtImage.getHeight();
        for (int x = 0; x < awtImage.getWidth(); x++) {
            for (int y = 0; y < awtImage.getHeight(); y++) {
                int rgb = awtImage.getRGB(x, y);
                int maskAlpha = mask.getPixelReader().getArgb(
                        (int) Math.floor(xF * x), (int) Math.floor(yF * y)) & 0xFF000000;
                awtImage.setRGB(x, y, maskAlpha + (rgb & 0x00FFFFFF));
            }
        }
    }

    private static int pickClosestColor(int input, int... colors) {
        int minDist = Integer.MAX_VALUE;
        int cMin = -1;
        int counter = 0;
        for (int c : colors) {
            if (Math.abs(input - c) < minDist) {
                minDist = Math.abs(input - c);
                cMin = counter;
            }
            counter++;
        }
        return cMin;
    }

    private static void pattern(Graphics g, Ck3Tag.CoatOfArms coa, SavegameInfo<Ck3Tag> info) {
        if (coa.getPatternFile() != null) {
            int pColor1 = coa.getColors().size() > 0 ? ColorHelper.intFromColor(ColorHelper.loadCk3(info)
                    .getOrDefault(coa.getColors().get(0), javafx.scene.paint.Color.TRANSPARENT)) : 0;
            int pColor2 = coa.getColors().size() > 1 ? ColorHelper.intFromColor(ColorHelper.loadCk3(info)
                    .getOrDefault(coa.getColors().get(1), javafx.scene.paint.Color.TRANSPARENT)) : 0;
            int pColor3 = coa.getColors().size() > 2 ? ColorHelper.intFromColor(ColorHelper.loadCk3(info)
                    .getOrDefault(coa.getColors().get(2), javafx.scene.paint.Color.TRANSPARENT)) : 0;
            Function<Integer, Integer> patternFunction = (Integer rgb) -> {
                int alpha = rgb & 0xFF000000;
                int color = rgb & 0x00FFFFFF;
                int colorIndex = pickClosestColor(color, PATTERN_COLOR_1, PATTERN_COLOR_2, PATTERN_COLOR_3);
                int usedColor = new int[] {pColor1, pColor2, pColor3}[colorIndex] & 0x00FFFFFF;
                return alpha + usedColor;
            };
            var patternFile = CascadeDirectoryHelper.openFile(
                    Path.of("gfx", "coat_of_arms", "patterns").resolve(coa.getPatternFile()),
                    info,
                    GameInstallation.CK3);
            patternFile.map(p -> ImageLoader.loadAwtImage(p, patternFunction)).ifPresent(img -> {
                g.drawImage(img, 0, 0, IMG_SIZE, IMG_SIZE, null);
            });
        }
    }

    private static void emblem(Graphics g, Ck3Tag.CoatOfArms.Emblem emblem, SavegameInfo<Ck3Tag> info) {
        int eColor1 = emblem.getColors().size() > 0 ? ColorHelper.intFromColor(ColorHelper.loadCk3(info)
                .getOrDefault(emblem.getColors().get(0), javafx.scene.paint.Color.TRANSPARENT)) : 0;
        int eColor2 = emblem.getColors().size() > 1 ? ColorHelper.intFromColor(ColorHelper.loadCk3(info)
                .getOrDefault(emblem.getColors().get(1), javafx.scene.paint.Color.TRANSPARENT)) : 0;
        int eColor3 = emblem.getColors().size() > 2 ? ColorHelper.intFromColor(ColorHelper.loadCk3(info)
                .getOrDefault(emblem.getColors().get(2), javafx.scene.paint.Color.TRANSPARENT)) : 0;
        Function<Integer, Integer> customFilter = (Integer rgb) -> {
            int alpha = rgb & 0xFF000000;
            int color = rgb & 0x00FFFFFF;
            int colorIndex = pickClosestColor(color, EMBLEM_COLOR_1, EMBLEM_COLOR_2, EMBLEM_COLOR_3);
            int usedColor = new int[] {eColor1, eColor2, eColor3}[colorIndex] & 0x00FFFFFF;
            return alpha + usedColor;
        };

        boolean hasColor = emblem.getColors().size() > 0;
        var path = CascadeDirectoryHelper.openFile(
                Path.of("gfx", "coat_of_arms",
                        (hasColor ? "colored" : "textured") + "_emblems").resolve(emblem.getFile()),
                info,
                GameInstallation.CK3);
        path.map(p -> ImageLoader.loadAwtImage(p, customFilter)).ifPresent(img -> {
            for (var instance : emblem.getInstances()) {
                g.drawImage(img,
                        (int) instance.getX() * IMG_SIZE,
                        (int) instance.getY() * IMG_SIZE,
                        (int) instance.getScaleX() * IMG_SIZE,
                        (int) instance.getScaleY() * IMG_SIZE,
                        new java.awt.Color(0, 0, 0, 0),
                        null);
            }
        });
    }
}
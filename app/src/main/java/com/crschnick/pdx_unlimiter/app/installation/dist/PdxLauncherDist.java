package com.crschnick.pdx_unlimiter.app.installation.dist;

import com.crschnick.pdx_unlimiter.app.installation.Game;
import com.crschnick.pdx_unlimiter.app.util.JsonHelper;
import com.crschnick.pdx_unlimiter.app.util.OsHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class PdxLauncherDist extends GameDist {

    public static Optional<GameDist> getDist(Game g, Path dir) {
        if (dir == null) {
            return Optional.empty();
        }

        if (!Files.exists(g.getInstallType().getLauncherDataPath(dir).resolve("launcher-settings.json"))) {
            return Optional.empty();
        }

        if (!Files.exists(getBootstrapper())) {
            return Optional.empty();
        }

        return Optional.of(new PdxLauncherDist(g, "Paradox Launcher", dir));
    }

    private static Path getBootstrapper() {
        Path bootstrapper;
        if (SystemUtils.IS_OS_WINDOWS) {
            bootstrapper = Path.of(System.getenv("LOCALAPPDATA"))
                    .resolve("Programs")
                    .resolve("Paradox Interactive")
                    .resolve("bootstrapper-v2.exe");
        } else {
            bootstrapper = Path.of(System.getProperty("user.home"))
                    .resolve(".paradoxlauncher")
                    .resolve("bootstrapper-v2");
        }
        return bootstrapper;
    }

    private static void startParadoxLauncher(Path launcherPath) throws IOException {
        Path bootstrapper = getBootstrapper();

        new ProcessBuilder()
                .directory(launcherPath.toFile())
                .command(bootstrapper.toString(),
                        "--pdxlGameDir", launcherPath.toString(),
                        "--gameDir", launcherPath.toString())
                .start();
    }

    public PdxLauncherDist(Game g, String name, Path installLocation) {
        super(g, name, installLocation);
    }

    private Path getLauncherSettings() {
        return getGame().getInstallType().getLauncherDataPath(getInstallLocation()).resolve("launcher-settings.json");
    }

    private Path replaceVariablesInPath(String value) {
        if (SystemUtils.IS_OS_WINDOWS) {
            value = value.replace("%USER_DOCUMENTS%",
                    OsHelper.getUserDocumentsPath().toString());
        } else if (SystemUtils.IS_OS_LINUX) {
            value = value.replace("$LINUX_DATA_HOME",
                    OsHelper.getUserDocumentsPath().toString());
        }
        return Path.of(value);
    }

    @Override
    public Optional<String> determineVersion() throws IOException {
        JsonNode node = JsonHelper.read(getLauncherSettings());
        return Optional.ofNullable(node.get("version")).map(JsonNode::textValue);
    }

    @Override
    public Path determineUserDir() throws IOException {
        JsonNode node = JsonHelper.read(getLauncherSettings());
        return Optional.ofNullable(node.get("gameDataPath"))
                .map(JsonNode::textValue)
                .map(this::replaceVariablesInPath)
                .orElse(super.determineUserDir());
    }

    @Override
    public boolean supportsLauncher() {
        return true;
    }

    @Override
    public boolean supportsDirectLaunch() {
        return true;
    }

    @Override
    public void startLauncher() throws IOException {
        startParadoxLauncher(getGame().getInstallType().getLauncherDataPath(getInstallLocation()));
    }

    @Override
    public List<Path> getAdditionalSavegamePaths() {
        return List.of();
    }
}

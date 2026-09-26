package de.artnet2dmx.service;

import de.artnet2dmx.ArtNet2DmxApp;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Automatischer Update-Dienst für artnet2dmx.
 * Prüft Releases auf GitHub und führt bei Bedarf ein nahtloses In-App-Update durch.
 */
public class UpdateService {
    private static final Logger LOGGER = Logger.getLogger(UpdateService.class.getName());

    public static final String CURRENT_VERSION = "1.0.5";
    public static final String GITHUB_REPO = "exitishere42/artnet2dmx";
    public static final String API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
    public static final String REPO_URL = "https://github.com/" + GITHUB_REPO;

    public record ReleaseInfo(
            String tagName,
            String name,
            String body,
            String htmlUrl,
            boolean isNewer,
            String jarDownloadUrl,
            String jarFileName,
            long jarSize,
            String appImageDownloadUrl,
            String appImageFileName,
            long appImageSize
    ) {}

    /**
     * Prüft asynchron das neueste Release auf GitHub.
     */
    public static CompletableFuture<ReleaseInfo> checkLatestRelease() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(6))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .timeout(Duration.ofSeconds(10))
                        .header("User-Agent", "artnet2dmx-updater/" + CURRENT_VERSION)
                        .header("Accept", "application/vnd.github.v3+json")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("GitHub API Fehler: HTTP " + response.statusCode());
                }

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());

                String tagName = root.path("tag_name").asText("");
                String name = root.path("name").asText(tagName);
                String body = root.path("body").asText("");
                String htmlUrl = root.path("html_url").asText(REPO_URL);

                String jarUrl = null;
                String jarName = null;
                long jarSize = 0;

                String appImageUrl = null;
                String appImageName = null;
                long appImageSize = 0;

                JsonNode assets = root.path("assets");
                if (assets.isArray()) {
                    for (JsonNode asset : assets) {
                        String aName = asset.path("name").asText("");
                        String aUrl = asset.path("browser_download_url").asText("");
                        long aSize = asset.path("size").asLong(0);

                        if (aName.endsWith("-all.jar") || (aName.endsWith(".jar") && !aName.contains("javadoc") && !aName.contains("sources"))) {
                            jarUrl = aUrl;
                            jarName = aName;
                            jarSize = aSize;
                        } else if (aName.endsWith(".AppImage")) {
                            appImageUrl = aUrl;
                            appImageName = aName;
                            appImageSize = aSize;
                        }
                    }
                }

                boolean isNewer = isVersionNewer(tagName, CURRENT_VERSION);

                return new ReleaseInfo(
                        tagName,
                        name,
                        body,
                        htmlUrl,
                        isNewer,
                        jarUrl,
                        jarName,
                        jarSize,
                        appImageUrl,
                        appImageName,
                        appImageSize
                );
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Konnte GitHub Releases nicht abrufen: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Vergleicht zwei Versionsnummern nach SemVer (z. B. "v1.0.1" vs "1.0.0").
     */
    public static boolean isVersionNewer(String latestTag, String currentVersion) {
        if (latestTag == null || currentVersion == null) return false;
        String cleanLatest = latestTag.trim().replaceAll("^[^0-9]+", "");
        String cleanCurrent = currentVersion.trim().replaceAll("^[^0-9]+", "");
        String[] latParts = cleanLatest.split("[.\\-_]");
        String[] curParts = cleanCurrent.split("[.\\-_]");
        int len = Math.max(latParts.length, curParts.length);
        for (int i = 0; i < len; i++) {
            int l = i < latParts.length ? parsePart(latParts[i]) : 0;
            int c = i < curParts.length ? parsePart(curParts[i]) : 0;
            if (l > c) return true;
            if (l < c) return false;
        }
        return false;
    }

    private static int parsePart(String s) {
        try {
            String digits = s.replaceAll("[^0-9].*$", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Lädt das Update herunter und startet die Anwendung nahtlos neu.
     */
    public static CompletableFuture<Void> performUpdate(
            ReleaseInfo release,
            Consumer<Double> progressCallback,
            Consumer<String> statusCallback
    ) {
        return CompletableFuture.runAsync(() -> {
            try {
                File codeSource;
                try {
                    codeSource = new File(ArtNet2DmxApp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                } catch (Exception e) {
                    codeSource = new File(".");
                }

                File installDir = codeSource.isDirectory() ? codeSource : codeSource.getParentFile();
                if (installDir == null) {
                    installDir = new File(".");
                }

                String appImagePath = System.getenv("APPIMAGE");
                boolean isAppImage = appImagePath != null && new File(appImagePath).exists() && release.appImageDownloadUrl() != null;
                boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

                if (isAppImage) {
                    // Update als AppImage
                    statusCallback.accept("Lade neues AppImage herunter...");
                    File targetAppImage = new File(appImagePath);
                    File tempFile = new File(targetAppImage.getParentFile(), targetAppImage.getName() + ".download");

                    downloadFile(release.appImageDownloadUrl(), tempFile, release.appImageSize(), progressCallback, statusCallback);

                    tempFile.setExecutable(true, false);
                    statusCallback.accept("Ersetze AppImage...");
                    Files.move(tempFile.toPath(), targetAppImage.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    statusCallback.accept("Starte neue Version...");
                    new ProcessBuilder(targetAppImage.getAbsolutePath()).start();
                    System.exit(0);
                    return;
                }

                // Update als Fat-JAR
                if (release.jarDownloadUrl() == null) {
                    throw new IllegalStateException("Kein passendes Release-JAR in den GitHub Assets gefunden.");
                }

                statusCallback.accept("Lade Programmdatei herunter...");
                File tempJar = new File(installDir, "artnet2dmx-update.jar");
                downloadFile(release.jarDownloadUrl(), tempJar, release.jarSize(), progressCallback, statusCallback);

                String newJarName = release.jarFileName() != null ? release.jarFileName() : "artnet2dmx-" + release.tagName() + "-all.jar";
                File targetJar = new File(installDir, newJarName);

                if (!isWindows) {
                    // Linux / macOS: Direktes Ersetzen möglich
                    statusCallback.accept("Wende Update an...");
                    Files.move(tempJar.toPath(), targetJar.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    // run.sh aktualisieren, falls vorhanden
                    File runSh = new File(installDir, "run.sh");
                    if (runSh.exists()) {
                        String runScriptContent = "#!/usr/bin/env bash\n" +
                                "SCRIPT_DIR=\"$(cd \"$(dirname \"${BASH_SOURCE[0]}\")\" && pwd)\"\n" +
                                "cd \"$SCRIPT_DIR\"\n" +
                                "exec java -jar \"$SCRIPT_DIR/" + targetJar.getName() + "\" \"$@\"\n";
                        Files.writeString(runSh.toPath(), runScriptContent);
                        runSh.setExecutable(true);
                        statusCallback.accept("Starte artnet2dmx neu...");
                        new ProcessBuilder("bash", runSh.getAbsolutePath()).directory(installDir).start();
                    } else {
                        statusCallback.accept("Starte artnet2dmx neu...");
                        new ProcessBuilder("java", "-jar", targetJar.getAbsolutePath()).directory(installDir).start();
                    }
                    System.exit(0);
                } else {
                    // Windows: Durch Dateisperre des laufenden Prozesses Helper-Batch nutzen
                    statusCallback.accept("Bereite Neustart vor...");
                    File updateBat = new File(installDir, "update-runner.bat");
                    String batContent = "@echo off\r\n" +
                            "chcp 65001 >nul 2>&1\r\n" +
                            "timeout /t 1 /nobreak >nul\r\n" +
                            ":wait\r\n" +
                            "del \"%~dp0artnet2dmx-*-all.jar\" >nul 2>&1\r\n" +
                            "del \"%~dp0artnet2dmx-1.0.0-all.jar\" >nul 2>&1\r\n" +
                            "if exist \"%~dp0artnet2dmx-1.0.0-all.jar\" (\r\n" +
                            "    timeout /t 1 /nobreak >nul\r\n" +
                            "    goto wait\r\n" +
                            ")\r\n" +
                            "move /y \"%~dp0artnet2dmx-update.jar\" \"%~dp0" + targetJar.getName() + "\" >nul 2>&1\r\n" +
                            "if exist \"%~dp0run.bat\" (\r\n" +
                            "    (\r\n" +
                            "    echo @echo off\r\n" +
                            "    echo cd /d \"%%~dp0\"\r\n" +
                            "    echo start \"\" javaw -jar \"%%~dp0" + targetJar.getName() + "\" %%*\r\n" +
                            "    ) > \"%~dp0run.bat\"\r\n" +
                            "    start \"\" \"%~dp0run.bat\"\r\n" +
                            ") else (\r\n" +
                            "    start \"\" javaw -jar \"%~dp0" + targetJar.getName() + "\"\r\n" +
                            ")\r\n" +
                            "del \"%~f0\"\r\n";

                    Files.writeString(updateBat.toPath(), batContent);
                    statusCallback.accept("Starte artnet2dmx neu...");
                    new ProcessBuilder("cmd.exe", "/c", "start", "/b", updateBat.getAbsolutePath())
                            .directory(installDir)
                            .start();
                    System.exit(0);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Fehler beim Ausführen des Updates: " + e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    private static void downloadFile(
            String downloadUrl,
            File destination,
            long expectedSize,
            Consumer<Double> progressCallback,
            Consumer<String> statusCallback
    ) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("User-Agent", "artnet2dmx-updater/" + CURRENT_VERSION)
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Download fehlgeschlagen: HTTP " + response.statusCode());
        }

        long totalBytes = expectedSize;
        if (totalBytes <= 0) {
            totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(0);
        }

        try (InputStream in = response.body();
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            long bytesReadTotal = 0;
            int bytesRead;

            long lastProgressUpdate = 0;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                bytesReadTotal += bytesRead;

                long now = System.currentTimeMillis();
                if (now - lastProgressUpdate > 100 || bytesReadTotal == totalBytes) {
                    lastProgressUpdate = now;
                    if (totalBytes > 0) {
                        double progress = (double) bytesReadTotal / totalBytes;
                        progressCallback.accept(progress);
                        double mbRead = bytesReadTotal / (1024.0 * 1024.0);
                        double mbTotal = totalBytes / (1024.0 * 1024.0);
                        statusCallback.accept(String.format("Lade Update herunter... (%.1f / %.1f MB - %.0f%%)", mbRead, mbTotal, progress * 100));
                    } else {
                        progressCallback.accept(-1.0);
                        double mbRead = bytesReadTotal / (1024.0 * 1024.0);
                        statusCallback.accept(String.format("Lade Update herunter... (%.1f MB)", mbRead));
                    }
                }
            }
        }
    }
}

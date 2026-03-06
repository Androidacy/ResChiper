package com.androidacy.reschiper;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ResChiperPluginFunctionalTest {

    private void setUp(Path projectDir) throws IOException, URISyntaxException {
        String androidHome = System.getenv("ANDROID_HOME");
        if (androidHome == null || androidHome.isEmpty()) {
            androidHome = System.getenv("ANDROID_SDK_ROOT");
        }
        assumeTrue(androidHome != null && !androidHome.isEmpty(),
                "ANDROID_HOME or ANDROID_SDK_ROOT must be set");

        URL fixtureUrl = getClass().getClassLoader().getResource("test-app");
        assertNotNull(fixtureUrl, "test-app fixture not found on classpath");
        Path fixturePath = Paths.get(fixtureUrl.toURI());

        String finalAndroidHome = androidHome;
        Files.walkFileTree(fixturePath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path target = projectDir.resolve(fixturePath.relativize(dir).toString());
                Files.createDirectories(target);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path target = projectDir.resolve(fixturePath.relativize(file).toString());
                Files.copy(file, target);
                return FileVisitResult.CONTINUE;
            }
        });

        Files.writeString(projectDir.resolve("local.properties"),
                "sdk.dir=" + finalAndroidHome.replace("\\", "\\\\") + "\n");
    }

    private void writeBuildGradle(Path projectDir, String resChiperBlock) throws IOException {
        String buildGradle = "plugins {\n" +
                "    id 'com.android.application'\n" +
                "    id 'com.androidacy.reschiper'\n" +
                "}\n" +
                "android {\n" +
                "    namespace 'com.androidacy.reschiper.testapp'\n" +
                "    compileSdk 35\n" +
                "    defaultConfig {\n" +
                "        applicationId 'com.androidacy.reschiper.testapp'\n" +
                "        minSdk 26\n" +
                "        targetSdk 35\n" +
                "        versionCode 1\n" +
                "        versionName '1.0'\n" +
                "    }\n" +
                "}\n" +
                "resChiper {\n" +
                resChiperBlock + "\n" +
                "}\n";
        Files.writeString(projectDir.resolve("build.gradle"), buildGradle);
    }

    private BuildResult runBuild(Path projectDir) {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments("bundleDebug", "--stacktrace")
                .withPluginClasspath()
                .forwardOutput()
                .build();
    }

    private Path findObfuscatedBundle(Path projectDir) {
        return projectDir.resolve("build/outputs/bundle/debug/obfuscated-bundle.aab");
    }

    private List<String> listZipEntries(Path zipPath) throws IOException {
        List<String> entries = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            var enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                entries.add(entry.getName());
            }
        }
        return entries;
    }

    private void assertBuildSuccess(BuildResult result) {
        assertNotNull(result.task(":resChiperDebug"));
        assertEquals(SUCCESS, result.task(":resChiperDebug").getOutcome());
    }

    @Test
    void obfuscationRenamesResources(@TempDir Path projectDir) throws Exception {
        setUp(projectDir);
        writeBuildGradle(projectDir,
                "    enableObfuscation = true");

        BuildResult result = runBuild(projectDir);
        assertBuildSuccess(result);

        Path bundle = findObfuscatedBundle(projectDir);
        assertTrue(Files.exists(bundle), "Obfuscated bundle should exist");

        List<String> entries = listZipEntries(bundle);
        boolean hasActivityMain = entries.stream()
                .anyMatch(e -> e.contains("res/layout/activity_main"));
        assertFalse(hasActivityMain, "activity_main should be obfuscated (renamed)");

        boolean hasBaseRes = entries.stream()
                .anyMatch(e -> e.startsWith("base/res/"));
        assertTrue(hasBaseRes, "Bundle should still contain base/res/ entries");
    }

    @Test
    void fileFilterRemovesRawFiles(@TempDir Path projectDir) throws Exception {
        setUp(projectDir);
        writeBuildGradle(projectDir,
                "    enableObfuscation = true\n" +
                "    enableFileFiltering = true\n" +
                "    fileFilterList = [\"*/res/raw/*\"]");

        BuildResult result = runBuild(projectDir);
        assertBuildSuccess(result);

        Path bundle = findObfuscatedBundle(projectDir);
        assertTrue(Files.exists(bundle), "Obfuscated bundle should exist");

        List<String> entries = listZipEntries(bundle);
        boolean hasRaw = entries.stream()
                .anyMatch(e -> e.contains("raw/"));
        assertFalse(hasRaw, "Raw directory entries should be filtered out");

        boolean hasSampleTxt = entries.stream()
                .anyMatch(e -> e.contains("sample.txt"));
        assertFalse(hasSampleTxt, "sample.txt should be filtered out");
    }

    @Test
    void duplicateResourcesMerged(@TempDir Path projectDir) throws Exception {
        setUp(projectDir);
        writeBuildGradle(projectDir,
                "    enableObfuscation = true\n" +
                "    mergeDuplicateResources = true");

        BuildResult result = runBuild(projectDir);
        assertBuildSuccess(result);

        Path bundle = findObfuscatedBundle(projectDir);
        assertTrue(Files.exists(bundle), "Obfuscated bundle should exist");
    }

    @Test
    void whitelistPreservesResourceNames(@TempDir Path projectDir) throws Exception {
        setUp(projectDir);
        writeBuildGradle(projectDir,
                "    enableObfuscation = true\n" +
                "    whiteList = [\"R.drawable.ic_logo\"]");

        BuildResult result = runBuild(projectDir);
        assertBuildSuccess(result);

        Path bundle = findObfuscatedBundle(projectDir);
        assertTrue(Files.exists(bundle), "Obfuscated bundle should exist");

        List<String> entries = listZipEntries(bundle);
        boolean hasIcLogo = entries.stream()
                .anyMatch(e -> e.contains("ic_logo"));
        assertTrue(hasIcLogo, "ic_logo should be preserved by whitelist");

        boolean hasActivityMain = entries.stream()
                .anyMatch(e -> e.contains("res/layout/activity_main"));
        assertFalse(hasActivityMain, "activity_main should be obfuscated (renamed)");
    }

    @Test
    void bundleStructureIsValid(@TempDir Path projectDir) throws Exception {
        setUp(projectDir);
        writeBuildGradle(projectDir,
                "    enableObfuscation = true\n" +
                "    enableFileFiltering = true\n" +
                "    fileFilterList = [\"*/res/raw/*\"]\n" +
                "    mergeDuplicateResources = true\n" +
                "    whiteList = [\"R.drawable.ic_logo\"]");

        BuildResult result = runBuild(projectDir);
        assertBuildSuccess(result);

        Path bundle = findObfuscatedBundle(projectDir);
        assertTrue(Files.exists(bundle), "Obfuscated bundle should exist");

        List<String> entries = listZipEntries(bundle);

        assertTrue(entries.stream().anyMatch(e -> e.equals("BundleConfig.pb")),
                "Bundle should contain BundleConfig.pb");
        assertTrue(entries.stream().anyMatch(e -> e.equals("base/manifest/AndroidManifest.xml")),
                "Bundle should contain base/manifest/AndroidManifest.xml");
        assertTrue(entries.stream().anyMatch(e -> e.startsWith("base/dex/")),
                "Bundle should contain entries under base/dex/");
        assertTrue(entries.stream().anyMatch(e -> e.startsWith("base/res/")),
                "Bundle should contain entries under base/res/");
        assertTrue(entries.stream().anyMatch(e -> e.equals("base/resources.pb")),
                "Bundle should contain base/resources.pb");

        try (JarFile jarFile = new JarFile(bundle.toFile())) {
            assertNotNull(jarFile.getManifest(), "Bundle should have a JAR manifest (signed)");
        }
    }
}

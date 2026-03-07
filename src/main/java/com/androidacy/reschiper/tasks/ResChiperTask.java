package com.androidacy.reschiper.tasks;

import com.androidacy.reschiper.ResChiper;
import com.androidacy.reschiper.command.Command;
import com.androidacy.reschiper.command.model.DuplicateResMergerCommand;
import com.androidacy.reschiper.command.model.FileFilterCommand;
import com.androidacy.reschiper.command.model.ObfuscateBundleCommand;
import com.androidacy.reschiper.command.model.StringFilterCommand;
import com.androidacy.reschiper.model.KeyStore;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.*;
import org.gradle.work.DisableCachingByDefault;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom Gradle task for running ResChiper.
 */
@DisableCachingByDefault(because = "Obfuscation output depends on the full bundle content and is not safely cacheable")
public abstract class ResChiperTask extends DefaultTask {

    private static final Logger logger = Logger.getLogger(ResChiperTask.class.getName());

    /**
     * Constructor for the ResChiperTask.
     */
    public ResChiperTask() {
        setDescription("Assemble resource proguard for bundle file");
        setGroup("bundle");
        getOutputs().upToDateWhen(task -> false);
    }

    // Property-based inputs and outputs for configuration cache compatibility

    @Inject
    protected abstract ProjectLayout getProjectLayout();

    @Input
    public abstract Property<String> getVariantName();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getBundlePath();

    @OutputFile
    public abstract RegularFileProperty getObfuscatedBundlePath();

    @Internal
    public abstract DirectoryProperty getBuildDirectory();

    // Extension properties
    @Input
    public abstract Property<Boolean> getEnableObfuscation();

    @Input
    public abstract Property<String> getObfuscationMode();

    @Input
    public abstract Property<Boolean> getEnableFileFiltering();

    @Input
    public abstract Property<Boolean> getEnableFilterStrings();

    @Input
    public abstract Property<Boolean> getMergeDuplicateResources();

    @Input
    @Optional
    public abstract Property<String> getMappingFilePath();

    @Input
    public abstract Property<String> getObfuscatedBundleName();

    @Input
    public abstract Property<String> getUnusedStringFilePath();

    @Input
    public abstract SetProperty<String> getFileFilterList();

    @Input
    public abstract SetProperty<String> getWhiteList();

    @Input
    public abstract SetProperty<String> getLocaleWhiteList();

    // Signing config properties
    @Input
    @Optional
    public abstract Property<String> getKeyStorePath();

    @Internal
    public abstract Property<String> getStorePassword();

    @Internal
    public abstract Property<String> getKeyAlias();

    @Internal
    public abstract Property<String> getKeyPassword();

    @Internal
    public abstract Property<String> getProjectName();

    @Internal
    public abstract Property<String> getAgpVersion();

    @Internal
    public abstract Property<String> getGradleVersion();

    /**
     * Executes the ResChiperTask.
     *
     * @throws Exception If an error occurs during execution.
     */
    @TaskAction
    public void execute() throws Exception {
        printResChiperBuildConfiguration();
        printProjectBuildConfiguration();
        logger.log(Level.INFO, buildExtensionString());

        // Build KeyStore from properties
        KeyStore keyStore = buildKeyStore();

        printSignConfiguration(keyStore);
        printOutputFileLocation();

        // Resolve unused string file path at execution time (config-cache safe)
        String resolvedUnusedStringPath = resolveUnusedStringFilePath();

        Path bundlePath = getBundlePath().get().getAsFile().toPath();
        Path obfuscatedBundlePath = getObfuscatedBundlePath().get().getAsFile().toPath();

        Command.Builder builder = Command.builder();
        builder.setBundlePath(bundlePath);
        builder.setOutputPath(obfuscatedBundlePath);

        ObfuscateBundleCommand.Builder obfuscateBuilder = ObfuscateBundleCommand.builder()
                .setEnableObfuscate(getEnableObfuscation().get())
                .setObfuscationMode(getObfuscationMode().get())
                .setMergeDuplicatedResources(getMergeDuplicateResources().get())
                .setWhiteList(getWhiteList().get())
                .setFilterFile(getEnableFileFiltering().get())
                .setFileFilterRules(getFileFilterList().get())
                .setRemoveStr(getEnableFilterStrings().get())
                .setUnusedStrPath(resolvedUnusedStringPath)
                .setLanguageWhiteList(getLocaleWhiteList().get());

        if (getMappingFilePath().isPresent() && !getMappingFilePath().get().isEmpty())
            obfuscateBuilder.setMappingPath(Path.of(getMappingFilePath().get()));

        if (keyStore.storeFile() != null && keyStore.storeFile().exists())
            builder.setStoreFile(keyStore.storeFile().toPath())
                    .setKeyAlias(keyStore.keyAlias())
                    .setKeyPassword(keyStore.keyPassword())
                    .setStorePassword(keyStore.storePassword());

        builder.setObfuscateBundleBuilder(obfuscateBuilder.build());

        FileFilterCommand.Builder fileFilterBuilder = FileFilterCommand.builder();
        fileFilterBuilder.setFileFilterRules(getFileFilterList().get());
        builder.setFileFilterBuilder(fileFilterBuilder.build());

        StringFilterCommand.Builder stringFilterBuilder = StringFilterCommand.builder();
        builder.setStringFilterBuilder(stringFilterBuilder.build());

        DuplicateResMergerCommand.Builder duplicateResMergeBuilder = DuplicateResMergerCommand.builder();
        builder.setDuplicateResMergeBuilder(duplicateResMergeBuilder.build());

        Command command = builder.build(builder.build(), Command.TYPE.OBFUSCATE_BUNDLE);
        command.execute(Command.TYPE.OBFUSCATE_BUNDLE);
    }

    /**
     * Builds a KeyStore from the task properties.
     */
    private KeyStore buildKeyStore() {
        File storeFile = null;
        if (getKeyStorePath().isPresent() && !getKeyStorePath().get().isEmpty()) {
            storeFile = new File(getKeyStorePath().get());
        }
        return new KeyStore(
                storeFile,
                getStorePassword().getOrElse(null),
                getKeyAlias().getOrElse(null),
                getKeyPassword().getOrElse(null)
        );
    }

    /**
     * Builds a string representation of extension configuration.
     */
    private String buildExtensionString() {
        return "-------------- Extension --------------\n" +
                "\tenableObfuscation=" + getEnableObfuscation().get() + "\n" +
                "\tobfuscationMode=" + getObfuscationMode().get() + "\n" +
                "\tenableFileFiltering=" + getEnableFileFiltering().get() + "\n" +
                "\tenableFilterStrings=" + getEnableFilterStrings().get() + "\n" +
                "\tmergeDuplicateResources=" + getMergeDuplicateResources().get() + "\n" +
                "\tmappingFile=" + getMappingFilePath().getOrElse(null) + "\n" +
                "\tobfuscatedBundleName=" + getObfuscatedBundleName().get() + "\n" +
                "\tunusedStringFile=" + getUnusedStringFilePath().get() + "\n" +
                "\tfileFilterList=" + getFileFilterList().get() + "\n" +
                "\tlocaleWhiteList=" + getLocaleWhiteList().get() + "\n" +
                "\twhiteList=" + getWhiteList().get() + "\n";
    }

    /**
     * Resolves the unused strings file path at execution time.
     * Returns either the configured path or auto-detected path from build outputs.
     *
     * @return The resolved unused strings file path
     */
    private String resolveUnusedStringFilePath() {
        String configuredPath = getUnusedStringFilePath().get();

        // If a path is already configured, use it
        if (configuredPath != null && !configuredPath.isBlank()) {
            File configuredFile = new File(configuredPath);
            if (configuredFile.exists()) {
                System.out.println("Using configured unused_strings.txt: " + configuredPath);
                return configuredPath;
            }
        }

        // Auto-detect from build outputs
        String variantName = getVariantName().get();
        // Extract build type: variant name format is {flavor}{BuildType} where BuildType is capitalized.
        // Find the last uppercase letter boundary to extract the build type.
        String buildType = variantName;
        String simpleName = "";
        for (int i = variantName.length() - 1; i > 0; i--) {
            if (Character.isUpperCase(variantName.charAt(i))) {
                buildType = variantName.substring(i).toLowerCase();
                simpleName = variantName.substring(0, i);
                break;
            }
        }
        if (simpleName.isEmpty()) {
            buildType = variantName.toLowerCase();
        }
        String name;
        if (simpleName.isEmpty()) {
            name = variantName.toLowerCase();
        } else {
            name = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
        }
        String resourcePath = getBuildDirectory().get().getAsFile().getAbsolutePath() + "/outputs/mapping/" + name + "/" + buildType + "/unused_strings.txt";
        File autoDetectedFile = new File(resourcePath);

        if (autoDetectedFile.exists()) {
            System.out.println("Auto-detected unused_strings.txt: " + autoDetectedFile.getAbsolutePath());
            return autoDetectedFile.getAbsolutePath();
        }

        logger.log(Level.WARNING, "unused_strings.txt not found at: " + resourcePath);
        return configuredPath != null ? configuredPath : "";
    }

    /**
     * Prints the signing configuration.
     */
    private void printSignConfiguration(KeyStore keyStore) {
        System.out.println("----------------------------------------");
        System.out.println(" Signing Configuration");
        System.out.println("----------------------------------------");
        System.out.println("\tKeyStoreFile:\t\t" + keyStore.storeFile());
        System.out.println("\tKeyPassword:\t" + encrypt(keyStore.keyPassword()));
        System.out.println("\tAlias:\t\t\t" + encrypt(keyStore.keyAlias()));
        System.out.println("\tStorePassword:\t" + encrypt(keyStore.storePassword()));
    }

    /**
     * Prints the output file location.
     */
    private void printOutputFileLocation() {
        Path obfuscatedBundlePath = getObfuscatedBundlePath().get().getAsFile().toPath();
        System.out.println("----------------------------------------");
        System.out.println(" Output configuration");
        System.out.println("----------------------------------------");
        System.out.println("\tFolder:\t\t" + obfuscatedBundlePath.getParent());
        System.out.println("\tFile:\t\t" + obfuscatedBundlePath.getFileName());
        System.out.println("----------------------------------------");
    }

    /**
     * Encrypts a value for printing (partially).
     *
     * @param value The value to encrypt.
     * @return The encrypted value.
     */
    private @NotNull String encrypt(String value) {
        if (value == null)
            return "/";
        if (value.length() > 2)
            return value.substring(0, Math.min(2, value.length())) + "****";
        return "****";
    }

    private void printResChiperBuildConfiguration() {
        System.out.println("----------------------------------------");
        System.out.println(" ResChiper Plugin Configuration:");
        System.out.println("----------------------------------------");
        System.out.println("- ResChiper version:\t" + ResChiper.VERSION);
        System.out.println("- BundleTool version:\t" + ResChiper.BT_VERSION);
        System.out.println("- AGP version:\t\t" + ResChiper.AGP_VERSION);
        System.out.println("- Gradle Wrapper:\t" + ResChiper.GRADLE_WRAPPER_VERSION);
    }

    private void printProjectBuildConfiguration() {
        System.out.println("----------------------------------------");
        System.out.println(" App Build Information:");
        System.out.println("----------------------------------------");
        System.out.println("- Project name:\t\t\t" + getProjectName().getOrElse("unknown"));
        System.out.println("- AGP version:\t\t\t" + getAgpVersion().getOrElse("unknown"));
        System.out.println("- Running Gradle version:\t" + getGradleVersion().getOrElse("unknown"));
    }
}

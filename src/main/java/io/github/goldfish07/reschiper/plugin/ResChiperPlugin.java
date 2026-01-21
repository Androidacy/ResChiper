package io.github.goldfish07.reschiper.plugin;

import com.android.build.api.variant.ApplicationAndroidComponentsExtension;
import com.android.build.api.variant.ApplicationVariant;
import io.github.goldfish07.reschiper.plugin.internal.AGP;
import io.github.goldfish07.reschiper.plugin.internal.Bundle;
import io.github.goldfish07.reschiper.plugin.internal.SigningConfigHelper;
import io.github.goldfish07.reschiper.plugin.model.KeyStore;
import io.github.goldfish07.reschiper.plugin.tasks.ResChiperTask;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Plugin for integrating ResChiper into an Android Gradle project.
 */
public class ResChiperPlugin implements Plugin<Project> {

    @Override
    public void apply(@NotNull Project project) {
        checkApplicationPlugin(project);
        project.getExtensions().create("resChiper", Extension.class);

        // Use the new androidComponents API (AGP 7.0+, required for AGP 9.0)
        ApplicationAndroidComponentsExtension androidComponents = project.getExtensions()
                .getByType(ApplicationAndroidComponentsExtension.class);

        androidComponents.onVariants(androidComponents.selector().all(), variant -> {
            createResChiperTask(project, variant);
        });
    }

    /**
     * Creates a ResChiper task for the given variant.
     *
     * @param project The Gradle project.
     * @param variant The Android application variant.
     */
    private void createResChiperTask(@NotNull Project project, @NotNull ApplicationVariant variant) {
        String variantName = capitalize(variant.getName());
        String taskName = "resChiper" + variantName;

        // Register the task using the new API
        project.getTasks().register(taskName, ResChiperTask.class, task -> {
            configureResChiperTask(project, task, variant);

            task.doFirst(t -> {
                printResChiperBuildConfiguration();
                printProjectBuildConfiguration(project);
            });
        });

        // Configure task dependencies after evaluation
        project.afterEvaluate(p -> {
            String bundleTaskName = "bundle" + variantName;
            String bundlePackageTaskName = "package" + variantName + "Bundle";
            String finalizeBundleTaskName = "sign" + variantName + "Bundle";

            if (p.getTasks().findByName(bundleTaskName) == null) {
                return;
            }

            ResChiperTask resChiperTask = (ResChiperTask) p.getTasks().findByName(taskName);
            if (resChiperTask == null) {
                return;
            }

            p.getTasks().named(bundleTaskName).configure(bundleTask -> {
                bundleTask.dependsOn(resChiperTask);
            });

            if (p.getTasks().findByName(bundlePackageTaskName) != null) {
                resChiperTask.dependsOn(p.getTasks().named(bundlePackageTaskName));
            }

            if (p.getTasks().findByName(finalizeBundleTaskName) != null) {
                resChiperTask.dependsOn(p.getTasks().named(finalizeBundleTaskName));
            }
        });
    }

    /**
     * Configures ResChiper task properties from project, variant, and extension.
     *
     * @param project The Gradle project.
     * @param task    The ResChiperTask to configure.
     * @param variant The Android application variant.
     */
    private void configureResChiperTask(@NotNull Project project, @NotNull ResChiperTask task, @NotNull ApplicationVariant variant) {
        Extension extension = project.getExtensions().getByType(Extension.class);

        // Set variant information
        task.getVariantName().set(variant.getName());
        task.getBuildDirectory().set(project.getLayout().getBuildDirectory());

        // Set bundle path using the new Artifacts API
        Provider<RegularFile> bundleProvider = Bundle.getBundleFileProvider(variant);
        task.getBundlePath().set(bundleProvider);

        // Set obfuscated bundle output path
        task.getObfuscatedBundlePath().set(project.getLayout().file(bundleProvider.map(bundle -> {
            File bundleFile = bundle.getAsFile();
            return new File(bundleFile.getParentFile(), extension.getObfuscatedBundleName());
        })));

        // Set extension properties
        task.getEnableObfuscation().set(extension.getEnableObfuscation());
        task.getObfuscationMode().set(extension.getObfuscationMode());
        task.getEnableFileFiltering().set(extension.getEnableFileFiltering());
        task.getEnableFilterStrings().set(extension.getEnableFilterStrings());
        task.getMergeDuplicateResources().set(extension.getMergeDuplicateResources());
        task.getObfuscatedBundleName().set(extension.getObfuscatedBundleName());
        task.getUnusedStringFilePath().set(extension.getUnusedStringFile());
        task.getFileFilterList().set(extension.getFileFilterList());
        task.getWhiteList().set(extension.getWhiteList());
        task.getLocaleWhiteList().set(extension.getLocaleWhiteList());

        // Set mapping file path if present
        if (extension.getMappingFile() != null) {
            task.getMappingFilePath().set(extension.getMappingFile().toString());
        } else {
            task.getMappingFilePath().set("");
        }

        // Set signing config properties by looking up from DSL
        KeyStore keyStore = SigningConfigHelper.getSigningConfig(project, variant);
        if (keyStore.storeFile() != null) {
            task.getKeyStorePath().set(keyStore.storeFile().getAbsolutePath());
        } else {
            task.getKeyStorePath().set("");
        }
        task.getStorePassword().set(keyStore.storePassword() != null ? keyStore.storePassword() : "");
        task.getKeyAlias().set(keyStore.keyAlias() != null ? keyStore.keyAlias() : "");
        task.getKeyPassword().set(keyStore.keyPassword() != null ? keyStore.keyPassword() : "");
    }

    /**
     * Checks if the Android Application plugin is applied to the project.
     *
     * @param project The Gradle project.
     */
    private void checkApplicationPlugin(@NotNull Project project) {
        if (!project.getPlugins().hasPlugin("com.android.application")) {
            throw new GradleException("Android Application plugin 'com.android.application' is required");
        }
    }

    /**
     * Prints the ResChiper build configuration information.
     */
    private void printResChiperBuildConfiguration() {
        System.out.println("----------------------------------------");
        System.out.println(" ResChiper Plugin Configuration:");
        System.out.println("----------------------------------------");
        System.out.println("- ResChiper version:\t" + ResChiper.VERSION);
        System.out.println("- BundleTool version:\t" + ResChiper.BT_VERSION);
        System.out.println("- AGP version:\t\t" + ResChiper.AGP_VERSION);
        System.out.println("- Gradle Wrapper:\t" + ResChiper.GRADLE_WRAPPER_VERSION);
    }

    /**
     * Prints the project's build information.
     *
     * @param project The Android Gradle project.
     */
    private void printProjectBuildConfiguration(@NotNull Project project) {
        System.out.println("----------------------------------------");
        System.out.println(" App Build Information:");
        System.out.println("----------------------------------------");
        System.out.println("- Project name:\t\t\t" + project.getRootProject().getName());
        System.out.println("- AGP version:\t\t\t" + AGP.getAGPVersion(project));
        System.out.println("- Running Gradle version:\t" + project.getGradle().getGradleVersion());
    }

    /**
     * Capitalizes the first character of a string.
     *
     * @param str The string to capitalize.
     * @return The capitalized string.
     */
    private static @NotNull String capitalize(@NotNull String str) {
        if (str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}

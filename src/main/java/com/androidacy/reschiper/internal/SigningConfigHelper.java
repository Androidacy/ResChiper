package com.androidacy.reschiper.internal;

import com.android.build.api.dsl.ApplicationBuildType;
import com.android.build.api.dsl.ApplicationExtension;
import com.android.build.api.dsl.ApkSigningConfig;
import com.android.build.api.variant.ApplicationVariant;
import com.androidacy.reschiper.model.KeyStore;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class SigningConfigHelper {
    /**
     * Gets the signing configuration for a variant by looking up the DSL.
     * In AGP 9.0, variant.getSigningConfig() doesn't expose credentials directly,
     * so we need to look them up from the DSL extension.
     *
     * @param project The Gradle project
     * @param variant The Android application variant
     * @return KeyStore with signing information, or KeyStore with null values if not configured
     */
    @Contract("_, _ -> new")
    public static @NotNull KeyStore getSigningConfig(@NotNull Project project, @NotNull ApplicationVariant variant) {
        ApplicationExtension android = project.getExtensions().findByType(ApplicationExtension.class);
        if (android == null) {
            return new KeyStore(null, null, null, null);
        }

        String buildType = variant.getBuildType();
        if (buildType == null) {
            return new KeyStore(null, null, null, null);
        }

        // Get the build type configuration
        ApplicationBuildType buildTypeConfig = findBuildType(android, buildType);
        if (buildTypeConfig == null) {
            return new KeyStore(null, null, null, null);
        }

        // Get the signing config name from the build type
        String signingConfigName = getSigningConfigName(buildTypeConfig);
        if (signingConfigName == null) {
            // Try using the build type name as signing config name (common convention)
            signingConfigName = buildType;
        }

        // Look up the signing config from the DSL
        ApkSigningConfig signingConfig = findSigningConfig(android, signingConfigName);
        if (signingConfig == null) {
            // Try "release" or "debug" as fallback
            signingConfig = findSigningConfig(android, "release");
            if (signingConfig == null) {
                signingConfig = findSigningConfig(android, "debug");
            }
        }

        if (signingConfig == null) {
            return new KeyStore(null, null, null, null);
        }

        return new KeyStore(
                signingConfig.getStoreFile(),
                signingConfig.getStorePassword(),
                signingConfig.getKeyAlias(),
                signingConfig.getKeyPassword()
        );
    }

    @Nullable
    private static ApplicationBuildType findBuildType(@NotNull ApplicationExtension android, @NotNull String name) {
        try {
            NamedDomainObjectContainer<? extends ApplicationBuildType> buildTypes = android.getBuildTypes();
            return buildTypes.findByName(name);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static String getSigningConfigName(@NotNull ApplicationBuildType buildType) {
        try {
            // In AGP 9.0, build types reference signing configs by name
            // This is accessed through the DSL but may not be directly exposed
            // We'll try reflection as a fallback
            var method = buildType.getClass().getMethod("getSigningConfig");
            Object signingConfig = method.invoke(buildType);
            if (signingConfig != null) {
                var nameMethod = signingConfig.getClass().getMethod("getName");
                return (String) nameMethod.invoke(signingConfig);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Nullable
    private static ApkSigningConfig findSigningConfig(@NotNull ApplicationExtension android, @NotNull String name) {
        try {
            NamedDomainObjectContainer<? extends ApkSigningConfig> signingConfigs = android.getSigningConfigs();
            return signingConfigs.findByName(name);
        } catch (Exception e) {
            return null;
        }
    }
}

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

public class SigningConfigHelper {
    /**
     * Gets the signing configuration for a variant by looking up the build type's
     * signing config from the DSL extension.
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

        ApplicationBuildType buildTypeConfig = findBuildType(android, buildType);
        if (buildTypeConfig == null) {
            return new KeyStore(null, null, null, null);
        }

        // Get the signing config directly from the build type DSL object
        ApkSigningConfig signingConfig = buildTypeConfig.getSigningConfig();
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
}

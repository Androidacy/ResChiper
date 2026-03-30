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

    // Properties injected by Android Studio's "Generate Signed Bundle/APK" UI
    private static final String INJECTED_STORE_FILE = "android.injected.signing.store.file";
    private static final String INJECTED_STORE_PASSWORD = "android.injected.signing.store.password";
    private static final String INJECTED_KEY_ALIAS = "android.injected.signing.key.alias";
    private static final String INJECTED_KEY_PASSWORD = "android.injected.signing.key.password";

    /**
     * Gets the signing configuration for a variant by looking up the build type's
     * signing config from the DSL extension, falling back to injected properties
     * set by Android Studio's "Generate Signed Bundle/APK" UI.
     *
     * @param project The Gradle project
     * @param variant The Android application variant
     * @return KeyStore with signing information, or KeyStore with null values if not configured
     */
    @Contract("_, _ -> new")
    public static @NotNull KeyStore getSigningConfig(@NotNull Project project, @NotNull ApplicationVariant variant) {
        // First try DSL signing config from the build type
        KeyStore dslConfig = getSigningConfigFromDsl(project, variant);
        if (dslConfig.storeFile() != null) {
            return dslConfig;
        }

        // Fall back to injected properties (Android Studio "Generate Signed Bundle/APK" flow)
        return getSigningConfigFromInjectedProperties(project);
    }

    @Contract("_, _ -> new")
    private static @NotNull KeyStore getSigningConfigFromDsl(@NotNull Project project, @NotNull ApplicationVariant variant) {
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

    @Contract("_ -> new")
    private static @NotNull KeyStore getSigningConfigFromInjectedProperties(@NotNull Project project) {
        String storeFilePath = getProjectProperty(project, INJECTED_STORE_FILE);
        if (storeFilePath == null) {
            return new KeyStore(null, null, null, null);
        }

        return new KeyStore(
                new File(storeFilePath),
                getProjectProperty(project, INJECTED_STORE_PASSWORD),
                getProjectProperty(project, INJECTED_KEY_ALIAS),
                getProjectProperty(project, INJECTED_KEY_PASSWORD)
        );
    }

    @Nullable
    private static String getProjectProperty(@NotNull Project project, @NotNull String name) {
        if (project.hasProperty(name)) {
            Object value = project.property(name);
            return value != null ? value.toString() : null;
        }
        return null;
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

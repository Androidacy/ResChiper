package com.androidacy.reschiper.internal;

import com.android.build.api.artifact.SingleArtifact;
import com.android.build.api.variant.ApplicationVariant;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;

public class Bundle {
    /**
     * Gets a Provider for the bundle file from the variant using the Artifacts API.
     *
     * @param variant The Android application variant
     * @return Provider for the bundle RegularFile
     */
    public static @NotNull Provider<RegularFile> getBundleFileProvider(@NotNull ApplicationVariant variant) {
        return variant.getArtifacts().get(SingleArtifact.BUNDLE.INSTANCE);
    }
}

package io.github.goldfish07.reschiper.plugin.internal;

import com.android.build.api.variant.AndroidComponentsExtension;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.jetbrains.annotations.NotNull;

public class AGP {
    public static @NotNull String getAGPVersion(@NotNull Project project) {
        // First try to get version from AndroidComponentsExtension (AGP 7.0+)
        AndroidComponentsExtension<?, ?, ?> androidComponents = project.getExtensions()
                .findByType(AndroidComponentsExtension.class);
        if (androidComponents != null) {
            return androidComponents.getPluginVersion().toString();
        }

        // Fallback: resolve from classpath artifacts
        return getAGPVersionFromClasspath(project);
    }

    private static @NotNull String getAGPVersionFromClasspath(@NotNull Project project) {
        for (ResolvedArtifact artifact : project.getRootProject().getBuildscript()
                .getConfigurations().getByName(ScriptHandler.CLASSPATH_CONFIGURATION)
                .getResolvedConfiguration().getResolvedArtifacts()) {
            if (artifact.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier identifier) {
                if ("com.android.tools.build".equals(identifier.getGroup())
                        && "gradle".equals(identifier.getModule())) {
                    return identifier.getVersion();
                }
            }
        }
        throw new GradleException("Failed to get AGP version");
    }
}

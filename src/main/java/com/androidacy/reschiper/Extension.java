package com.androidacy.reschiper;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration class for customizing the behavior of the ResChiper tool or plugin.
 */
public class Extension {
    private boolean enableObfuscation = true;
    private String obfuscationMode = "default";
    private boolean enableFileFiltering = false;
    private boolean enableFilterStrings = false;
    private boolean mergeDuplicateResources = false;
    private Path mappingFile = null;
    private String obfuscatedBundleName = "obfuscated-bundle.aab";
    private String unusedStringFile = "";
    private Set<String> fileFilterList = new HashSet<>();
    private Set<String> whiteList = new HashSet<>();
    private Set<String> localeWhiteList = new HashSet<>();

    public boolean getEnableObfuscation() {
        return enableObfuscation;
    }

    public void setEnableObfuscation(boolean enableObfuscation) {
        this.enableObfuscation = enableObfuscation;
    }

    public String getObfuscationMode() {
        return obfuscationMode;
    }

    public void setObfuscationMode(String obfuscationMode) {
        this.obfuscationMode = obfuscationMode;
    }

    public boolean getEnableFileFiltering() {
        return enableFileFiltering;
    }

    public void setEnableFileFiltering(boolean enableFileFiltering) {
        this.enableFileFiltering = enableFileFiltering;
    }

    public boolean getEnableFilterStrings() {
        return enableFilterStrings;
    }

    public void setEnableFilterStrings(boolean enableFilterStrings) {
        this.enableFilterStrings = enableFilterStrings;
    }

    public boolean getMergeDuplicateResources() {
        return mergeDuplicateResources;
    }

    public void setMergeDuplicateResources(boolean mergeDuplicateResources) {
        this.mergeDuplicateResources = mergeDuplicateResources;
    }

    public Path getMappingFile() {
        return mappingFile;
    }

    public void setMappingFile(Path mappingFile) {
        this.mappingFile = mappingFile;
    }

    public String getObfuscatedBundleName() {
        return obfuscatedBundleName;
    }

    public void setObfuscatedBundleName(String obfuscatedBundleName) {
        this.obfuscatedBundleName = obfuscatedBundleName;
    }

    public String getUnusedStringFile() {
        return unusedStringFile;
    }

    public void setUnusedStringFile(String unusedStringFile) {
        this.unusedStringFile = unusedStringFile;
    }

    public Set<String> getFileFilterList() {
        return fileFilterList;
    }

    public void setFileFilterList(Set<String> fileFilterList) {
        this.fileFilterList = fileFilterList;
    }

    public Set<String> getLocaleWhiteList() {
        return localeWhiteList;
    }

    public void setLocaleWhiteList(Set<String> localeWhiteList) {
        this.localeWhiteList = localeWhiteList;
    }

    public Set<String> getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(Set<String> whiteList) {
        this.whiteList = whiteList;
    }

    /**
     * Provides a formatted string representation of the configuration options.
     *
     * @return A formatted string containing the configuration details.
     */
    @Override
    public String toString() {
        return "-------------- Extension --------------\n" +
                "\tenableObfuscation=" + enableObfuscation + "\n" +
                "\tobfuscationMode=" + obfuscationMode + "\n" +
                "\tenableFileFiltering=" + enableFileFiltering + "\n" +
                "\tenableFilterStrings=" + enableFilterStrings + "\n" +
                "\tmergeDuplicateResources=" + mergeDuplicateResources + "\n" +
                "\tmappingFile=" + mappingFile + "\n" +
                "\tobfuscatedBundleName=" + obfuscatedBundleName + "\n" +
                "\tunusedStringFile=" + unusedStringFile + "\n" +
                "\tfileFilterList=" + fileFilterList + "\n" +
                "\tlocaleWhiteList=" + localeWhiteList + "\n" +
                "\twhiteList=" + whiteList + "\n";
    }
}

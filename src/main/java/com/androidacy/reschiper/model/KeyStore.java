package com.androidacy.reschiper.model;

import java.io.File;

/**
 * Represents a keystore containing a cryptographic key pair for signing purposes.
 */
public record KeyStore(File storeFile, String storePassword, String keyAlias, String keyPassword) {
}

package com.nanotech.flux_pro_backend.common;

/**
 * Implémentée par les exceptions dont le message doit être traduit côté serveur avant
 * d'être renvoyé au client (via {@code MessageSource}, cf. GlobalExceptionHandler).
 *
 * <p>{@code getCode()} sert de clé de résolution i18n (fichiers {@code i18n/messages*.properties})
 * et {@code getArgs()} fournit les valeurs dynamiques injectées dans le message traduit
 * (paramètres {0}, {1}, ... au format {@link java.text.MessageFormat}).</p>
 */
public interface TranslatableError {

    String getCode();

    Object[] getArgs();

    /** Message par défaut (anglais), utilisé si la clé est absente des bundles de traduction. */
    String getMessage();
}

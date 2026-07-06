package com.nanotech.flux_pro_backend.common;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Point d'accès unique à la traduction des messages serveur (erreurs et messages
 * d'import/succès) selon la langue résolue pour la requête courante
 * ({@link LocaleContextHolder}, alimenté par {@code Accept-Language} — cf. LocaleConfig).
 *
 * <p>Retombe toujours sur le message par défaut (anglais, celui écrit dans le code Java)
 * si la clé est absente des bundles {@code i18n/messages*.properties} : une traduction
 * manquante ne peut jamais faire échouer une requête.</p>
 */
@Component
@RequiredArgsConstructor
public class MessageTranslator {

    private final MessageSource messageSource;

    public String translate(String code, Object[] args, String fallback) {
        if (code == null) {
            return fallback;
        }
        try {
            return messageSource.getMessage(code, args, fallback, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return fallback;
        }
    }

    /** Traduit une exception quelconque : messages complets pour les {@link TranslatableError}, sinon message brut. */
    public String translate(Throwable throwable) {
        if (throwable instanceof TranslatableError translatable) {
            return translate(translatable.getCode(), translatable.getArgs(), translatable.getMessage());
        }
        return throwable.getMessage();
    }

    public String translateKey(String key, String fallback) {
        return translate(key, null, fallback);
    }
}

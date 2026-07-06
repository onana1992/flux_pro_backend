package com.nanotech.flux_pro_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Résolution de la langue de réponse à partir de l'en-tête HTTP {@code Accept-Language}
 * envoyé par le frontend (API stateless, pas de session ni de cookie de langue).
 * Seuls le français et l'anglais sont supportés ; toute autre langue retombe sur le français
 * (langue par défaut de l'application, cf. flux-pro-front/src/i18n/settings.ts).
 */
@Configuration
public class LocaleConfig {

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(Locale.FRENCH, Locale.ENGLISH));
        resolver.setDefaultLocale(Locale.FRENCH);
        return resolver;
    }
}

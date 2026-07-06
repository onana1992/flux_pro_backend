package com.nanotech.flux_pro_backend.common;

import com.nanotech.flux_pro_backend.security.AccountInactiveException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les messages d'erreur renvoyés au client selon l'en-tête {@code Accept-Language}
 * (résolu par {@link com.nanotech.flux_pro_backend.config.LocaleConfig}), via
 * {@link MessageTranslator} et les fichiers {@code src/main/resources/i18n/messages*.properties}.
 *
 * <p>Les exceptions implémentant {@link TranslatableError} sont traduites à partir de leur
 * {@code code} (clé i18n) et de leurs {@code args} (paramètres {0}, {1}, ...). Si la clé est
 * absente des bundles, le message d'origine (anglais, celui du code Java) est utilisé comme
 * repli — aucune requête ne peut donc échouer à cause d'une traduction manquante.</p>
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageTranslator translator;

    private ProblemDetail buildDetail(TranslatableError ex, HttpStatus status) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                status, translator.translate(ex.getCode(), ex.getArgs(), ex.getMessage()));
        detail.setTitle(status.is4xxClientError()
                ? translator.translateKey("error.title.badRequest", "Requête invalide")
                : translator.translateKey("error.title.generic", "Erreur"));
        detail.setProperty("code", ex.getCode());
        return detail;
    }

    @ExceptionHandler(FileException.class)
    public ProblemDetail handleFile(FileException ex) {
        return buildDetail(ex, ex.getStatus());
    }

    @ExceptionHandler(ChainTemplateException.class)
    public ProblemDetail handleChainTemplate(ChainTemplateException ex) {
        return buildDetail(ex, ex.getStatus());
    }

    @ExceptionHandler(AlertException.class)
    public ProblemDetail handleAlert(AlertException ex) {
        return buildDetail(ex, ex.getStatus());
    }

    @ExceptionHandler(DashboardException.class)
    public ProblemDetail handleDashboard(DashboardException ex) {
        return buildDetail(ex, ex.getStatus());
    }

    @ExceptionHandler(AppException.class)
    public ProblemDetail handleApp(AppException ex) {
        return buildDetail(ex, ex.getStatus());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setTitle(translator.translateKey("error.title.badRequest", "Requête invalide"));
        return detail;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, translator.translate(ex));
        detail.setTitle(translator.translateKey("error.title.authFailed", "Authentification échouée"));
        if (ex instanceof TranslatableError translatable) {
            detail.setProperty("code", translatable.getCode());
        }
        return detail;
    }

    @ExceptionHandler(LockedException.class)
    public ProblemDetail handleLocked(LockedException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.LOCKED);
        detail.setTitle(translator.translateKey("error.title.accountLocked", "Compte verrouillé"));
        detail.setDetail(translator.translate(ex));
        if (ex instanceof TranslatableError translatable) {
            detail.setProperty("code", translatable.getCode());
        }
        return detail;
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ProblemDetail handleInactive(AccountInactiveException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, translator.translate(ex));
        detail.setTitle(translator.translateKey("error.title.accountInactive", "Compte inactif"));
        detail.setProperty("failureReason", "USER_INACTIVE");
        return detail;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        String detailMessage = ex instanceof TranslatableError
                ? translator.translate(ex)
                : translator.translateKey("error.accessDenied", "Access denied");
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, detailMessage);
        detail.setTitle(translator.translateKey("error.title.accessDenied", "Accès refusé"));
        if (ex instanceof TranslatableError translatable) {
            detail.setProperty("code", translatable.getCode());
        }
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(translator.translateKey("error.validationFailed", "Validation échouée"));
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        detail.setTitle(translator.translateKey("error.title.validationFailed", "Validation échouée"));
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage() != null ? ex.getMessage() : translator.translateKey("error.internal", "Erreur interne"));
        detail.setTitle(translator.translateKey("error.title.server", "Erreur serveur"));
        detail.setProperty("debug", ex.getClass().getSimpleName());
        return detail;
    }
}

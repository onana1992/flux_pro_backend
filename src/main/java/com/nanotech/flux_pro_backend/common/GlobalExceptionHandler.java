package com.nanotech.flux_pro_backend.common;

import com.nanotech.flux_pro_backend.security.AccountInactiveException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FileException.class)
    public ProblemDetail handleFile(FileException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        detail.setTitle(ex.getStatus().is4xxClientError() ? "Requête invalide" : "Erreur");
        detail.setProperty("code", ex.getCode());
        return detail;
    }

    @ExceptionHandler(ChainTemplateException.class)
    public ProblemDetail handleChainTemplate(ChainTemplateException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        detail.setTitle(ex.getStatus().is4xxClientError() ? "Requête invalide" : "Erreur");
        detail.setProperty("code", ex.getCode());
        return detail;
    }

    @ExceptionHandler(AlertException.class)
    public ProblemDetail handleAlert(AlertException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        detail.setTitle(ex.getStatus().is4xxClientError() ? "Requête invalide" : "Erreur");
        detail.setProperty("code", ex.getCode());
        return detail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setTitle("Requête invalide");
        return detail;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        detail.setTitle("Authentification échouée");
        return detail;
    }

    @ExceptionHandler(LockedException.class)
    public ProblemDetail handleLocked(LockedException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.LOCKED);
        detail.setTitle("Compte verrouillé");
        detail.setDetail(ex.getMessage());
        return detail;
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ProblemDetail handleInactive(AccountInactiveException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        detail.setTitle("Compte inactif");
        detail.setProperty("failureReason", "USER_INACTIVE");
        return detail;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Accès refusé");
        detail.setTitle("Accès refusé");
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation échouée");
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        detail.setTitle("Validation échouée");
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage() != null ? ex.getMessage() : "Erreur interne");
        detail.setTitle("Erreur serveur");
        detail.setProperty("debug", ex.getClass().getSimpleName());
        return detail;
    }
}

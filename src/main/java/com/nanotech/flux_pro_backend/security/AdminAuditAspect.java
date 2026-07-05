package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.service.AdminAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

/**
 * Trace automatiquement toute action d'administration (création, modification, exécution) exposée
 * via un endpoint annoté {@link RequiresPermission}, dans le journal {@code admin_audit_log}.
 * <p>
 * Les actions en lecture (suffixe {@code READ}) et d'export (suffixe {@code EXPORT}) ne sont pas
 * tracées afin de limiter le volume aux véritables actions d'administration.
 * <p>
 * Cet aspect s'exécute après {@link RbacValidationAspect} (voir {@code @Order}) : une action
 * refusée pour défaut de permission n'est donc jamais journalisée ici.
 */
@Aspect
@Component
@Order(20)
@RequiredArgsConstructor
public class AdminAuditAspect {

    private static final Set<String> SKIPPED_ACTIONS = Set.of("READ", "EXPORT");

    private final AdminAuditLogService adminAuditLogService;

    @Around("@annotation(requiresPermission) || @within(requiresPermission)")
    public Object audit(ProceedingJoinPoint joinPoint, RequiresPermission requiresPermission) throws Throwable {
        RequiresPermission effective = requiresPermission != null
                ? requiresPermission
                : resolveRequiresPermission(joinPoint);
        if (effective == null || effective.value().length == 0) {
            return joinPoint.proceed();
        }

        String permission = effective.value()[0];
        int separator = permission.indexOf(':');
        if (separator < 0) {
            return joinPoint.proceed();
        }
        String resourceType = permission.substring(0, separator);
        String action = permission.substring(separator + 1);
        if (SKIPPED_ACTIONS.contains(action)) {
            return joinPoint.proceed();
        }

        try {
            Object result = joinPoint.proceed();
            record(resourceType, action, joinPoint.getArgs(), result, true, null);
            return result;
        } catch (Throwable ex) {
            record(resourceType, action, joinPoint.getArgs(), null, false, ex.getMessage());
            throw ex;
        }
    }

    private void record(String resourceType, String action, Object[] args, Object result,
                         boolean success, String errorMessage) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID actorUserId = null;
        String actorEmail = "unknown";
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser user) {
            actorUserId = user.getId();
            actorEmail = user.getEmail();
        } else if (authentication != null && authentication.getName() != null) {
            actorEmail = authentication.getName();
        }

        String resourceId = extractResourceId(args, result);
        String resourceLabel = extractResourceLabel(args, result);
        String ipAddress = resolveIp();
        String userAgent = resolveUserAgent();

        adminAuditLogService.log(actorUserId, actorEmail, resourceType, action,
                resourceId, resourceLabel, success, errorMessage, ipAddress, userAgent);
    }

    private String extractResourceId(Object[] args, Object result) {
        for (Object arg : args) {
            if (arg instanceof UUID uuid) {
                return uuid.toString();
            }
        }
        String fromResult = invokeAccessor(result, "getId", "id");
        return fromResult;
    }

    private String extractResourceLabel(Object[] args, Object result) {
        String label = invokeAccessor(result, "getName", "name");
        if (label == null) {
            label = invokeAccessor(result, "getCode", "code");
        }
        if (label == null) {
            label = invokeAccessor(result, "getEmail", "email");
        }
        if (label != null) {
            return label;
        }
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            String candidate = invokeAccessor(arg, "getName", "name");
            if (candidate == null) {
                candidate = invokeAccessor(arg, "getCode", "code");
            }
            if (candidate == null) {
                candidate = invokeAccessor(arg, "getEmail", "email");
            }
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private String invokeAccessor(Object target, String... methodNames) {
        if (target == null) {
            return null;
        }
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object value = method.invoke(target);
                if (value instanceof String str && !str.isBlank()) {
                    return str;
                }
            } catch (Exception ignored) {
                // accessor absent ou non applicable : on tente le suivant
            }
        }
        return null;
    }

    private String resolveIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUserAgent() {
        HttpServletRequest request = currentRequest();
        return request != null ? request.getHeader("User-Agent") : null;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private RequiresPermission resolveRequiresPermission(ProceedingJoinPoint joinPoint) {
        if (!(joinPoint.getSignature() instanceof MethodSignature sig)) {
            return null;
        }
        Method method = sig.getMethod();
        RequiresPermission onMethod = method.getAnnotation(RequiresPermission.class);
        if (onMethod != null) {
            return onMethod;
        }
        Object target = joinPoint.getTarget();
        if (target == null) {
            return null;
        }
        Class<?>[] paramTypes = method.getParameterTypes();
        for (Class<?> clazz = target.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                Method m = clazz.getDeclaredMethod(method.getName(), paramTypes);
                RequiresPermission annotation = m.getAnnotation(RequiresPermission.class);
                if (annotation != null) {
                    return annotation;
                }
            } catch (NoSuchMethodException ignored) {
                // essai sur la superclasse
            }
        }
        return null;
    }
}

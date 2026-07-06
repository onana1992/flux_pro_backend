package com.nanotech.flux_pro_backend.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** S'exécute avant {@link AdminAuditAspect} (voir {@code @Order}) : un accès refusé n'est jamais journalisé comme une action. */
@Aspect
@Component
@Order(10)
public class RbacValidationAspect {

    @Around("@annotation(requiresPermission) || @within(requiresPermission)")
    public Object validatePermission(ProceedingJoinPoint joinPoint, RequiresPermission requiresPermission)
            throws Throwable {
        if (requiresPermission == null) {
            requiresPermission = resolveRequiresPermission(joinPoint);
        }
        if (requiresPermission == null) {
            return joinPoint.proceed();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new TranslatableAccessDeniedException("AUTH_REQUIRED", "Authentication required");
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        boolean allowed = Arrays.stream(requiresPermission.value()).anyMatch(authorities::contains);
        if (!allowed) {
            String permissions = String.join(", ", requiresPermission.value());
            throw new TranslatableAccessDeniedException(
                    "PERMISSION_REQUIRED", "Permission required: " + permissions, permissions);
        }
        return joinPoint.proceed();
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
                // try superclass
            }
        }
        return null;
    }
}

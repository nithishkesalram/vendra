package com.procureai.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.procureai.common.security.SecurityUtils;
import java.lang.reflect.Method;
import java.time.Instant;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditAspect(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(auditLogged)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLogged auditLogged) throws Throwable {
        Object result = joinPoint.proceed();
        AuditLog log = new AuditLog();
        log.setEntityType(auditLogged.entityType());
        log.setAction(auditLogged.action());
        log.setActor(SecurityUtils.currentActor());
        log.setTimestamp(Instant.now());
        log.setEntityId(extractId(result));
        log.setDiffJson(toJson(joinPoint.getArgs()));
        auditLogRepository.save(log);
        return result;
    }

    private String extractId(Object result) {
        if (result == null) {
            return null;
        }
        for (String methodName : new String[]{"id", "contractId", "quotationId"}) {
            try {
                Method method = result.getClass().getMethod(methodName);
                Object value = method.invoke(result);
                return value == null ? null : value.toString();
            } catch (ReflectiveOperationException ignored) {
                // Try the next conventional response accessor.
            }
        }
        return null;
    }

    private String toJson(Object[] args) {
        try {
            return objectMapper.writeValueAsString(args);
        } catch (JsonProcessingException ex) {
            return "{\"serialization\":\"unavailable\"}";
        }
    }
}

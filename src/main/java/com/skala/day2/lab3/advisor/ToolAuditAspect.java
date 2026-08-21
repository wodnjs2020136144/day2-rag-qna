package com.skala.day2.lab3.advisor;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;

@Aspect
@Component
public class ToolAuditAspect {

    private static final Logger audit =
            LoggerFactory.getLogger("AI_TOOL_AUDIT");

    private final MeterRegistry registry;

    public ToolAuditAspect(MeterRegistry registry) {
        this.registry = registry;
    }

    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object auditToolCall(ProceedingJoinPoint joinPoint) throws Throwable {

        String tool =
                joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "#"
                + joinPoint.getSignature().getName();

        String args = mask(Arrays.toString(joinPoint.getArgs()));
        long started = System.nanoTime();

        try {
            Object result = joinPoint.proceed();

            long elapsedMs =
                    (System.nanoTime() - started) / 1_000_000;

            registry.counter(
                    "ai.tool.calls",
                    "tool", tool,
                    "result", "ok")
                    .increment();

            audit.info(
                    "tool={} args={} status=OK elapsedMs={}",
                    tool, args, elapsedMs);

            return result;

        } catch (Throwable e) {

            registry.counter(
                    "ai.tool.calls",
                    "tool", tool,
                    "result", "fail")
                    .increment();

            audit.error(
                    "tool={} args={} status=FAIL error={}",
                    tool, args, e.toString());

            throw e;
        }
    }

    private String mask(String raw) {
        return raw
                .replaceAll("\\d{6}-\\d{7}", "******-*******")
                .replaceAll(
                        "\\d{4}-\\d{4}-\\d{4}-\\d{4}",
                        "****-****-****-****")
                .replaceAll(
                        "[\\w.+-]+@[\\w-]+\\.[\\w.]+",
                        "***@***");
    }
}

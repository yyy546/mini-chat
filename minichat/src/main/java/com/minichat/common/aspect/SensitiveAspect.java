package com.minichat.common.aspect;

import com.minichat.common.annotation.SensitiveFilter;
import com.minichat.common.util.sensitive.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Aspect
@Component
@RequiredArgsConstructor
public class SensitiveAspect {

    private final SensitiveWordService sensitiveWordService;

    @Around("@annotation(sensitiveFilter)")
    public Object doFilter(ProceedingJoinPoint joinPoint, SensitiveFilter sensitiveFilter) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String replacement = sensitiveFilter.replacement();

        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                continue;
            }
            // 情况 A：参数直接是 String 类型
            if (args[i] instanceof String) {
                args[i] = sensitiveWordService.filterText((String) args[i], replacement);
            }
            // 情况 B：参数是 DTO 对象
            else {
                filterObjectFields(args[i], replacement);
            }
        }
        return joinPoint.proceed(args);
    }

    /**
     * 自动过滤对象中的 String 类型的字段（如 content）
     */
    private void filterObjectFields(Object obj, String replacement) throws IllegalAccessException {
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            // 这里可以根据需要进行过滤，比如过滤名称为 "content" 的字段，或者过滤所有 String 字段
            if (field.getType().equals(String.class)) {
                field.setAccessible(true);
                String value = (String) field.get(obj);
                if (value != null) {
                    String filtered = sensitiveWordService.filterText(value, replacement);
                    field.set(obj, filtered);
                }
            }
        }
    }
}

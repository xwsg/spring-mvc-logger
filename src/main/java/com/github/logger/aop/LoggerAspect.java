package com.github.logger.aop;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.logger.annotation.LoggerIgnore;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author xwsg
 */
@Aspect
@Component
@Slf4j
public class LoggerAspect {

    private static final String IGNORED_HINT = "<Ignored>";
    private static final String ERROR_HINT = "<Error>";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        //禁止将日期解析为时间戳形式
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        //设置时区与格式
        OBJECT_MAPPER.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        OBJECT_MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        // 设置可见性，仅序列化字段，忽略getter/setter
        OBJECT_MAPPER.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        OBJECT_MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        //自动查找外部modules，如jsr310等
        OBJECT_MAPPER.findAndRegisterModules();
    }

    @Pointcut("@within(org.springframework.web.bind.annotation.RequestMapping) && !@annotation(com.github.logger.annotation.LoggerIgnore)")
    public void loggerPointcut() {
    }

    @Around("loggerPointcut()")
    public Object doMonitor(ProceedingJoinPoint pjp) throws Exception {
        StringBuffer className = new StringBuffer();
        StringBuffer methodName = new StringBuffer();
        StringBuffer paramStr = new StringBuffer();
        getJointPointInfo(pjp, className, methodName, paramStr);

        String resultStr = null;
        long start = System.currentTimeMillis();
        long end = 0L;
        Object result = null;
        try {
            result = pjp.proceed();
            end = System.currentTimeMillis();
            if (result instanceof ResponseEntity
                && ((ResponseEntity) result).getBody() instanceof InputStreamResource) {
                InputStreamResource resource = (InputStreamResource) ((ResponseEntity) result).getBody();
                if (resource != null) {
                    resultStr = OBJECT_MAPPER.writeValueAsString(resource.getDescription());
                }
            } else {
                resultStr = OBJECT_MAPPER.writeValueAsString(result);
            }
            return result;
        } catch (Throwable t) {
            if (t instanceof JsonProcessingException) {
                resultStr = ERROR_HINT;
                return result;
            } else if (t instanceof Exception) {
                throw (Exception) t;
            } else {
                throw new RuntimeException(t);
            }
        } finally {
            if (end == 0) {
                end = System.currentTimeMillis();
            }

            log.info("invoke service=" + className + "." + methodName + ", params=["
                + paramStr + "], result=[" + resultStr + "]"
                + ", use time=" + (end - start) + "ms");
        }
    }

    private void getJointPointInfo(ProceedingJoinPoint pjp, StringBuffer className,
        StringBuffer methodName, StringBuffer paramStr) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        className.append(method.getDeclaringClass().getName());

        methodName.append(method.getName());
        Object[] params = pjp.getArgs();
        int length = 0;
        if (params != null) {
            length = params.length;
        }
        Annotation[][] parameterAnnotations = signature.getMethod().getParameterAnnotations();

        for (int i = 0; i < length; i++) {
            try {
                Annotation[] annotations = parameterAnnotations[i];
                if (annotations.length > 0 && Stream.of(annotations).anyMatch(
                    annotation -> annotation.annotationType().equals(LoggerIgnore.class))) {
                    paramStr.append(IGNORED_HINT).append("|");
                    continue;
                }
                if (params[i] instanceof MultipartFile) {
                    paramStr
                        .append(OBJECT_MAPPER
                            .writeValueAsString(((MultipartFile) params[i]).getOriginalFilename()))
                        .append("|");
                    continue;
                } else if (params[i] instanceof HttpServletRequest || params[i] instanceof HttpServletResponse) {
                    paramStr.append(IGNORED_HINT).append("|");
                    continue;
                }
                paramStr.append(OBJECT_MAPPER.writeValueAsString(params[i])).append("|");
            } catch (Throwable t) {
                paramStr.append(ERROR_HINT).append("|");
                log.warn(t.getMessage());
            }
        }
    }
}

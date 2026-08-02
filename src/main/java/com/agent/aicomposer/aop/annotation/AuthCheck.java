package com.agent.aicomposer.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 定义标注目标的类型
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {
    /**
     * 角色参数
     * @return
     */
    String mustRole() default "";
}

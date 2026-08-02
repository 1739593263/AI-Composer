package com.agent.aicomposer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.agent.aicomposer.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class AiComposerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiComposerApplication.class, args);
    }

}

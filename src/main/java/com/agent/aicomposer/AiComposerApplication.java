package com.agent.aicomposer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class AiComposerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiComposerApplication.class, args);
    }

}

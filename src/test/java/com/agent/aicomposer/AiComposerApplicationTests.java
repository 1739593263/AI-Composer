package com.agent.aicomposer;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

@SpringBootTest
class AiComposerApplicationTests {

    @Resource
    DashScopeChatModel chatModel;

    @Test
    void testChat() {
        // 同步调用
//        String response = chatModel.call("你好，请介绍一下自己");
//
//        System.out.println(response);

        // 流式调用
        Flux<ChatResponse> responseFlux = chatModel.stream(new Prompt("您好，请介绍一下自己"));

        CompletableFuture<Void> streamFuture = new CompletableFuture<>();
        responseFlux.subscribe(
                chunk -> System.out.println("--: " + chunk.getResult().getOutput().getText()),
                error -> {
                    System.err.println("流式调用失败: " + error.getMessage());
                    streamFuture.completeExceptionally(error);
                },
                () -> streamFuture.complete(null)
        );

        streamFuture.join();
    }


    @Test
    void contextLoads() {
    }
}

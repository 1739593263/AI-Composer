package com.agent.aicomposer;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

@SpringBootTest
class AiComposerApplicationTests {

    @Resource
    DashScopeChatModel chatModel;

    @Test
    void testChat() {
        // 同步调用
//        String response = chatModel.call("您好，请介绍一下自己");
//        System.out.println(response);
//        ChatResponse response = chatModel.call(
//                new Prompt(
//                        "Generate the names of 5 famous pirates.",
//                        DashScopeChatOptions.builder()
//                                .withModel("qwen-plus")
//                                .withTemperature(0.4)
//                                .build()
//                ));
//        System.out.println(response.toString());

        // 流式调用
        Flux<ChatResponse> responseFlux = chatModel.stream(new Prompt("您好，请介绍一下自己"));
        responseFlux.subscribe(chunk ->
                System.out.println("--: "+chunk.getResult().getOutput().getText())
        );
    }

    @Test
    void contextLoads() {
    }
}

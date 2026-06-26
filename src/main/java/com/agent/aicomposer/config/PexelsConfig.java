package com.agent.aicomposer.config;

import lombok.Getter;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class PexelsConfig {
    private String APIKey = "n9Tv9VO50mQp9sj2gMPpF81EN0zBxjSUIPXvXlvw0nX31BQ40aKtH2aH";
    private String Url = "https://api.pexels.com/v1/search";
}

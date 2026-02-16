package com.ddu.culture.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.genai.Client;

@Configuration
public class GeminiConfig {
	@Value("${gemini.api.key}")
	private String apikey;
	
	@Bean
	public Client geminiClient() {
		if (apikey == null || apikey.isEmpty()) {
			throw new IllegalStateException("GEMINI_API_KEY 환경 변수가 설정되어 있지 않습니다");
		}
		return new Client.Builder().apiKey(apikey).build();
	}
}

package com.wasdi.cmclient.configuration;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
//import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
//import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CmClientConfiguration {

	@Value("${connect.timeout}")
	private Integer connectTimeout;

	@Value("${read.timeout}")
	private Integer readTimeout;

//	@Bean
//	public RestTemplate restTemplate(RestTemplateBuilder builder) {
////		RestTemplate restTemplate = new RestTemplate(getClientHttpRequestFactory());
//		return builder.build();
//	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder
				.setConnectTimeout(Duration.ofSeconds(connectTimeout))
				.setReadTimeout(Duration.ofSeconds(readTimeout))
				.build();
	}

//	@Bean
//	public RestTemplate restTemplate() {
//		HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
//		httpRequestFactory.setConnectionRequestTimeout(60_000);
//		httpRequestFactory.setConnectTimeout(60_000);
//		httpRequestFactory.setReadTimeout(60_000);
//
//		return new RestTemplate(httpRequestFactory);
//	}

//	private SimpleClientHttpRequestFactory getClientHttpRequestFactory() {
//		SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
//		//Connect timeout
//		clientHttpRequestFactory.setConnectTimeout(60_000);
// 
//		//Read timeout
//		clientHttpRequestFactory.setReadTimeout(60_000);
//
//		return clientHttpRequestFactory;
//	}

}

package com.example.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * The one way to call another service: inject {@code RestClient.Builder}, set the base URL, build.
 *
 * It comes with a connect and a read timeout. Without them a hung upstream holds the request
 * thread for as long as the socket stays open, and a handful of those stop the whole app from
 * answering — which is how a slow text-to-speech provider once could take down a children's
 * app, board and all. Override per client when a call is legitimately slow (reading an image,
 * generating audio), never by removing the limit.
 *
 * And never make the call inside a {@code @Transactional} method: the transaction holds a
 * database connection for the whole wait. Do the call first, then the write in its own short
 * transaction.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    @Scope("prototype")   // a builder is mutable: every client starts from a fresh one
    public RestClient.Builder restClientBuilder(
            @Value("${app.http.connect-timeout:5s}") Duration connectTimeout,
            @Value("${app.http.read-timeout:30s}") Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(factory);
    }
}

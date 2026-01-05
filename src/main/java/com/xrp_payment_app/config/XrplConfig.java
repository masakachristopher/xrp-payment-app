package com.xrp_payment_app.config;

import okhttp3.HttpUrl;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.xrpl.xrpl4j.client.XrplClient;

import io.netty.handler.logging.LogLevel;

import java.util.Objects;

@Configuration
public class XrplConfig {

    @Value("${xrpl.testnet.http-url}")
    private String xrplHttpUrl;

    @Value("${xaman.api.baseUrl}")
    private String xamanApiBaseUrlV1;

    @Bean
    public XrplClient xrplClient() {
            HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse(xrplHttpUrl), "XRPL HTTP URL must not be null");
            return new XrplClient(httpUrl);
    }

    @Bean
    public RestClient xrplRestClient() {                         
        return RestClient.builder()
                .baseUrl(xrplHttpUrl)
                .build();
    }

    @Bean("xrplWebClient")
    public WebClient xrplWebClient() {
        // For debugging HTTP requests/responses

        // HttpClient httpClient = HttpClient.create()
        //         .wiretap("reactor.netty.http.client.HttpClient",
        //                  LogLevel.DEBUG,
        //                  AdvancedByteBufFormat.TEXTUAL);

        return WebClient.builder()
                .baseUrl(xrplHttpUrl)
                // .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean("xamanWebClient")
    public WebClient xamanWebClient() {
        // For debugging HTTP requests/responses

        // HttpClient httpClient = HttpClient.create()
        //         .wiretap("reactor.netty.http.client.HttpClient",
        //                  LogLevel.DEBUG,
        //                  AdvancedByteBufFormat.TEXTUAL);
                         
        return WebClient.builder()
            .baseUrl(xamanApiBaseUrlV1)
        //     .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();  
    }

}
package com.xrp_payment_app.config;

import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xrpl.xrpl4j.client.XrplClient;

import java.util.Objects;

@Configuration
public class XrplConfig {

    @Value("${xrpl.testnet.http-url}")
    private String xrplHttpUrl;

    @Bean
    public XrplClient xrplClient() {
            HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse(xrplHttpUrl), "XRPL HTTP URL must not be null");
            return new XrplClient(httpUrl);
    }
}

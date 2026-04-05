package com.minichat.common.config;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EsConfig {

    @Value("${spring.elasticsearch.uris}")
    private String esUri;

    @Value("${spring.elasticsearch.username}")
    private String esUsername;

    @Value("${spring.elasticsearch.password}")
    private String esPassword;



    @Bean
    public RestClient restClient() {
        // 创建凭证提供者，填入账号密码
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,  // 匹配所有ES节点的认证范围
                new UsernamePasswordCredentials(esUsername, esPassword)  // 注入配置的账号密码
        );

        HttpHost host = HttpHost.create(esUri);
        return RestClient.builder(host)
                // 新增：配置HttpClient的认证凭证
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                )
                .build();
    }

    @Bean
    public RestClientTransport restClientTransport(RestClient restClient) {
        // 创建并配置ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // 使用配置好的ObjectMapper创建JacksonJsonpMapper
        JacksonJsonpMapper jacksonJsonpMapper = new JacksonJsonpMapper(objectMapper);

        return new RestClientTransport(
                restClient,
                jacksonJsonpMapper  // 使用配置了JavaTimeModule的mapper
        );
    }

    @Bean("elasticsearchClient")
    public ElasticsearchClient elasticsearchClient(RestClientTransport transport) {
        return new ElasticsearchClient(transport);
    }

    @Bean("elasticsearchAsyncClient")
    public ElasticsearchAsyncClient elasticsearchAsyncClient(RestClientTransport transport) {
        return new ElasticsearchAsyncClient(transport);
    }
}
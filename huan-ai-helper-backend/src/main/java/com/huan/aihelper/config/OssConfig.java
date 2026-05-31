package com.huan.aihelper.config;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "oss.client")
@Data
public class OssConfig {
    /**
     *  域名
     */
    private String endpoint;
    /**
     * 密钥
     */
    private String accessKeyId;
    /**
     *  凭证
     */
    private String accessKeySecret;
    /**
     *  桶名称
     */
    private String bucketName;

    @Bean
    public OSS ossClient() {
        CredentialsProvider provider = new DefaultCredentialProvider(accessKeyId, accessKeySecret);
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();

        return OSSClientBuilder.create()
                .credentialsProvider(provider)
                .clientConfiguration(clientBuilderConfiguration)
                .endpoint(endpoint)
                .build();
    }
}

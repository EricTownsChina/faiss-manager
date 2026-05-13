package faiss.manager.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger API 文档配置
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI faissManagerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FAISS Manager SDK API")
                        .description("基于 FAISS 的向量索引和数据管理 SDK 接口文档")
                        .version("1.0.0"));
    }
}

package com.liangshou.agentic.common.config;

import com.mongodb.client.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * MongoDB 配置诊断 - 用于检查和验证 MongoDB 连接配置。
 *
 * <p>该配置类在应用启动时执行，输出以下诊断信息：</p>
 * <ul>
 *     <li>MongoDB URI 配置值</li>
 *     <li>实际连接的数据库名称</li>
 *     <li>MongoDB 服务器版本</li>
 *     <li>可用的数据库列表</li>
 * </ul>
 *
 * <p>通过此诊断可以确认配置是否正确加载，以及数据是否存储到了预期的数据库中。</p>
 *
 * @author LiangshouX
 */
@Configuration
public class MongoDbDiagnosticConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoDbDiagnosticConfig.class);

    @Value("${spring.mongodb.uri:NOT_SET}")
    private String mongoUri;

    @Value("${spring.mongodb.database:NOT_SET}")
    private String mongoDatabase;

    /**
     * 启动时执行 MongoDB 连接诊断。
     *
     * @param mongoClient MongoDB 客户端
     * @param mongoTemplate MongoDB 模板
     * @return CommandLineRunner
     */
    @Bean
    public CommandLineRunner diagnoseMongoConnection(MongoClient mongoClient, MongoTemplate mongoTemplate) {
        return args -> {
            log.info("========================================");
            log.info("🔍 MongoDB Configuration Diagnostic");
            log.info("========================================");
            log.info("📌 Configured URI: {}", mongoUri);
            log.info("📌 Configured Database: {}", mongoDatabase);
            log.info("📌 Actual Database in Use: {}", mongoTemplate.getDb().getName());
            
            try {
                // 获取 MongoDB 服务器信息
                var serverInfo = mongoClient.getClusterDescription().getClusterSettings().getHosts();
                log.info("📌 MongoDB Hosts: {}", serverInfo);
                
                // 列出所有数据库
                var databases = mongoClient.listDatabaseNames();
                log.info("📌 Available Databases:");
                databases.forEach(dbName -> 
                    log.info("   - {}{}", dbName, dbName.equals(mongoTemplate.getDb().getName()) ? " ✅ (CURRENT)" : "")
                );
                
                // 验证当前使用的数据库
                String actualDb = mongoTemplate.getDb().getName();
                if ("test".equals(actualDb)) {
                    log.warn("⚠️  WARNING: Using 'test' database! This is likely a configuration issue.");
                    log.warn("💡 Solution: Check your spring.mongodb.uri or spring.mongodb.database property.");
                } else if ("hivemind".equals(actualDb)) {
                    log.info("✅ SUCCESS: Correctly using 'hivemind' database!");
                }
                
            } catch (Exception e) {
                log.error("❌ Error during MongoDB diagnostic: {}", e.getMessage(), e);
            }
            
            log.info("========================================");
        };
    }
}

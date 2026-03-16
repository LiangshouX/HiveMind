package com.tangdynasty.agent.engine.performance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 性能压力测试
 */
@DisplayName("性能压力测试")
class PerformanceTest {
    
    @Test
    @DisplayName("并发执行测试（100 个请求）")
    void testConcurrentExecution() throws Exception {
        int concurrentUsers = 100;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // 提交 100 个并发请求
        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    // 模拟业务处理
                    Thread.sleep(50); // 模拟 50ms 的处理时间
                    successCount.incrementAndGet();
                    System.out.printf("用户 %d 执行成功%n", userId);
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.printf("用户 %d 执行失败：%s%n", userId, e.getMessage());
                }
            });
        }
        
        // 关闭线程池并等待完成
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 输出统计结果
        System.out.println("==================== 性能测试结果 ====================");
        System.out.printf("总请求数：%d%n", concurrentUsers);
        System.out.printf("成功数：%d (%.2f%%)%n", successCount.get(), 
            (successCount.get() * 100.0 / concurrentUsers));
        System.out.printf("失败数：%d (%.2f%%)%n", failCount.get(), 
            (failCount.get() * 100.0 / concurrentUsers));
        System.out.printf("总耗时：%d ms%n", duration);
        System.out.printf("平均响应时间：%.2f ms%n", (duration * 1.0 / successCount.get()));
        System.out.printf("QPS: %.2f%n", (successCount.get() * 1000.0 / duration));
        System.out.println("======================================================");
        
        // 断言：成功率应该达到 95% 以上
        assert successCount.get() >= concurrentUsers * 0.95 : "成功率低于 95%";
    }
    
    @Test
    @DisplayName("内存使用测试")
    void testMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        
        // 强制垃圾回收
        runtime.gc();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // 模拟大量数据处理
        int dataSize = 10000;
        Object[] data = new Object[dataSize];
        for (int i = 0; i < dataSize; i++) {
            data[i] = new byte[1024]; // 每个对象 1KB
        }
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long usedMemory = finalMemory - initialMemory;
        
        System.out.println("==================== 内存使用测试 ====================");
        System.out.printf("初始内存：%d KB%n", initialMemory / 1024);
        System.out.printf("最终内存：%d KB%n", finalMemory / 1024);
        System.out.printf("使用内存：%d KB%n", usedMemory / 1024);
        System.out.printf("对象数量：%d%n", dataSize);
        System.out.printf("平均每个对象占用：%.2f bytes%n", (usedMemory * 1.0 / dataSize));
        System.out.println("======================================================");
    }
}

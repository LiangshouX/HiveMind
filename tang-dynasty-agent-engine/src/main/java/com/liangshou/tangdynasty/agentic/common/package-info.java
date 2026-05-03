/**
 * 通用层 - 跨领域共享的异常和工具类。
 *
 * <p>该包包含不特定于任何领域的通用组件。</p>
 *
 * <h2>子包说明</h2>
 * <ul>
 *   <li><b>config/</b> - Spring 配置类（JacksonConfig、MongoDbDiagnosticConfig）</li>
 *   <li><b>exception/</b> - 通用异常定义（BizException、ErrorCodeEnum、IErrorCode）</li>
 *   <li><b>util/</b> - 通用工具类（MessageMapper、SoulPromptLoader）</li>
 * </ul>
 *
 * <h2>设计原则</h2>
 * <ul>
 *   <li>common 包仅包含跨领域共享的通用组件</li>
 *   <li>领域相关的枚举和常量应该在 domain/shared/ 中</li>
 *   <li>异常定义是通用的，但错误码枚举可能包含领域特定的错误</li>
 * </ul>
 */
package com.liangshou.tangdynasty.agentic.common;

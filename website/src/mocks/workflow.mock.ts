/**
 * 前端 Mock 数据生成器
 * 
 * 基于 OpenAPI 规范自动生成 Mock 数据
 * 用于前端开发和测试
 */

export interface WorkflowRequest {
  content: string;
  options?: {
    departments?: ('libu' | 'hubu' | 'bingbu' | 'xingbu' | 'gongbu' | 'libu_hr')[];
    priority?: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
    timeout?: number;
  };
}

export interface WorkflowResponse {
  success: boolean;
  data: WorkflowResult;
  message: string;
}

export interface WorkflowResult {
  response?: string;
  plan?: string;
  todos?: TodoItem[];
  status: 'SUCCESS' | 'FAILED' | 'PARTIAL';
  executionTime?: number;
}

export interface TodoItem {
  id: string;
  title: string;
  department: string;
  estimatedTime: string;
  status: 'pending' | 'in_progress' | 'completed' | 'failed';
}

/**
 * 生成 Mock 响应数据
 */
export function generateMockResponse(request: WorkflowRequest): WorkflowResponse {
  const isChat = request.content.length < 20;
  
  if (isChat) {
    // 闲聊场景
    return {
      success: true,
      data: {
        response: `您好！我是您的 AI 助手。您刚才说的是："${request.content}"，请问有什么可以帮助您的吗？`,
        status: 'SUCCESS',
        executionTime: Math.random() * 500 + 100
      },
      message: '执行成功'
    };
  } else {
    // 任务场景
    return {
      success: true,
      data: {
        plan: generateMockPlan(request.content),
        todos: generateMockTodos(),
        status: 'SUCCESS',
        executionTime: Math.random() * 2000 + 500
      },
      message: '执行成功'
    };
  }
}

/**
 * 生成 Mock 方案
 */
function generateMockPlan(requirement: string): string {
  return `# ${requirement} - 执行方案

## 一、方案概述
本方案旨在通过系统化的方法完成"${requirement}"任务，预计分为三个阶段实施。

## 二、执行步骤
1. **需求分析阶段**（礼部负责）
   - 收集相关文档和资料
   - 整理核心需求点
   - 输出需求分析报告

2. **方案设计阶段**（工部负责）
   - 设计整体架构
   - 制定技术规范
   - 评审设计方案

3. **实施执行阶段**（六部协同）
   - 按照分工执行具体任务
   - 定期检查进度和质量
   - 及时调整和优化

## 三、预期成果
- 完整的需求分析文档
- 详细的技术设计方案
- 可运行的系统/功能模块

## 四、风险评估
- 技术风险：新技术学习成本
- 时间风险：任务量可能超出预期
- 人员风险：关键人员依赖

建议做好风险预案，确保项目顺利推进。`;
}

/**
 * 生成 Mock 待办任务
 */
function generateMockTodos(): TodoItem[] {
  return [
    {
      id: '1',
      title: '需求分析与整理',
      department: '礼部',
      estimatedTime: '2h',
      status: 'pending'
    },
    {
      id: '2',
      title: '技术方案设计',
      department: '工部',
      estimatedTime: '3h',
      status: 'pending'
    },
    {
      id: '3',
      title: '资源调配与预算',
      department: '户部',
      estimatedTime: '1h',
      status: 'pending'
    },
    {
      id: '4',
      title: '代码实现与开发',
      department: '兵部',
      estimatedTime: '8h',
      status: 'pending'
    },
    {
      id: '5',
      title: '质量测试与验证',
      department: '刑部',
      estimatedTime: '2h',
      status: 'pending'
    },
    {
      id: '6',
      title: '人员安排与协调',
      department: '吏部',
      estimatedTime: '1h',
      status: 'pending'
    }
  ];
}

/**
 * 模拟 API 调用延迟
 */
export function mockApiCall<T>(data: T, delay: number = 1000): Promise<T> {
  return new Promise((resolve) => {
    setTimeout(() => resolve(data), delay);
  });
}

/**
 * 模拟 SSE 流式事件
 */
export function* simulateSSEEvents() {
  yield { event: 'node-start', data: { node: 'chengxiang', timestamp: Date.now() } };
  yield { event: 'node-progress', data: { node: 'zhongshu', progress: 25 } };
  yield { event: 'node-progress', data: { node: 'zhongshu', progress: 50 } };
  yield { event: 'node-progress', data: { node: 'zhongshu', progress: 75 } };
  yield { event: 'node-complete', data: { node: 'zhongshu', result: '规划完成' } };
  yield { event: 'node-start', data: { node: 'menxia', timestamp: Date.now() } };
  yield { event: 'node-complete', data: { node: 'menxia', result: '审议通过' } };
  yield { event: 'workflow-complete', data: { status: 'SUCCESS', totalNodes: 3 } };
}

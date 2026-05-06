import React, { useState, useEffect, useCallback } from 'react';
import {
  Typography, Card, Button, Row, Col, Tag, Switch, Drawer, Form, Input,
  Space, message, Badge, Select, Spin, Empty, Popconfirm, Tooltip,
  Tabs, Divider, Alert
} from 'antd';
import {
  PlusOutlined, CloudOutlined, EditOutlined,
  DeleteOutlined, DownloadOutlined, CheckCircleOutlined,
  FolderOutlined, InfoCircleOutlined, CrownOutlined
} from '@ant-design/icons';
import { useAuth } from '../../providers/AuthProvider';
import {
  fetchAllSkills,
  createCloudSkill,
  updateCloudSkill,
  publishCloudSkill,
  archiveCloudSkill,
  deleteCloudSkill,
  getSkillDownloadUrl
} from '../../services/skillApi';
import type { CloudSkill, SkillCreateRequest, SkillVersionRequest } from '../../types/skillApi';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

// 状态颜色映射
const STATUS_COLORS: Record<string, { color: string; text: string }> = {
  draft: { color: 'default', text: '草稿' },
  published: { color: 'success', text: '已发布' },
  deprecated: { color: 'warning', text: '已弃用' },
  archived: { color: 'error', text: '已归档' },
};

const SkillLibrary: React.FC = () => {
  const { user } = useAuth();
  const userId = user?.userId || '';

  // 状态管理
  const [skills, setSkills] = useState<CloudSkill[]>([]);
  const [loading, setLoading] = useState(false);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [currentSkill, setCurrentSkill] = useState<CloudSkill | null>(null);
  const [form] = Form.useForm();
  const [preview, setPreview] = useState(true);
  const [drawerMode, setDrawerMode] = useState<'create' | 'edit' | 'view'>('view');
  const [publishLoading, setPublishLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('content');

  // 加载 Skills
  const loadSkills = useCallback(async () => {
    if (!userId) return;

    setLoading(true);
    try {
      const allSkills = await fetchAllSkills(userId);
      setSkills(allSkills);
    } catch (error: any) {
      message.error(error.message || '加载技能列表失败');
      console.error('加载技能列表失败:', error);
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    loadSkills();
  }, [loadSkills]);

  // 打开创建抽屉
  const handleCreate = () => {
    setDrawerMode('create');
    setCurrentSkill(null);
    form.resetFields();
    form.setFieldsValue({
      version: '1.0.0',
      publish: false,
      tags: [],
    });
    setDrawerVisible(true);
  };

  // 打开编辑抽屉
  const handleEdit = (skill: CloudSkill) => {
    setDrawerMode('edit');
    setCurrentSkill(skill);
    form.setFieldsValue({
      name: skill.name,
      description: skill.description,
      skillMarkdown: '', // 需要从 OSS 下载
      version: incrementVersion(skill.currentVersion),
      tags: skill.tags || [],
    });
    setDrawerVisible(true);
  };

  // 打开查看抽屉
  const handleView = (skill: CloudSkill) => {
    setDrawerMode('view');
    setCurrentSkill(skill);
    setDrawerVisible(true);
  };

  // 保存 Skill
  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      
      if (drawerMode === 'create') {
        const request: SkillCreateRequest = {
          name: values.name,
          description: values.description,
          skillMarkdown: values.skillMarkdown,
          resources: values.resources ? JSON.parse(values.resources) : undefined,
          tags: values.tags,
          version: values.version || '1.0.0',
          publish: values.publish || false,
        };
        
        await createCloudSkill(userId, request);
        message.success('技能创建成功');
      } else if (drawerMode === 'edit' && currentSkill) {
        const request: SkillVersionRequest = {
          skillMarkdown: values.skillMarkdown,
          resources: values.resources ? JSON.parse(values.resources) : undefined,
          version: values.version || incrementVersion(currentSkill.currentVersion),
        };
        
        await updateCloudSkill(userId, currentSkill.skillId, request);
        message.success('技能更新成功');
      }
      
      setDrawerVisible(false);
      loadSkills();
    } catch (error: any) {
      if (error.errorFields) {
        message.error('请检查表单输入');
      } else {
        message.error(error.message || '保存失败');
        console.error('保存失败:', error);
      }
    }
  };

  // 发布 Skill
  const handlePublish = async (skill: CloudSkill) => {
    setPublishLoading(true);
    try {
      await publishCloudSkill(userId, skill.skillId);
      message.success('技能发布成功');
      loadSkills();
    } catch (error: any) {
      message.error(error.message || '发布失败');
      console.error('发布失败:', error);
    } finally {
      setPublishLoading(false);
    }
  };

  // 归档 Skill
  const handleArchive = async (skill: CloudSkill) => {
    try {
      await archiveCloudSkill(userId, skill.skillId);
      message.success('技能已归档');
      loadSkills();
    } catch (error: any) {
      message.error(error.message || '归档失败');
      console.error('归档失败:', error);
    }
  };

  // 删除 Skill
  const handleDelete = async (skill: CloudSkill) => {
    try {
      await deleteCloudSkill(userId, skill.skillId);
      message.success('技能已删除');
      loadSkills();
    } catch (error: any) {
      message.error(error.message || '删除失败');
      console.error('删除失败:', error);
    }
  };

  // 下载 Skill
  const handleDownload = async (skill: CloudSkill) => {
    try {
      const { downloadUrl } = await getSkillDownloadUrl(userId, skill.skillId);
      window.open(downloadUrl, '_blank');
      message.success('开始下载');
    } catch (error: any) {
      message.error(error.message || '获取下载链接失败');
      console.error('下载失败:', error);
    }
  };

  // 版本号递增
  const incrementVersion = (version: string): string => {
    const parts = version.split('.').map(Number);
    if (parts.length === 3) {
      parts[2] += 1;
      return parts.join('.');
    }
    return '1.0.0';
  };

  // 渲染状态标签
  const renderStatus = (status: string) => {
    const config = STATUS_COLORS[status] || STATUS_COLORS.draft;
    return <Badge status={config.color as any} text={config.text} />;
  };

  return (
    <div style={{ height: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column', padding: '16px' }}>
      {/* 头部 */}
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={4} style={{ margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
            <CloudOutlined />
            云端技能库
          </Title>
          <Text type="secondary">
            管理云端存储的智能体技能，支持版本管理和在线发布。
            <Tooltip title="技能文件存储在阿里云 OSS，支持版本控制和预签名下载">
              <InfoCircleOutlined style={{ marginLeft: 8, color: '#999', cursor: 'help' }} />
            </Tooltip>
          </Text>
        </div>
        <Button 
          type="primary" 
          icon={<PlusOutlined />} 
          onClick={handleCreate}
          size="large"
        >
          创建新技能
        </Button>
      </div>

      {/* 技能列表 */}
      <div style={{ flex: 1, overflowY: 'auto' }}>
        {loading ? (
          <div style={{ textAlign: 'center', padding: '100px 0' }}>
            <Spin size="large" tip="加载中..." />
          </div>
        ) : skills.length === 0 ? (
          <Empty 
            description="暂无技能" 
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          >
            <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
              创建第一个技能
            </Button>
          </Empty>
        ) : (
          <Row gutter={[16, 16]}>
            {skills.map(skill => {
              const isBuiltin = skill.source === 'BUILTIN';
              return (
                <Col xs={24} sm={12} md={8} lg={6} key={skill.skillId}>
                  <Card
                    hoverable
                    style={{ height: '100%', display: 'flex', flexDirection: 'column' }}
                    bodyStyle={{ flex: 1, display: 'flex', flexDirection: 'column', padding: 16 }}
                    onClick={() => handleView(skill)}
                    actions={isBuiltin ? [] : [
                      <Tooltip key="edit" title="编辑">
                        <Button
                          type="text"
                          icon={<EditOutlined />}
                          onClick={(e) => { e.stopPropagation(); handleEdit(skill); }}
                        />
                      </Tooltip>,
                      <Tooltip key="download" title="下载">
                        <Button
                          type="text"
                          icon={<DownloadOutlined />}
                          onClick={(e) => { e.stopPropagation(); handleDownload(skill); }}
                        />
                      </Tooltip>,
                      skill.status !== 'published' && (
                        <Tooltip key="publish" title="发布">
                          <Button
                            type="text"
                            icon={<CheckCircleOutlined />}
                            onClick={(e) => { e.stopPropagation(); handlePublish(skill); }}
                          />
                        </Tooltip>
                      ),
                      <Tooltip key="archive" title="归档">
                        <Popconfirm
                          title="确定要归档此技能吗？"
                          onConfirm={(e) => { e?.stopPropagation(); handleArchive(skill); }}
                          okText="确定"
                          cancelText="取消"
                        >
                          <Button
                            type="text"
                            icon={<FolderOutlined />}
                            onClick={(e) => e.stopPropagation()}
                          />
                        </Popconfirm>
                      </Tooltip>,
                      <Tooltip key="delete" title="删除">
                        <Popconfirm
                          title="确定要删除此技能吗？"
                          description="删除后将无法恢复"
                          onConfirm={(e) => { e?.stopPropagation(); handleDelete(skill); }}
                          okText="确定"
                          cancelText="取消"
                        >
                          <Button
                            type="text"
                            danger
                            icon={<DeleteOutlined />}
                            onClick={(e) => e.stopPropagation()}
                          />
                        </Popconfirm>
                      </Tooltip>,
                    ]}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
                      <Space>
                        {isBuiltin ? (
                          <CrownOutlined style={{ fontSize: 24, color: '#faad14' }} />
                        ) : (
                          <CloudOutlined style={{ fontSize: 24, color: '#1890ff' }} />
                        )}
                        <Text strong style={{ fontSize: 16 }}>{skill.name}</Text>
                      </Space>
                      <Space direction="vertical" size={4} align="end">
                        {isBuiltin && (
                          <Tag color="gold" style={{ fontSize: 11 }}>系统内置</Tag>
                        )}
                        {renderStatus(skill.status)}
                        {!isBuiltin && <Tag color="geekblue">v{skill.currentVersion}</Tag>}
                      </Space>
                    </div>

                    <div style={{ flex: 1, marginBottom: 16 }}>
                      <Paragraph type="secondary" ellipsis={{ rows: 3 }}>
                        {skill.description || '暂无描述'}
                      </Paragraph>
                    </div>

                    <Divider style={{ margin: '12px 0' }} />

                    <div style={{ marginTop: 'auto' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                        <Text type="secondary" style={{ fontSize: 12 }}>标签</Text>
                        <Space wrap>
                          {(skill.tags || []).slice(0, 3).map(tag => (
                            <Tag key={tag} style={{ fontSize: 11 }}>{tag}</Tag>
                          ))}
                          {(skill.tags || []).length > 3 && (
                            <Tag style={{ fontSize: 11 }}>+{skill.tags!.length - 3}</Tag>
                          )}
                        </Space>
                      </div>

                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 11, color: '#999' }}>
                        <Text type="secondary">
                          {skill.updatedAt ? `更新于 ${new Date(skill.updatedAt).toLocaleDateString()}` : '系统预置'}
                        </Text>
                      </div>
                    </div>
                  </Card>
                </Col>
              );
            })}
            
            {/* 创建按钮卡片 */}
            <Col xs={24} sm={12} md={8} lg={6}>
              <Card 
                hoverable 
                style={{ 
                  height: '100%', 
                  minHeight: 280,
                  border: '2px dashed #d9d9d9',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  cursor: 'pointer',
                }}
                onClick={handleCreate}
              >
                <div style={{ textAlign: 'center', color: '#999' }}>
                  <PlusOutlined style={{ fontSize: 48, marginBottom: 16 }} />
                  <div>添加新技能</div>
                </div>
              </Card>
            </Col>
          </Row>
        )}
      </div>

      {/* 抽屉 */}
      <Drawer
        title={
          drawerMode === 'create' ? '创建技能' : 
          drawerMode === 'edit' ? '编辑技能' : 
          '查看技能'
        }
        placement="right"
        width={700}
        onClose={() => setDrawerVisible(false)}
        open={drawerVisible}
        extra={
          <Space>
            {drawerMode !== 'view' && (
              <>
                <Button onClick={() => setDrawerVisible(false)}>取消</Button>
                <Button type="primary" onClick={handleSave} loading={publishLoading}>
                  保存
                </Button>
              </>
            )}
            {drawerMode === 'view' && currentSkill && (
              <>
                <Button 
                  icon={<DownloadOutlined />} 
                  onClick={() => handleDownload(currentSkill)}
                >
                  下载
                </Button>
                {currentSkill.status !== 'published' && (
                  <Button 
                    type="primary" 
                    icon={<CheckCircleOutlined />}
                    onClick={() => handlePublish(currentSkill)}
                    loading={publishLoading}
                  >
                    发布
                  </Button>
                )}
                <Button 
                  icon={<EditOutlined />}
                  onClick={() => {
                    setDrawerMode('edit');
                    handleEdit(currentSkill);
                  }}
                >
                  编辑
                </Button>
              </>
            )}
          </Space>
        }
      >
        {currentSkill && drawerMode === 'view' && (
          <div>
            <Alert
              message={`当前版本: v${currentSkill.currentVersion}`}
              description={`状态: ${renderStatus(currentSkill.status)}`}
              type="info"
              showIcon
              style={{ marginBottom: 24 }}
            />

            <Tabs
              activeKey={activeTab}
              onChange={setActiveTab}
              items={[
                {
                  key: 'content',
                  label: '内容',
                  children: (
                    <div style={{ padding: 16, background: '#f5f5f5', borderRadius: 8, minHeight: 400, whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: 13 }}>
                      {currentSkill.fileManifest 
                        ? JSON.stringify(currentSkill.fileManifest, null, 2)
                        : '内容需要从 OSS 下载后查看'
                      }
                    </div>
                  ),
                },
                {
                  key: 'info',
                  label: '信息',
                  children: (
                    <div style={{ padding: 16 }}>
                      <div style={{ marginBottom: 16 }}>
                        <Text strong>名称</Text>
                        <div>{currentSkill.name}</div>
                      </div>
                      <div style={{ marginBottom: 16 }}>
                        <Text strong>描述</Text>
                        <div>{currentSkill.description || '暂无'}</div>
                      </div>
                      <div style={{ marginBottom: 16 }}>
                        <Text strong>标签</Text>
                        <div>
                          <Space wrap>
                            {(currentSkill.tags || []).map(tag => (
                              <Tag key={tag}>{tag}</Tag>
                            ))}
                          </Space>
                        </div>
                      </div>
                      <div style={{ marginBottom: 16 }}>
                        <Text strong>版本</Text>
                        <div>v{currentSkill.currentVersion}</div>
                      </div>
                      <div style={{ marginBottom: 16 }}>
                        <Text strong>创建时间</Text>
                        <div>{currentSkill.createdAt ? new Date(currentSkill.createdAt).toLocaleString() : '系统预置'}</div>
                      </div>
                      <div>
                        <Text strong>更新时间</Text>
                        <div>{currentSkill.updatedAt ? new Date(currentSkill.updatedAt).toLocaleString() : '系统预置'}</div>
                      </div>
                    </div>
                  ),
                },
              ]}
            />
          </div>
        )}

        {(drawerMode === 'create' || drawerMode === 'edit') && (
          <Form form={form} layout="vertical">
            <Form.Item 
              name="name" 
              label="技能名称" 
              rules={[{ required: true, message: '请输入技能名称' }]}
            >
              <Input placeholder="例如：数据分析助手" />
            </Form.Item>

            <Form.Item name="description" label="描述">
              <TextArea rows={2} placeholder="简要描述此技能的功能" />
            </Form.Item>

            <Form.Item name="tags" label="标签">
              <Select
                mode="tags"
                placeholder="输入标签后按回车"
                style={{ width: '100%' }}
              />
            </Form.Item>

            <Form.Item 
              name="version" 
              label="版本号" 
              rules={[
                { required: true, message: '请输入版本号' },
                { pattern: /^\d+\.\d+\.\d+$/, message: '版本号格式必须为 X.Y.Z' }
              ]}
            >
              <Input placeholder="例如：1.0.0" />
            </Form.Item>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
              <Text strong>SKILL.md 内容</Text>
              <Space>
                <Text type="secondary">预览</Text>
                <Switch checked={preview} onChange={setPreview} />
              </Space>
            </div>

            <Form.Item 
              name="skillMarkdown" 
              rules={[{ required: true, message: '请输入 SKILL.md 内容' }]}
            >
              {preview ? (
                <div style={{ 
                  padding: 16, 
                  background: '#fafafa', 
                  borderRadius: 6, 
                  minHeight: 300, 
                  whiteSpace: 'pre-wrap', 
                  fontFamily: 'monospace',
                  fontSize: 13,
                  border: '1px solid #d9d9d9',
                }}>
                  {form.getFieldValue('skillMarkdown') || '内容预览将显示在这里...'}
                </div>
              ) : (
                <TextArea 
                  rows={15} 
                  style={{ fontFamily: 'monospace', fontSize: 13 }}
                  placeholder="在此输入 SKILL.md 内容..."
                />
              )}
            </Form.Item>

            <Form.Item name="resources" label="资源文件 (JSON 格式)">
              <TextArea 
                rows={5} 
                placeholder='例如：{"scripts/analyze.py": "#!/usr/bin/env python3..."}'
                style={{ fontFamily: 'monospace' }}
              />
            </Form.Item>

            {drawerMode === 'create' && (
              <Form.Item name="publish" valuePropName="checked">
                <Switch checkedChildren="立即发布" unCheckedChildren="保存为草稿" />
              </Form.Item>
            )}
          </Form>
        )}
      </Drawer>
    </div>
  );
};

export default SkillLibrary;

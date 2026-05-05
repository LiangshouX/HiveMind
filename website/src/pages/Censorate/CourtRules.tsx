import React, { useState, useEffect, useCallback } from 'react';
import { Typography, List, Switch, Button, Input, Card, Row, Col, message, Space, Spin } from 'antd';
import { SaveOutlined, ReloadOutlined, FileMarkdownOutlined, EyeOutlined, EditOutlined, CloudUploadOutlined, CloudDownloadOutlined } from '@ant-design/icons';
import ReactMarkdown from 'react-markdown';
import { profileApi } from '../../services/profileApi';
import type { ProfileFile } from '../../types/profile';

const { Title, Text } = Typography;
const { TextArea } = Input;

const CourtRules: React.FC = () => {
  const [activeFile, setActiveFile] = useState<string>('SOUL.md');
  const [files, setFiles] = useState<Record<string, ProfileFile>>({});
  const [preview, setPreview] = useState(true);
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);

  // 加载 Profile 列表
  const loadProfiles = useCallback(async () => {
    setFetching(true);
    try {
      const profiles = await profileApi.listProfiles();
      const filesMap: Record<string, ProfileFile> = {};
      profiles.forEach((profile) => {
        filesMap[profile.filename] = profile;
      });
      setFiles(filesMap);
    } catch (error) {
      message.error('加载 Profile 失败');
    } finally {
      setFetching(false);
    }
  }, []);

  useEffect(() => {
    loadProfiles();
  }, [loadProfiles]);

  const currentFile = files[activeFile];

  const handleSave = async () => {
    if (!currentFile) return;
    
    setLoading(true);
    try {
      await profileApi.updateProfile(activeFile, {
        filename: activeFile,
        content: currentFile.content,
        enabled: currentFile.enabled,
      });
      message.success(`${activeFile} 已保存`);
      await loadProfiles(); // 重新加载
    } catch (error) {
      message.error('保存失败');
    } finally {
      setLoading(false);
    }
  };

  const handleContentChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    if (!currentFile) return;
    
    setFiles({
      ...files,
      [activeFile]: { ...currentFile, content: e.target.value }
    });
  };

  const toggleFileActive = async (key: string, checked: boolean) => {
    const file = files[key];
    if (!file) return;

    try {
      await profileApi.updateProfile(key, {
        filename: key,
        content: file.content,
        enabled: checked,
      });
      setFiles({
        ...files,
        [key]: { ...file, enabled: checked }
      });
      message.success(`${key} ${checked ? '已启用' : '已禁用'}`);
    } catch (error) {
      message.error('更新状态失败');
    }
  };

  const handleReset = async () => {
    if (!activeFile) return;
    
    try {
      await profileApi.resetProfile(activeFile);
      message.success(`${activeFile} 已重置为默认值`);
      await loadProfiles();
    } catch (error) {
      message.error('重置失败');
    }
  };

  const handleRefresh = () => {
    loadProfiles();
    message.info('已刷新');
  };

  const handleUpload = async () => {
    // TODO: 实现文件上传功能
    message.info('文件上传功能待实现');
  };

  const handleDownload = async () => {
    if (!currentFile) return;
    
    try {
      const blob = await profileApi.downloadProfile(activeFile);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = activeFile;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      message.success(`${activeFile} 已下载`);
    } catch (error) {
      message.error('下载失败');
    }
  };

  if (fetching) {
    return (
      <div style={{ height: 'calc(100vh - 120px)', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <Spin size="large" tip="加载中..." />
      </div>
    );
  }

  return (
    <div style={{ height: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>工作区</Title>
        <Space>
          <Button icon={<CloudUploadOutlined />} onClick={handleUpload}>上传</Button>
          <Button icon={<CloudDownloadOutlined />} onClick={handleDownload}>下载</Button>
        </Space>
      </div>

      <Row gutter={16} style={{ flex: 1, overflow: 'hidden' }}>
        {/* Left Sidebar: File List */}
        <Col span={6} style={{ height: '100%', overflowY: 'auto' }}>
          <Card 
            title="核心文件" 
            extra={<Button type="text" icon={<ReloadOutlined />} onClick={handleRefresh}>刷新</Button>} 
            bodyStyle={{ padding: 0 }}
          >
            <List
              itemLayout="horizontal"
              dataSource={Object.keys(files)}
              renderItem={(key) => {
                const file = files[key];
                if (!file) return null;
                
                return (
                  <List.Item
                    style={{
                      padding: '12px 16px',
                      cursor: 'pointer',
                      background: activeFile === key ? '#e6f7ff' : 'transparent',
                      borderLeft: activeFile === key ? '3px solid #1890ff' : '3px solid transparent'
                    }}
                    onClick={() => setActiveFile(key)}
                    actions={[
                      <Switch
                        size="small"
                        checked={file.enabled}
                        onChange={(checked) => toggleFileActive(key, checked)}
                        onClick={(_, e) => e.stopPropagation()}
                      />
                    ]}
                  >
                    <List.Item.Meta
                      avatar={<FileMarkdownOutlined style={{ fontSize: 20, color: file.enabled ? '#52c41a' : '#d9d9d9' }} />}
                      title={<span style={{ fontWeight: activeFile === key ? 'bold' : 'normal' }}>{key}</span>}
                      description={
                        <Space direction="vertical" size={0}>
                          <span style={{ fontSize: 12 }}>{file.size} · {new Date(file.updatedAt).toLocaleString('zh-CN')}</span>
                          <span style={{ fontSize: 11, color: '#999' }}>
                            {file.source === 'DEFAULT' ? '默认' : '自定义'}
                          </span>
                        </Space>
                      }
                    />
                  </List.Item>
                );
              }}
            />
          </Card>
        </Col>

        {/* Right Content: Editor */}
        <Col span={18} style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
          <Card
            title={activeFile}
            extra={
              <Space>
                <Button icon={<ReloadOutlined />} onClick={handleReset}>重置</Button>
                <Button type="primary" icon={<SaveOutlined />} loading={loading} onClick={handleSave} disabled={!currentFile}>
                  保存
                </Button>
              </Space>
            }
            bodyStyle={{ flex: 1, display: 'flex', flexDirection: 'column', height: 'calc(100% - 57px)', padding: 0 }}
            style={{ height: '100%', display: 'flex', flexDirection: 'column' }}
          >
            <div style={{ padding: '8px 16px', borderBottom: '1px solid #f0f0f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Text strong>内容</Text>
              <Space>
                <Text type="secondary" style={{ fontSize: 12 }}>预览</Text>
                <Switch
                  checkedChildren={<EyeOutlined />}
                  unCheckedChildren={<EditOutlined />}
                  checked={preview}
                  onChange={setPreview}
                />
              </Space>
            </div>

            <div style={{ flex: 1, overflow: 'auto', padding: 16 }}>
              {currentFile ? (
                preview ? (
                  <div style={{ lineHeight: 1.6 }}>
                    <ReactMarkdown
                      components={{
                        a: ({ node, ...props }) => {
                          void node;
                          return <a {...props} target="_blank" rel="noreferrer" />;
                        },
                      }}
                    >
                      {currentFile.content}
                    </ReactMarkdown>
                  </div>
                ) : (
                  <TextArea
                    value={currentFile.content}
                    onChange={handleContentChange}
                    style={{ height: '100%', resize: 'none', fontFamily: 'monospace', border: 'none', boxShadow: 'none' }}
                    placeholder="在此输入 Markdown 内容..."
                  />
                )
              ) : (
                <div style={{ textAlign: 'center', color: '#999', padding: '40px 0' }}>
                  文件不存在
                </div>
              )}
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default CourtRules;

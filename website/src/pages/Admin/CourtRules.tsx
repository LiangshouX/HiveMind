import React, { useState, useEffect, useCallback } from 'react';
import { Typography, Switch, Button, Input, Card, Row, Col, message, Space, Spin, Collapse, Empty } from 'antd';
import {
  SaveOutlined, ReloadOutlined, FileMarkdownOutlined, EyeOutlined, EditOutlined,
  CloudUploadOutlined, CloudDownloadOutlined, FolderOutlined, FileTextOutlined,
  DatabaseOutlined, DownOutlined, RightOutlined
} from '@ant-design/icons';
import ReactMarkdown from 'react-markdown';
import { profileApi } from '../../services/profileApi';
import { memoryApi } from '../../services/memoryApi';
import type { ProfileFile } from '../../types/profile';

const { Title, Text } = Typography;
const { TextArea } = Input;

/** 文件分类 */
interface FileCategory {
  key: string;
  label: string;
  icon: React.ReactNode;
  files: string[];
  defaultExpanded?: boolean;
}

const CourtRules: React.FC = () => {
  // 核心文件状态
  const [activeFile, setActiveFile] = useState<string>('SOUL.md');
  const [activeCategory, setActiveCategory] = useState<string>('core');
  const [files, setFiles] = useState<Record<string, ProfileFile>>({});
  const [preview, setPreview] = useState(true);
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);

  // 记忆文件状态
  const [memoryFiles, setMemoryFiles] = useState<string[]>([]);
  const [memoryContent, setMemoryContent] = useState<string>('');
  const [memoryLoading, setMemoryLoading] = useState(false);
  const [memoryPath, setMemoryPath] = useState<string>('');
  const [currentMemoryFile, setCurrentMemoryFile] = useState<string>('');
  const [memoryEditContent, setMemoryEditContent] = useState<string>('');

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

  // 加载记忆文件列表
  const loadMemoryFiles = useCallback(async (path: string = '') => {
    try {
      const fileList = await memoryApi.listFiles(path);
      setMemoryFiles(fileList);
    } catch (error) {
      message.error('加载记忆文件失败');
    }
  }, []);

  useEffect(() => {
    loadProfiles();
    loadMemoryFiles();
  }, [loadProfiles, loadMemoryFiles]);

  const currentFile = files[activeFile];

  // 保存核心文件
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
      await loadProfiles();
    } catch (error) {
      message.error('保存失败');
    } finally {
      setLoading(false);
    }
  };

  // 保存记忆文件
  const handleSaveMemory = async () => {
    if (!currentMemoryFile || !memoryContent) return;

    setMemoryLoading(true);
    try {
      await memoryApi.editFile({
        path: currentMemoryFile,
        oldText: memoryContent,
        newText: memoryEditContent,
      });
      setMemoryContent(memoryEditContent);
      message.success(`${currentMemoryFile} 已保存`);
    } catch (error) {
      message.error('保存失败');
    } finally {
      setMemoryLoading(false);
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
    loadMemoryFiles(memoryPath);
    message.info('已刷新');
  };

  const handleUpload = async () => {
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

  // 点击记忆文件
  const handleMemoryFileClick = async (fileName: string) => {
    const fullPath = memoryPath ? `${memoryPath}/${fileName}` : fileName;
    setCurrentMemoryFile(fullPath);
    setActiveCategory('memory');
    setActiveFile('');

    setMemoryLoading(true);
    try {
      const content = await memoryApi.readFile(fullPath);
      setMemoryContent(content);
      setMemoryEditContent(content);
    } catch (error) {
      message.error('读取记忆文件失败');
    } finally {
      setMemoryLoading(false);
    }
  };

  // 点击记忆目录
  const handleMemoryDirClick = (dirName: string) => {
    const newPath = memoryPath ? `${memoryPath}/${dirName}` : dirName;
    setMemoryPath(newPath);
    loadMemoryFiles(newPath);
  };

  // 返回上级目录
  const handleBackDir = () => {
    const parts = memoryPath.split('/');
    parts.pop();
    const newPath = parts.join('/');
    setMemoryPath(newPath);
    loadMemoryFiles(newPath);
  };

  // 点击核心文件
  const handleCoreFileClick = (key: string) => {
    setActiveFile(key);
    setActiveCategory('core');
    setCurrentMemoryFile('');
  };

  // 判断文件名是否是目录（简单判断：没有 .md 后缀且不是已知文件名）
  const isDirectory = (name: string): boolean => {
    return !name.includes('.');
  };

  // 分类配置
  const categories: FileCategory[] = [
    {
      key: 'core',
      label: '核心文件',
      icon: <FileMarkdownOutlined />,
      files: Object.keys(files),
      defaultExpanded: true,
    },
    {
      key: 'memory',
      label: '记忆',
      icon: <DatabaseOutlined />,
      files: memoryFiles,
      defaultExpanded: false,
    },
  ];

  if (fetching) {
    return (
      <div style={{ height: 'calc(100vh - 120px)', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <Spin size="large" tip="加载中..." />
      </div>
    );
  }

  // 获取当前激活的内容
  const isMemoryMode = activeCategory === 'memory' && currentMemoryFile;
  const currentDisplayContent = isMemoryMode ? memoryEditContent : currentFile?.content;
  const currentDisplayFile = isMemoryMode ? currentMemoryFile : activeFile;

  return (
    <div style={{ height: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>工作区</Title>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={handleRefresh}>刷新</Button>
          <Button icon={<CloudUploadOutlined />} onClick={handleUpload}>上传</Button>
          <Button icon={<CloudDownloadOutlined />} onClick={handleDownload} disabled={!!isMemoryMode}>下载</Button>
        </Space>
      </div>

      <Row gutter={16} style={{ flex: 1, overflow: 'hidden' }}>
        {/* Left Sidebar: File List */}
        <Col span={6} style={{ height: '100%', overflowY: 'auto' }}>
          <Collapse
            defaultActiveKey={['core', 'memory']}
            ghost
            expandIcon={({ isActive }) => isActive ? <DownOutlined /> : <RightOutlined />}
            items={categories.map(category => ({
              key: category.key,
              label: (
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
                  <Space>
                    {category.icon}
                    <span style={{ fontWeight: 600 }}>{category.label}</span>
                    <Text type="secondary" style={{ fontSize: 12 }}>({category.files.length})</Text>
                  </Space>
                  <Button
                    type="text"
                    size="small"
                    icon={<ReloadOutlined />}
                    onClick={(e) => {
                      e.stopPropagation();
                      if (category.key === 'core') {
                        loadProfiles();
                      } else {
                        loadMemoryFiles(memoryPath);
                      }
                      message.info('已刷新');
                    }}
                  />
                </div>
              ),
              children: (
                <div style={{ padding: '0 8px' }}>
                  {/* 记忆目录导航 */}
                  {category.key === 'memory' && memoryPath && (
                    <div
                      style={{
                        padding: '8px 12px',
                        cursor: 'pointer',
                        color: '#1890ff',
                        borderBottom: '1px solid #f0f0f0',
                        marginBottom: 4,
                      }}
                      onClick={handleBackDir}
                    >
                      <Space>
                        <FolderOutlined />
                        <span>..</span>
                      </Space>
                    </div>
                  )}

                  {/* 当前路径提示 */}
                  {category.key === 'memory' && memoryPath && (
                    <div style={{ padding: '4px 12px', marginBottom: 4 }}>
                      <Text type="secondary" style={{ fontSize: 11 }}>
                        📂 {memoryPath || '/'}
                      </Text>
                    </div>
                  )}

                  {/* 文件列表 */}
                  {category.files.length > 0 ? (
                    category.files.map((fileName) => {
                      const isActive = category.key === 'core'
                        ? activeFile === fileName && activeCategory === 'core'
                        : currentMemoryFile === (memoryPath ? `${memoryPath}/${fileName}` : fileName);

                      return (
                        <div
                          key={fileName}
                          style={{
                            padding: '10px 12px',
                            cursor: 'pointer',
                            background: isActive ? 'rgba(24, 144, 255, 0.08)' : 'transparent',
                            borderLeft: isActive ? '3px solid #1890ff' : '3px solid transparent',
                            borderRadius: '0 4px 4px 0',
                            marginBottom: 2,
                            transition: 'all 0.2s',
                          }}
                          onClick={() => {
                            if (category.key === 'core') {
                              handleCoreFileClick(fileName);
                            } else if (category.key === 'memory') {
                              if (isDirectory(fileName)) {
                                handleMemoryDirClick(fileName);
                              } else {
                                handleMemoryFileClick(fileName);
                              }
                            }
                          }}
                          onMouseEnter={(e) => {
                            if (!isActive) {
                              e.currentTarget.style.background = 'rgba(0, 0, 0, 0.02)';
                            }
                          }}
                          onMouseLeave={(e) => {
                            if (!isActive) {
                              e.currentTarget.style.background = 'transparent';
                            }
                          }}
                        >
                          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                            <Space size={8}>
                              {category.key === 'memory' && isDirectory(fileName) ? (
                                <FolderOutlined style={{ color: '#faad14', fontSize: 16 }} />
                              ) : (
                                <FileTextOutlined style={{ color: isActive ? '#1890ff' : '#999', fontSize: 16 }} />
                              )}
                              <span style={{ fontWeight: isActive ? 600 : 400, fontSize: 13 }}>
                                {fileName}
                              </span>
                            </Space>

                            {/* 核心文件的启用/禁用开关 */}
                            {category.key === 'core' && files[fileName] && (
                              <Switch
                                size="small"
                                checked={files[fileName].enabled}
                                onChange={(checked) => toggleFileActive(fileName, checked)}
                                onClick={(_, e) => e.stopPropagation()}
                              />
                            )}
                          </div>

                          {/* 核心文件的额外信息 */}
                          {category.key === 'core' && files[fileName] && (
                            <div style={{ marginTop: 4, marginLeft: 24 }}>
                              <Text type="secondary" style={{ fontSize: 11 }}>
                                {files[fileName].size} · {new Date(files[fileName].updatedAt).toLocaleString('zh-CN')}
                              </Text>
                            </div>
                          )}
                        </div>
                      );
                    })
                  ) : (
                    <Empty
                      image={Empty.PRESENTED_IMAGE_SIMPLE}
                      description={category.key === 'memory' ? '暂无记忆文件' : '暂无文件'}
                      style={{ margin: '16px 0' }}
                    />
                  )}
                </div>
              ),
            }))}
          />
        </Col>

        {/* Right Content: Editor */}
        <Col span={18} style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
          <Card
            title={
              <Space>
                {isMemoryMode ? <DatabaseOutlined /> : <FileMarkdownOutlined />}
                <span>{currentDisplayFile || '未选择文件'}</span>
              </Space>
            }
            extra={
              <Space>
                {!isMemoryMode && (
                  <Button icon={<ReloadOutlined />} onClick={handleReset} disabled={!activeFile}>重置</Button>
                )}
                <Button
                  type="primary"
                  icon={<SaveOutlined />}
                  loading={isMemoryMode ? memoryLoading : loading}
                  onClick={isMemoryMode ? handleSaveMemory : handleSave}
                  disabled={!currentDisplayFile}
                >
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
              {currentDisplayFile ? (
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
                      {currentDisplayContent || ''}
                    </ReactMarkdown>
                  </div>
                ) : (
                  <TextArea
                    value={isMemoryMode ? memoryEditContent : currentFile?.content}
                    onChange={(e) => {
                      if (isMemoryMode) {
                        setMemoryEditContent(e.target.value);
                      } else {
                        handleContentChange(e);
                      }
                    }}
                    style={{ height: '100%', resize: 'none', fontFamily: 'monospace', border: 'none', boxShadow: 'none' }}
                    placeholder="在此输入 Markdown 内容..."
                  />
                )
              ) : (
                <div style={{ textAlign: 'center', color: '#999', padding: '40px 0' }}>
                  <FileMarkdownOutlined style={{ fontSize: 48, marginBottom: 16 }} />
                  <div>请从左侧选择文件</div>
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

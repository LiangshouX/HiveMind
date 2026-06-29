import { SendOutlined } from "@ant-design/icons";
import { Button } from "antd";

interface ChatSendButtonProps {
  disabled?: boolean;
  loading?: boolean;
  onClick?: () => void | Promise<void>;
}

/**
 * 消息发送按钮 - "发送"
 */
export function ChatSendButton({ 
  disabled = false, 
  loading = false,
  onClick 
}: ChatSendButtonProps) {
  const hasContent = !disabled && !loading;
  
  return (
    <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center' }}>
      <Button
        type="primary"
        icon={<SendOutlined />}
        onClick={() => void onClick?.()}
        disabled={disabled || loading}
        loading={loading}
        style={{
          background: hasContent ? 'var(--td-primary)' : 'var(--td-bg-base)',
          borderColor: hasContent ? 'var(--td-primary)' : 'var(--td-border-light)',
          color: hasContent ? 'var(--td-text-inverse)' : 'var(--td-text-tertiary)',
          borderRadius: '8px',
          boxShadow: hasContent ? 'var(--td-shadow-primary)' : 'none',
          height: '40px',
          padding: '0 24px',
          fontWeight: 600,
          letterSpacing: '1px',
          transition: 'all 0.3s ease'
        }}
      >
        发送
      </Button>
    </div>
  );
}

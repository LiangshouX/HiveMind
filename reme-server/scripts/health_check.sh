# scripts/health_check.sh
#!/bin/bash
PORT="${HTTP_PORT:-8002}"
URL="http://127.0.0.1:${PORT}/health"

echo "🏥 Checking health endpoint: $URL"

# 尝试 3 次，每次间隔 5 秒
for i in {1..3}; do
    if curl -sf "$URL" >/dev/null 2>&1; then
        echo "✅ Service healthy (attempt $i)"
        # 可选：解析响应体
        # curl -s "$URL" | jq .
        exit 0
    fi
    echo "⏳ Waiting... (attempt $i/3)"
    sleep 5
done

echo "❌ Health check failed after 3 attempts"
exit 1
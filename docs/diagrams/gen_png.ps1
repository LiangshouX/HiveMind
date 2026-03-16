$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Drawing

function New-DiagramPng {
  param(
    [Parameter(Mandatory = $true)][string]$Path,
    [Parameter(Mandatory = $true)][string]$Title,
    [Parameter(Mandatory = $true)][string[]]$Lines
  )

  $w = 1200
  $h = 700
  $bmp = New-Object System.Drawing.Bitmap($w, $h)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.SmoothingMode = 'HighQuality'
  $g.Clear([System.Drawing.Color]::White)

  $titleFont = New-Object System.Drawing.Font('Segoe UI', 28, [System.Drawing.FontStyle]::Bold)
  $bodyFont = New-Object System.Drawing.Font('Segoe UI', 16)

  $g.DrawString($Title, $titleFont, [System.Drawing.Brushes]::Black, 40, 40)

  $y = 110
  foreach ($l in $Lines) {
    $g.DrawString($l, $bodyFont, [System.Drawing.Brushes]::Black, 40, $y)
    $y += 28
  }

  $g.DrawString('对应 .drawio 为可编辑原图，可用 draw.io 导出高清 PNG。', $bodyFont, [System.Drawing.Brushes]::Gray, 40, ($h - 70))

  $dir = Split-Path -Parent $Path
  if (!(Test-Path $dir)) {
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
  }

  $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
  $g.Dispose()
  $bmp.Dispose()
}

New-DiagramPng -Path 'd:\Code\Java\TangDynasty\docs\diagrams\system_architecture.png' -Title '系统架构图（预览）' -Lines @(
  'Website → Spring Boot Backend → MySQL/Redis',
  'Backend 通过 Adapter 零改动集成 CoPaw 与 Edict',
  'Kafka 作为可选领域事件总线，Redis Pub/Sub 用于 WS 推送'
)

New-DiagramPng -Path 'd:\Code\Java\TangDynasty\docs\diagrams\sequence_mainflow.png' -Title '主流程时序图（预览）' -Lines @(
  '1) 用户下旨/聊天',
  '2) 后端落库并发布任务事件',
  '3) 编排器派发 → 调用 CoPaw/OpenClaw',
  '4) 输出 thoughts/heartbeat → WS 推送前端'
)

New-DiagramPng -Path 'd:\Code\Java\TangDynasty\docs\diagrams\er.png' -Title 'ER 图（预览）' -Lines @(
  '核心：sys_task ↔ sys_task_log ↔ sys_memorial',
  '聊天：sys_conversation ↔ sys_message',
  '配置：sys_official/sys_model/sys_skill/sys_tool/sys_mcp/sys_channel/sys_config/sys_token_usage'
)


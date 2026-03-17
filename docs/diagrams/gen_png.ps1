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

  $g.DrawString('Editable source: .drawio (export high-res PNG via draw.io).', $bodyFont, [System.Drawing.Brushes]::Gray, 40, ($h - 70))

  $dir = Split-Path -Parent $Path
  if (!(Test-Path $dir)) {
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
  }

  $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
  $g.Dispose()
  $bmp.Dispose()
}

New-DiagramPng -Path 'd:\Code\Java\TangDynasty\docs\diagrams\system_architecture.png' -Title 'System Architecture (Preview)' -Lines @(
  'Website -> Spring Boot Backend -> MySQL/Redis',
  'Backend integrates CoPaw and Edict via adapters (no changes).',
  'Kafka optional for domain events; Redis Pub/Sub for WS push.'
)

New-DiagramPng -Path 'd:\Code\Java\TangDynasty\docs\diagrams\sequence_mainflow.png' -Title 'Main Flow Sequence (Preview)' -Lines @(
  '1) User creates edict or chats',
  '2) Backend persists and publishes task events',
  '3) Orchestrator dispatches and calls CoPaw/OpenClaw',
  '4) Thoughts/heartbeat -> WS push to UI'
)

New-DiagramPng -Path 'd:\Code\Java\TangDynasty\docs\diagrams\er.png' -Title 'ER Diagram (Preview)' -Lines @(
  'Core: sys_task <-> sys_task_log <-> sys_memorial',
  'Chat: sys_conversation <-> sys_message',
  'Config: sys_official/sys_model/sys_skill/sys_tool/sys_mcp/sys_channel/sys_config/sys_token_usage'
)

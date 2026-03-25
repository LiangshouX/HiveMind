Param(
    [string]$ServerHost = "0.0.0.0",
    [int]$Port = 8090,
    [string]$EnvName = "reme-serve",
    [string]$PythonVersion = "3.12"
)

$ErrorActionPreference = "Stop"

function Test-Conda {
    try { conda --version *> $null; return $true } catch { return $false }
}

# 确保在工程根目录执行（脚本位于 scripts/）
if ($PSScriptRoot) {
    Set-Location (Join-Path $PSScriptRoot "..")
}

$UsingConda = Test-Conda

if ($UsingConda) {
    $envList = conda env list | Out-String
    if ($envList -notmatch "(^|\s)$EnvName(\s|$)") {
        conda create -y -n $EnvName "python=$PythonVersion"
    }
    try {
        $CondaBase = (conda info --base).Trim()
        $HookPs1 = Join-Path $CondaBase "shell\condabin\conda-hook.ps1"
        if (Test-Path $HookPs1) { & $HookPs1 }
    } catch {}
    conda activate $EnvName
} else {
    if (-not (Test-Path ".venv")) {
        py -$PythonVersion -m venv .venv
    }
    . .\.venv\Scripts\Activate.ps1
}

python -m pip install -U pip setuptools wheel
pip install -e .

if (-not $env:REME_WORKING_DIR) { $env:REME_WORKING_DIR = ".reme" }
if (-not $env:REME_LLM_BACKEND) { $env:REME_LLM_BACKEND = "openai" }
if (-not $env:REME_LLM_MODEL_NAME) { $env:REME_LLM_MODEL_NAME = "qwen3.5-plus" }
if (-not $env:REME_EMBEDDING_BACKEND) { $env:REME_EMBEDDING_BACKEND = "openai" }
if (-not $env:REME_EMBEDDING_MODEL_NAME) { $env:REME_EMBEDDING_MODEL_NAME = "text-embedding-v4" }
if (-not $env:REME_EMBEDDING_DIMENSIONS) { $env:REME_EMBEDDING_DIMENSIONS = "1024" }
if (-not $env:REME_VECTOR_BACKEND) { $env:REME_VECTOR_BACKEND = "chroma" }
if (-not $env:REME_CHROMA_HOST) { $env:REME_CHROMA_HOST = "127.0.0.1" }
if (-not $env:REME_CHROMA_PORT) { $env:REME_CHROMA_PORT = "8000" }
if (-not $env:REME_CHROMA_COLLECTION) { $env:REME_CHROMA_COLLECTION = "reme" }
if (-not $env:REME_AUTO_START) { $env:REME_AUTO_START = "1" }

if (-not $env:REME_SERVER_HOST) { $env:REME_SERVER_HOST = $ServerHost }
if (-not $env:REME_SERVER_PORT) { $env:REME_SERVER_PORT = "$Port" }

if (-not $env:OPENAI_API_KEY) {
    Write-Host "Warning: No OPENAI_API_KEY detected, make sure you have set up or switched to a different model provider in your system environment"
}

reme-server --host $ServerHost --port $Port
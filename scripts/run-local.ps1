<#
.SYNOPSIS
  Start the full ClearFund stack (PostgreSQL + backend) via Docker Compose.

.DESCRIPTION
  Flyway applies the V1-V3 migrations on backend startup.

.EXAMPLE
  ./scripts/run-local.ps1
  ./scripts/run-local.ps1 -Detach
#>
[CmdletBinding()]
param(
    [switch]$Detach
)

$ErrorActionPreference = "Stop"

$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $RootDir

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Error "Docker does not appear to be running. Start Docker and retry."
    exit 1
}

Write-Host "Starting ClearFund (backend on http://localhost:8080)..."
if ($Detach) {
    docker compose up --build --detach
} else {
    docker compose up --build
}

<#
.SYNOPSIS
  Run the ClearFund test suite. Extra arguments are passed through to Maven.

.EXAMPLE
  ./scripts/run-tests.ps1
  ./scripts/run-tests.ps1 -Dtest=OrderStatusTest
  ./scripts/run-tests.ps1 '-Dtest=!OrderLifecycleIntegrationTest'
#>
[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs
)

$ErrorActionPreference = "Stop"

$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $RootDir

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host "NOTE: Docker not available - the Testcontainers integration test will be skipped."
}

Write-Host "Running tests..."
mvn -B test @MavenArgs

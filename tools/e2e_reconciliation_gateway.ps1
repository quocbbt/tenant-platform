param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$TenantCode = "demo",
    [string]$Username = "demo.owner",
    [string]$Password = "Demo@123"
)

$ErrorActionPreference = "Stop"

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers,
        [object]$Body = $null
    )

    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri $Url -Headers $Headers -TimeoutSec 30
    }

    return Invoke-RestMethod -Method $Method -Uri $Url -Headers $Headers -Body ($Body | ConvertTo-Json -Depth 8) -TimeoutSec 30
}

Write-Host "[1/8] Setup password (idempotent)..."
try {
    Invoke-Api -Method "POST" -Url "$BaseUrl/api/auth/setup-password" -Headers @{
        "X-Tenant-Code" = $TenantCode
        "Content-Type"  = "application/json"
    } -Body @{
        username = $Username
        password = $Password
    } | Out-Null
} catch {
}

Write-Host "[2/8] Login..."
$login = Invoke-Api -Method "POST" -Url "$BaseUrl/api/auth/login" -Headers @{
    "X-Tenant-Code" = $TenantCode
    "Content-Type"  = "application/json"
} -Body @{
    username = $Username
    password = $Password
}
$accessToken = $login.data.accessToken
if ([string]::IsNullOrWhiteSpace($accessToken)) {
    throw "Login failed: accessToken is empty"
}

$authHeaders = @{
    "X-Tenant-Code" = $TenantCode
    "Authorization" = "Bearer $accessToken"
    "Content-Type"  = "application/json"
}

Write-Host "[3/8] Create order..."
$order = Invoke-Api -Method "POST" -Url "$BaseUrl/api/logiflow/orders" -Headers $authHeaders -Body @{
    receiverName    = "E2E Receiver"
    receiverAddress = "Ho Chi Minh City"
    codAmount       = 123456
}
$orderId = $order.data.id
if ([string]::IsNullOrWhiteSpace($orderId)) {
    throw "Create order failed: orderId is empty"
}

Write-Host "[4/8] Update COD to COLLECTED..."
Invoke-Api -Method "POST" -Url "$BaseUrl/api/logiflow/orders/$orderId/cod" -Headers $authHeaders -Body @{
    amount = 123456
    status = "COLLECTED"
    note   = "e2e collected"
} | Out-Null

Write-Host "[5/8] Read eligible COD list..."
$eligible = Invoke-Api -Method "GET" -Url "$BaseUrl/api/logiflow/reconciliations/eligible-cod?page=0&size=50" -Headers @{
    "X-Tenant-Code" = $TenantCode
    "Authorization" = "Bearer $accessToken"
}
$items = @($eligible.data.items)
if ($items.Count -eq 0) {
    throw "Eligible COD list is empty"
}
$codRecordId = $items[0].id
if ([string]::IsNullOrWhiteSpace($codRecordId)) {
    throw "Eligible COD item has empty id"
}

Write-Host "[6/8] Create reconciliation..."
$createRec = Invoke-Api -Method "POST" -Url "$BaseUrl/api/logiflow/reconciliations" -Headers $authHeaders -Body @{
    codRecordIds = @($codRecordId)
    note         = "e2e reconciliation create"
}
$reconciliationId = $createRec.data.id
if ([string]::IsNullOrWhiteSpace($reconciliationId)) {
    throw "Create reconciliation failed: reconciliationId is empty"
}

Write-Host "[7/8] Update reconciliation status to RECONCILED..."
$updateRec = Invoke-Api -Method "PATCH" -Url "$BaseUrl/api/logiflow/reconciliations/$reconciliationId/status" -Headers $authHeaders -Body @{
    status = "RECONCILED"
    note   = "e2e reconciliation done"
}
if ($updateRec.data.status -ne "RECONCILED") {
    throw "Expected reconciliation status RECONCILED, got: $($updateRec.data.status)"
}

Write-Host "[8/8] Validate COD summary..."
$summary = Invoke-Api -Method "GET" -Url "$BaseUrl/api/logiflow/operations/cod/summary" -Headers @{
    "X-Tenant-Code" = $TenantCode
    "Authorization" = "Bearer $accessToken"
}
if ($summary.data.reconciledAmount -le 0) {
    throw "Expected reconciledAmount > 0, got: $($summary.data.reconciledAmount)"
}

$result = [pscustomobject]@{
    tenantCode        = $TenantCode
    orderId           = $orderId
    codRecordId       = $codRecordId
    reconciliationId  = $reconciliationId
    reconciliation    = $updateRec.data.status
    reconciledAmount  = $summary.data.reconciledAmount
}

Write-Host "E2E SUCCESS"
$result | ConvertTo-Json -Compress

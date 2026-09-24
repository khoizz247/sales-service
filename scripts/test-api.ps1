param(
    [string]$BaseUrl = 'http://localhost:18080'
)

$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
$checks = 0

function Call-Api {
    param(
        [string]$Method,
        [string]$Path,
        [int]$Expected,
        [string]$Token,
        [object]$Body
    )
    $headers = @{}
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }
    $requestParameters = @{ Uri = "$BaseUrl$Path"; Method = $Method; Headers = $headers; UseBasicParsing = $true }
    if ($null -ne $Body) {
        $requestParameters['ContentType'] = 'application/json; charset=utf-8'
        $requestParameters['Body'] = ConvertTo-Json -InputObject $Body -Depth 10 -Compress
    }
    try {
        $response = Invoke-WebRequest @requestParameters
        $status = [int]$response.StatusCode
        $data = if ($response.Content) { $response.Content | ConvertFrom-Json } else { $null }
    } catch {
        if ($null -eq $_.Exception.Response) { throw }
        $status = [int]$_.Exception.Response.StatusCode
        $data = $null
    }
    if ($status -ne $Expected) {
        throw "$Method ${Path}: expected HTTP $Expected, got $status"
    }
    $script:checks++
    return $data
}

function Assert-Equal {
    param([object]$Actual, [object]$Expected, [string]$Message)
    if ($Actual -ne $Expected) { throw "$Message`: expected $Expected, got $Actual" }
    $script:checks++
}

function New-Product {
    param([string]$Token, [int]$Stock, [int]$Price)
    $sku = 'TEST-' + [guid]::NewGuid().ToString('N').Substring(0, 12)
    return Call-Api POST '/api/products' 201 $Token @{
        sku = $sku; name = 'Test product'; description = 'Automated test'
        price = $Price; stockQuantity = $Stock
    }
}

$admin = Call-Api POST '/api/auth/login' 200 $null @{
    email = 'admin@example.com'; password = 'Admin@123'
}
$adminToken = $admin.accessToken
$customerEmail = 'auto-' + [guid]::NewGuid().ToString('N').Substring(0, 12) + '@example.com'
$customer = Call-Api POST '/api/auth/register' 201 $null @{
    fullName = 'Test customer'; email = $customerEmail; password = 'Test@1234'
}
$customerToken = $customer.accessToken

# 1. Customer cannot mutate products; 2. Admin can.
$managed = New-Product $adminToken 6 10000
$newSku = 'TEST-' + [guid]::NewGuid().ToString('N').Substring(0, 12)
Call-Api POST '/api/products' 403 $customerToken @{
    sku = $newSku; name = 'Forbidden product'; price = 10000; stockQuantity = 1
} | Out-Null
Call-Api PUT "/api/products/$($managed.id)" 403 $customerToken @{
    name = 'Forbidden update'; price = 10000; stockQuantity = 6
} | Out-Null
Call-Api DELETE "/api/products/$($managed.id)" 403 $customerToken $null | Out-Null
$changed = Call-Api PUT "/api/products/$($managed.id)" 200 $adminToken @{
    name = 'Updated product'; price = 12000; stockQuantity = 6
}
Assert-Equal $changed.name 'Updated product' 'Admin update product'
$changed = Call-Api PATCH "/api/products/$($managed.id)/stock" 200 $adminToken @{
    stockQuantity = 5
}
Assert-Equal $changed.stockQuantity 5 'Admin update stock'
Call-Api DELETE "/api/products/$($managed.id)" 204 $adminToken $null | Out-Null
Call-Api GET "/api/products/$($managed.id)" 404 $null $null | Out-Null

# 3-7 and 10. Order stock, oversell, cancellation, idempotence, status, total.
$product = New-Product $adminToken 10 10000
$secondProduct = New-Product $adminToken 5 3000
$orderBody = @{
    recipientName = 'Test buyer'; recipientPhone = '0901234567'
    shippingAddress = '1 Test Street'
    items = @(
        @{ productId = $product.id; quantity = 2 },
        @{ productId = $secondProduct.id; quantity = 3 }
    )
}
$order = Call-Api POST '/api/orders' 201 $customerToken $orderBody
Assert-Equal ([decimal]$order.totalAmount) ([decimal]29000) 'Server-calculated total'
Assert-Equal ([decimal]$order.items[0].lineTotal) ([decimal]20000) 'First line total'
Assert-Equal ([decimal]$order.items[1].lineTotal) ([decimal]9000) 'Second line total'
$afterOrder = Call-Api GET "/api/products/$($product.id)" 200 $null $null
Assert-Equal $afterOrder.stockQuantity 8 'Stock after order'
$secondAfterOrder = Call-Api GET "/api/products/$($secondProduct.id)" 200 $null $null
Assert-Equal $secondAfterOrder.stockQuantity 2 'Second stock after order'
$tooMany = @{
    recipientName = 'Test buyer'; recipientPhone = '0901234567'
    shippingAddress = '1 Test Street'
    items = @(@{ productId = $product.id; quantity = 9 })
}
Call-Api POST '/api/orders' 409 $customerToken $tooMany | Out-Null
Call-Api PATCH "/api/admin/orders/$($order.id)/status" 409 $adminToken @{
    status = 'COMPLETED'
} | Out-Null
Call-Api PATCH "/api/admin/orders/$($order.id)/status" 200 $adminToken @{
    status = 'CANCELLED'
} | Out-Null
Call-Api PATCH "/api/admin/orders/$($order.id)/status" 200 $adminToken @{
    status = 'CANCELLED'
} | Out-Null
$afterCancel = Call-Api GET "/api/products/$($product.id)" 200 $null $null
Assert-Equal $afterCancel.stockQuantity 10 'Stock after repeated cancellation'
$secondAfterCancel = Call-Api GET "/api/products/$($secondProduct.id)" 200 $null $null
Assert-Equal $secondAfterCancel.stockQuantity 5 'Second stock after cancellation'
$movements = @(Call-Api GET "/api/admin/products/$($product.id)/inventory" 200 $adminToken $null)
$reversals = @($movements | Where-Object { $_.type -eq 'SALE_REVERSAL' })
Assert-Equal $reversals.Count 1 'Exactly one stock reversal'

# 8-9. Missing JWT and wrong role.
Call-Api GET '/api/users/me' 401 $null $null | Out-Null
Call-Api GET '/api/admin/orders' 403 $customerToken $null | Out-Null
Call-Api POST '/api/orders' 403 $adminToken $orderBody | Out-Null

# Other important read APIs and customer ownership.
$profile = Call-Api GET '/api/users/me/profile' 200 $customerToken $null
$me = Call-Api GET '/api/users/me' 200 $customerToken $null
Assert-Equal $profile.userId $me.id 'Customer profile belongs to registration'
$mine = @((Call-Api GET '/api/orders/me' 200 $customerToken $null).items)
if (@($mine | Where-Object { $_.id -eq $order.id }).Count -ne 1) {
    throw 'Created order missing from customer order list'
}
$checks++
Call-Api GET "/api/orders/$($order.id)/history" 200 $customerToken $null | Out-Null
Call-Api GET "/api/orders/$($order.id)/payments" 200 $customerToken $null | Out-Null

$secondEmail = 'auto-' + [guid]::NewGuid().ToString('N').Substring(0, 12) + '@example.com'
$secondCustomer = Call-Api POST '/api/auth/register' 201 $null @{
    fullName = 'Other customer'; email = $secondEmail; password = 'Test@1234'
}
Call-Api GET "/api/orders/$($order.id)" 404 $secondCustomer.accessToken $null | Out-Null
Call-Api GET "/api/orders/$($order.id)/payments" 404 $secondCustomer.accessToken $null | Out-Null

$address = Call-Api POST '/api/users/me/addresses' 201 $customerToken @{
    label = 'Home'; recipientName = 'Test buyer'; recipientPhone = '0901234567'
    addressLine1 = '1 Test Street'; city = 'Ha Noi'; countryCode = 'VN'; isDefault = $true
}
Call-Api DELETE "/api/users/me/addresses/$($address.id)" 404 $secondCustomer.accessToken $null | Out-Null
Call-Api DELETE "/api/users/me/addresses/$($address.id)" 204 $customerToken $null | Out-Null

Write-Host "PASS: $checks API/status/value checks on $BaseUrl"

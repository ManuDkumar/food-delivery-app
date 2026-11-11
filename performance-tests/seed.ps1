# ============================================================
# seed.ps1 — fill the dev DB with test data, via the REST API
#
# What it creates (unique per run, thanks to a timestamp suffix):
#   - 1 restaurant owner user  + 2 restaurants
#   - 8 menu items per restaurant  (16 total, active + available)
#   - 1 new customer with 3 cart items (restaurant #2)
#   - hero@test.com's cart emptied then loaded with 3 cart items
#
# Run it like this (from the project root):
#   powershell -ExecutionPolicy Bypass -File .\performance-tests\seed.ps1
#
# Why via the API and not raw SQL: it uses the app's own validation,
# so the data is guaranteed shaped exactly like real usage.
# ============================================================

$ErrorActionPreference = "Continue"
$base = "http://localhost:8081"
$sfx  = (Get-Date -Format "yyyyMMddHHmmss")           # suffix → re-runs never collide
$ownerEmail    = "seed$sfx-owner@test.com"
$customerEmail = "seed$sfx-user@test.com"
$pwdCommon     = "Abcd@999!"
# phones must be ^[6-9]\d{9}$ (exactly 10 digits) → build them from the last 8 digits
$phoneOwnerHolder = if ($sfx.Length -ge 8) { "98" + $sfx.Substring($sfx.Length - 8) } else { "98$sfx" }
$phoneCustomer    = if ($sfx.Length -ge 8) { "78" + $sfx.Substring($sfx.Length - 8) } else { "78$sfx" }

function J($h) { return $h | ConvertTo-Json -Compress }

function Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }

# --- helper: fat-finger-proof request, returns body or error msg -------------
function Rest($method, $url, $token, $bodyObj) {
    $headers = @{}
    if ($token) { $headers.Authorization = "Bearer $token" }
    try {
        if ($bodyObj) {
            Invoke-RestMethod -Method $method -Uri $url -Headers $headers `
                -ContentType "application/json" -Body (J $bodyObj)
        } else {
            Invoke-RestMethod -Method $method -Uri $url -Headers $headers
        }
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        return @{ error = "$code $($_.ErrorDetails.Message)" }
    }
}

Step "1. Register owner ($ownerEmail) + login"
Rest Post "$base/api/v1/auth/restaurant/register" $null @{
    name = "Seed Owner"; email = $ownerEmail; password = $pwdCommon; phone = $phoneOwnerHolder
} | Out-Null
$owner = Rest Post "$base/api/v1/auth/login" $null @{
    email = $ownerEmail; password = $pwdCommon
}
$ownerToken = $owner.token

Step "2. Owner creates 2 restaurants"
$rA = Rest Post "$base/api/v1/restaurants/create" $ownerToken @{
    name = "Seed Kitchen A $sfx"; description = "seeded"; address = "Seed Road $sfx"; phone = $phoneOwnerHolder
}
$rB = Rest Post "$base/api/v1/restaurants/create" $ownerToken @{
    name = "Seed Kitchen B $sfx"; description = "seeded"; address = "Seed Lane $sfx"; phone = "97" + $sfx.Substring($sfx.Length - 8)
}
Write-Host " restaurants: $($rA.restaurantName) / $($rB.restaurantName)"

Step "3. 8 menu items per restaurant (active + available by default)"
$itemsA = @(); $itemsB = @()
1..8 | ForEach-Object {
    $n = $_
    $itemsA += Rest Post "$base/api/v1/restaurants/$($rA.id)/menu" $ownerToken @{
        name = "Seed Dish A$n $sfx"; description = "seeded item"; price = 99.50; veg = $true
    }
    $itemsB += Rest Post "$base/api/v1/restaurants/$($rB.id)/menu" $ownerToken @{
        name = "Seed Dish B$n $sfx"; description = "seeded item"; price = 149.50; veg = $false
    }
}
Write-Host " menu A: $($itemsA.Count) items, menu B: $($itemsB.Count) items"

Step "4. New customer + 3 items in cart (from restaurant B)"
Rest Post "$base/api/v1/auth/customer/register" $null @{
    name = "Seed Customer"; email = $customerEmail; password = $pwdCommon; phone = $phoneCustomer
} | Out-Null
$cust = Rest Post "$base/api/v1/auth/login" $null @{ email = $customerEmail; password = $pwdCommon }
2..4 | ForEach-Object {
    Rest Post "$base/api/v1/cart/items" $cust.token @{
        menuItemId = $itemsB[$_-1].id; quantity = 1
    }
} | Out-Null
Write-Host " cart for $customerEmail loaded (3 items)"

Step "5. hero@test.com: clear stale cart, then load 3 items (from restaurant A)"
$hero = Rest Post "$base/api/v1/auth/login" $null @{ email = "hero@test.com"; password = "Abcd@1234" }
$null = Rest Delete "$base/api/v1/cart/items" $hero.token
0..2 | ForEach-Object {
    Rest Post "$base/api/v1/cart/items" $hero.token @{
        menuItemId = $itemsA[$_].id; quantity = 2
    }
} | Out-Null

Step "6. Verify"
$heroCart = Rest Get "$base/api/v1/cart" $hero.token
$myCart   = Rest Get "$base/api/v1/cart" $cust.token
Write-Host (" hero cart    : {0} items, total {1}" -f $heroCart.items.Count, $heroCart.total)
Write-Host (" $customerEmail cart : {0} items, total {1}" -f $myCart.items.Count, $myCart.total)
$all    = Rest Get "$base/api/v1/restaurants" $null
Write-Host (" restaurants  : {0} total in DB" -f $all.Count)

Write-Host "`n== SEED DONE ==" -ForegroundColor Green
Write-Host " logins to remember for later:"
Write-Host "   owner    : $ownerEmail / $pwdCommon"
Write-Host "   customer : $customerEmail / $pwdCommon"

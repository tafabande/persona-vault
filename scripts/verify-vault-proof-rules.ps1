# Run with npx --yes firebase-tools@13.35.1 emulators:exec --project demo-persona-proof --only firestore,auth "powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-vault-proof-rules.ps1"
$ErrorActionPreference = 'Stop'
$project = 'demo-persona-proof'
$authBody = @{returnSecureToken=$true} | ConvertTo-Json
$first = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fixture-key' -ContentType 'application/json' -Body $authBody
$second = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fixture-key' -ContentType 'application/json' -Body $authBody
$base = "http://127.0.0.1:8080/v1/projects/$project/databases/(default)/documents/accounts/$($first.localId)/vault/fixture-card/proofs/fixture-proof"
$headers = @{Authorization="Bearer $($first.idToken)"}
$body = @{fields=@{itemId=@{stringValue='fixture-card'};isDeleted=@{booleanValue=$false}}} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Patch -Uri $base -Headers $headers -ContentType 'application/json' -Body $body | Out-Null
$chunk = @{fields=@{data=@{bytesValue='AQID'}}} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Patch -Uri "$base/chunks/0" -Headers $headers -ContentType 'application/json' -Body $chunk | Out-Null
Invoke-RestMethod -Method Get -Uri "$base/chunks/0" -Headers $headers | Out-Null
foreach ($blockedHeaders in @(@{Authorization="Bearer $($second.idToken)"}, @{})) {
    foreach ($method in @('Get','Patch','Delete')) {
        $request = @{Method=$method;Uri="$base/chunks/0";Headers=$blockedHeaders}
        if ($method -eq 'Patch') { $request.ContentType='application/json'; $request.Body=$chunk }
        $denied = $false
        try { Invoke-RestMethod @request | Out-Null }
        catch { if ([int]$_.Exception.Response.StatusCode -eq 403) {$denied=$true} else {throw} }
        if (-not $denied) {throw 'Proof rules allowed unauthorized access'}
    }
}
Invoke-RestMethod -Method Delete -Uri "$base/chunks/0" -Headers $headers | Out-Null
Write-Output 'Proof owner read/write/delete passed. Other-account and anonymous operations denied.'


$parent = $base.Replace('/proofs/fixture-proof','')
$deleted = @{fields=@{id=@{stringValue='fixture-card'};personId=@{stringValue='owner'};isDeleted=@{booleanValue=$true};updatedAt=@{integerValue='123'}}} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Patch -Uri $parent -Headers $headers -ContentType 'application/json' -Body $deleted | Out-Null
foreach ($method in @('Patch','Delete')) {
    $request = @{Method=$method;Uri=$parent;Headers=$headers}
    if ($method -eq 'Patch') { $request.ContentType='application/json'; $request.Body=$body }
    $denied=$false
    try {Invoke-RestMethod @request | Out-Null}
    catch {if ([int]$_.Exception.Response.StatusCode -eq 403) {$denied=$true} else {throw}}
    if (-not $denied) {throw 'A stale client removed a deletion marker'}
}
Invoke-RestMethod -Method Patch -Uri $parent -Headers $headers -ContentType 'application/json' -Body $deleted | Out-Null
Write-Output 'Vault deletion markers reject stale recreation and removal, and permit idempotent retry.'

# Account-owned media is private until every chunk has been uploaded and its manifest published.
function Assert-AccountMediaDenied($method, $uri, $requestHeaders, $requestBody) {
    $request = @{Method=$method;Uri=$uri;Headers=$requestHeaders}
    if ($method -eq 'Patch') { $request.ContentType='application/json'; $request.Body=$requestBody }
    $denied=$false
    try {Invoke-RestMethod @request | Out-Null}
    catch {if ([int]$_.Exception.Response.StatusCode -eq 403) {$denied=$true} else {throw}}
    if (-not $denied) {throw 'Account media rules allowed a forbidden operation'}
}
$media = "http://127.0.0.1:8080/v1/projects/$project/databases/(default)/documents/accounts/$($first.localId)/media/549df0fa-5c57-4980-9d44-2fbd3c1a9833"
$mediaChunk = @{fields=@{data=@{bytesValue='AQID'}}} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Patch -Uri "$media/chunks/0" -Headers $headers -ContentType 'application/json' -Body $mediaChunk | Out-Null
Assert-AccountMediaDenied 'Get' "$media/chunks/0" $headers $null
$manifest = @{fields=@{ownerUid=@{stringValue=$first.localId};storageVersion=@{integerValue='1'};cipherSize=@{integerValue='3'};cipherHash=@{stringValue='039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81'};chunkCount=@{integerValue='1'};isDeleted=@{booleanValue=$false}}} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Patch -Uri $media -Headers $headers -ContentType 'application/json' -Body $manifest | Out-Null
Invoke-RestMethod -Method Get -Uri "$media/chunks/0" -Headers $headers | Out-Null
foreach ($blockedHeaders in @(@{Authorization="Bearer $($second.idToken)"}, @{})) {
    foreach ($method in @('Get','Patch','Delete')) {
        Assert-AccountMediaDenied $method $media $blockedHeaders $manifest
        Assert-AccountMediaDenied $method "$media/chunks/0" $blockedHeaders $mediaChunk
    }
}
Assert-AccountMediaDenied 'Patch' "$media/chunks/0" $headers $mediaChunk
Assert-AccountMediaDenied 'Delete' "$media/chunks/0" $headers $null
Assert-AccountMediaDenied 'Delete' $media $headers $null
$mediaDeleted = @{fields=@{ownerUid=@{stringValue=$first.localId};isDeleted=@{booleanValue=$true};chunkCount=@{integerValue='1'}}} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Patch -Uri $media -Headers $headers -ContentType 'application/json' -Body $mediaDeleted | Out-Null
Assert-AccountMediaDenied 'Get' "$media/chunks/0" $headers $null
Assert-AccountMediaDenied 'Patch' "$media/chunks/0" $headers $mediaChunk
Assert-AccountMediaDenied 'Patch' $media $headers $manifest
Assert-AccountMediaDenied 'Delete' $media $headers $null
Invoke-RestMethod -Method Delete -Uri "$media/chunks/0" -Headers $headers | Out-Null
Invoke-RestMethod -Method Delete -Uri "$media/chunks/0" -Headers $headers | Out-Null
Invoke-RestMethod -Method Patch -Uri $media -Headers $headers -ContentType 'application/json' -Body $mediaDeleted | Out-Null
$oversized = @{fields=@{data=@{bytesValue=[Convert]::ToBase64String((New-Object byte[] 524289))}}} | ConvertTo-Json -Depth 5
Assert-AccountMediaDenied 'Patch' "$media-too-large/chunks/0" $headers $oversized
Write-Output 'Account media publication, owner isolation, immutable chunks, bounded size and deletion retry passed.'

$documentUri = "http://127.0.0.1:8080/v1/projects/$project/databases/(default)/documents/accounts/$($first.localId)/documents/fixture-document"
$protectedHeader = @{fields=@{documentId=@{stringValue='fixture-document'};personId=@{stringValue='owner'};metadataFormatVersion=@{integerValue='2'};metadataCiphertext=@{stringValue='fixture-cipher'};metadataIv=@{stringValue='00'}}} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Patch -Uri $documentUri -Headers $headers -ContentType 'application/json' -Body $protectedHeader | Out-Null
$versionUri = "$documentUri/versions/fixture-version"
$protectedVersion = @{fields=@{documentId=@{stringValue='fixture-document'};versionId=@{stringValue='fixture-version'};metadataFormatVersion=@{integerValue='2'};metadataCiphertext=@{stringValue='fixture-cipher'};metadataIv=@{stringValue='00'}}} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Patch -Uri $versionUri -Headers $headers -ContentType 'application/json' -Body $protectedVersion | Out-Null
foreach ($target in @($documentUri, $versionUri)) {
    $plain = @{fields=@{title=@{stringValue='Plaintext regression'};notes=@{stringValue='Private metadata'}}} | ConvertTo-Json -Depth 5
    $denied=$false
    try {Invoke-RestMethod -Method Patch -Uri $target -Headers $headers -ContentType 'application/json' -Body $plain | Out-Null}
    catch {if ([int]$_.Exception.Response.StatusCode -eq 403) {$denied=$true} else {throw}}
    if (-not $denied) {throw 'A stale client downgraded encrypted document metadata'}
    foreach ($blockedHeaders in @(@{Authorization="Bearer $($second.idToken)"}, @{})) {
        $denied=$false
        try {Invoke-RestMethod -Method Get -Uri $target -Headers $blockedHeaders | Out-Null}
        catch {if ([int]$_.Exception.Response.StatusCode -eq 403) {$denied=$true} else {throw}}
        if (-not $denied) {throw 'Document metadata crossed account boundaries'}
    }
}
$docDeleted = @{fields=@{documentId=@{stringValue='fixture-document'};personId=@{stringValue='owner'};isDeleted=@{booleanValue=$true};updatedAt=@{integerValue='123'}}} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Patch -Uri $documentUri -Headers $headers -ContentType 'application/json' -Body $docDeleted | Out-Null
foreach ($method in @('Patch','Delete')) {
    $request = @{Method=$method;Uri=$documentUri;Headers=$headers}
    if ($method -eq 'Patch') {$request.ContentType='application/json';$request.Body=$protectedHeader}
    $denied=$false
    try {Invoke-RestMethod @request | Out-Null}
    catch {if ([int]$_.Exception.Response.StatusCode -eq 403) {$denied=$true} else {throw}}
    if (-not $denied) {throw 'A stale client removed the document deletion marker'}
}
Invoke-RestMethod -Method Patch -Uri $documentUri -Headers $headers -ContentType 'application/json' -Body $docDeleted | Out-Null
$denied=$false
try {Invoke-RestMethod -Method Patch -Uri $versionUri -Headers $headers -ContentType 'application/json' -Body $protectedVersion | Out-Null}
catch {if ([int]$_.Exception.Response.StatusCode -eq 403) {$denied=$true} else {throw}}
if (-not $denied) {throw 'A stale client restored a version under a deleted document'}
Write-Output 'Document metadata encryption guards, account boundaries and deletion markers passed.'

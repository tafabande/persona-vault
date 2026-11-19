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

# Wallpaper metadata and deletion markers have the same account-owner boundary as photos.
function Assert-WallpaperDenied($method, $uri, $requestHeaders, $requestBody) {
    $request = @{Method=$method;Uri=$uri;Headers=$requestHeaders}
    if ($method -eq 'Patch') {
        $request.ContentType = 'application/json'
        $request.Body = $requestBody
    }
    try {
        Invoke-RestMethod @request | Out-Null
    } catch {
        if ([int]$_.Exception.Response.StatusCode -eq 403) { return }
        throw
    }
    throw "Wallpaper rules allowed forbidden $method access to $uri"
}
$accountBase = "http://127.0.0.1:8080/v1/projects/$project/databases/(default)/documents/accounts/$($first.localId)"
foreach ($collectionName in @('wallpapers', 'wallpaper_tombstones')) {
    $collectionUri = "$accountBase/$collectionName"
    $wallpaperUri = "$collectionUri/fixture-wallpaper"
    $wallpaperMetadata = @{fields=@{wallpaperId=@{stringValue='fixture-wallpaper'};updatedAt=@{integerValue='1'}}} | ConvertTo-Json -Depth 5
    Invoke-RestMethod -Method Patch -Uri $wallpaperUri -Headers $headers -ContentType 'application/json' -Body $wallpaperMetadata | Out-Null
    Invoke-RestMethod -Method Get -Uri $wallpaperUri -Headers $headers | Out-Null
    $wallpaperList = Invoke-RestMethod -Method Get -Uri $collectionUri -Headers $headers
    if (@($wallpaperList.documents).Count -ne 1) { throw "Owner could not list $collectionName" }
    $wallpaperUpdate = @{fields=@{wallpaperId=@{stringValue='fixture-wallpaper'};updatedAt=@{integerValue='2'}}} | ConvertTo-Json -Depth 5
    Invoke-RestMethod -Method Patch -Uri $wallpaperUri -Headers $headers -ContentType 'application/json' -Body $wallpaperUpdate | Out-Null
    $updatedWallpaper = Invoke-RestMethod -Method Get -Uri $wallpaperUri -Headers $headers
    if ($updatedWallpaper.fields.updatedAt.integerValue -ne '2') { throw "Owner update failed for $collectionName" }
    foreach ($blockedHeaders in @(@{Authorization="Bearer $($second.idToken)"}, @{})) {
        Assert-WallpaperDenied 'Get' $collectionUri $blockedHeaders $null
        foreach ($method in @('Get', 'Patch', 'Delete')) {
            Assert-WallpaperDenied $method $wallpaperUri $blockedHeaders $wallpaperUpdate
        }
        Assert-WallpaperDenied 'Patch' "$collectionUri/forbidden-new-wallpaper" $blockedHeaders $wallpaperMetadata
    }
    Assert-WallpaperDenied 'Patch' "$wallpaperUri/unmatched/fixture" $headers $wallpaperMetadata
    Invoke-RestMethod -Method Delete -Uri $wallpaperUri -Headers $headers | Out-Null
    Write-Output "$collectionName owner create/read/list/update/delete passed. Other-account and anonymous access and unmatched subcollections denied."
}

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

# Retiring the historical UID-derived key envelope requires durable passphrase recovery.
function Assert-EscrowDenied($method, $uri, $requestHeaders, $requestBody) {
    $request = @{Method=$method;Uri=$uri;Headers=$requestHeaders}
    if ($method -eq 'Patch') {$request.ContentType='application/json';$request.Body=$requestBody}
    $denied=$false
    try {Invoke-RestMethod @request | Out-Null}
    catch {if ([int]$_.Exception.Response.StatusCode -eq 403) {$denied=$true} else {throw}}
    if (-not $denied) {throw 'Recovery escrow rules allowed a forbidden operation'}
}
$escrowUri = "http://127.0.0.1:8080/v1/projects/$project/databases/(default)/documents/accounts/$($first.localId)/vaultEscrow"
$weakEscrow = @{fields=@{isCiphertext=@{booleanValue=$true};wrappedKeyBase64=@{stringValue='historical-uid-wrap'};wrapIvBase64=@{stringValue='historical-iv'}}} | ConvertTo-Json -Depth 5
Assert-EscrowDenied 'Patch' "$escrowUri/account_sync_pfk" $headers $weakEscrow
# The emulator's admin token seeds legacy state that current clients cannot create.
Invoke-RestMethod -Method Patch -Uri "$escrowUri/account_sync_pfk" -Headers @{Authorization='Bearer owner'} -ContentType 'application/json' -Body $weakEscrow | Out-Null
Assert-EscrowDenied 'Patch' "$escrowUri/account_sync_pfk" $headers $weakEscrow
Assert-EscrowDenied 'Delete' "$escrowUri/account_sync_pfk" $headers $null

$incompleteEscrow = @{fields=@{keyId=@{stringValue='incomplete-recovery'};isCiphertext=@{booleanValue=$true}}} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Patch -Uri "$escrowUri/incomplete-recovery" -Headers $headers -ContentType 'application/json' -Body $incompleteEscrow | Out-Null
$incompletePointer = @{fields=@{keyId=@{stringValue='incomplete-recovery'};isCiphertext=@{booleanValue=$true}}} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Patch -Uri "$escrowUri/latest" -Headers $headers -ContentType 'application/json' -Body $incompletePointer | Out-Null
Assert-EscrowDenied 'Delete' "$escrowUri/account_sync_pfk" $headers $null

$strongEscrow = @{fields=@{keyId=@{stringValue='fixture-passphrase-recovery'};version=@{integerValue='1'};isCiphertext=@{booleanValue=$true};saltBase64=@{stringValue='AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA='};wrappedKeyBase64=@{stringValue='AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'};wrapIvBase64=@{stringValue='AAAAAAAAAAAAAAAA'};kdfAlgorithm=@{stringValue='PBKDF2WithHmacSHA256'};iterations=@{integerValue='210000'}}} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Patch -Uri "$escrowUri/fixture-passphrase-recovery" -Headers $headers -ContentType 'application/json' -Body $strongEscrow | Out-Null
Invoke-RestMethod -Method Get -Uri "$escrowUri/fixture-passphrase-recovery" -Headers $headers | Out-Null
Assert-EscrowDenied 'Delete' "$escrowUri/fixture-passphrase-recovery" $headers $null
Assert-EscrowDenied 'Delete' "$escrowUri/account_sync_pfk" $headers $null
$latestPointer = @{fields=@{keyId=@{stringValue='fixture-passphrase-recovery'};isCiphertext=@{booleanValue=$true}}} | ConvertTo-Json -Depth 5
$invalidPointer = @{fields=@{keyId=@{stringValue='fixture-passphrase-recovery'};isCiphertext=@{booleanValue=$false}}} | ConvertTo-Json -Depth 5
Assert-EscrowDenied 'Patch' "$escrowUri/latest" $headers $invalidPointer
Invoke-RestMethod -Method Patch -Uri "$escrowUri/latest" -Headers $headers -ContentType 'application/json' -Body $latestPointer | Out-Null
foreach ($blockedHeaders in @(@{Authorization="Bearer $($second.idToken)"}, @{})) {
    foreach ($method in @('Get','Patch','Delete')) {
        Assert-EscrowDenied $method "$escrowUri/fixture-passphrase-recovery" $blockedHeaders $strongEscrow
        Assert-EscrowDenied $method "$escrowUri/latest" $blockedHeaders $latestPointer
        Assert-EscrowDenied $method "$escrowUri/account_sync_pfk" $blockedHeaders $weakEscrow
    }
}
Invoke-RestMethod -Method Delete -Uri "$escrowUri/account_sync_pfk" -Headers $headers | Out-Null
Assert-EscrowDenied 'Patch' "$escrowUri/account_sync_pfk" $headers $weakEscrow
Assert-EscrowDenied 'Delete' "$escrowUri/latest" $headers $null
Assert-EscrowDenied 'Delete' "$escrowUri/fixture-passphrase-recovery" $headers $null
Write-Output 'Recovery escrow isolation, permanent passphrase protection and guarded legacy-envelope retirement passed.'

# File-key fingerprints are established once and survive partial account updates.
function Assert-FingerprintDenied($method, $uri, $requestHeaders, $requestBody) {
    $request = @{Method=$method;Uri=$uri;Headers=$requestHeaders}
    if ($method -eq 'Patch') {$request.ContentType='application/json';$request.Body=$requestBody}
    $denied=$false
    try {Invoke-RestMethod @request | Out-Null}
    catch {if ([int]$_.Exception.Response.StatusCode -eq 403) {$denied=$true} else {throw}}
    if (-not $denied) {throw 'Account file-key fingerprint rules allowed a forbidden operation'}
}
$fingerprintUri = "http://127.0.0.1:8080/v1/projects/$project/databases/(default)/documents/accounts/$($first.localId)"
$fingerprintValue = 'ab' * 32
$fingerprintAccount = @{fields=@{accountUid=@{stringValue=$first.localId};createdAt=@{integerValue='1'};fileKeyFingerprintV2=@{stringValue=$fingerprintValue}}} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Patch -Uri $fingerprintUri -Headers $headers -ContentType 'application/json' -Body $fingerprintAccount | Out-Null
$sparseAccount = @{fields=@{displayName=@{stringValue='Owner fixture'}}} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Patch -Uri "${fingerprintUri}?updateMask.fieldPaths=displayName" -Headers $headers -ContentType 'application/json' -Body $sparseAccount | Out-Null
$preservedAccount = Invoke-RestMethod -Method Get -Uri $fingerprintUri -Headers $headers
if ($preservedAccount.fields.fileKeyFingerprintV2.stringValue -ne $fingerprintValue) {throw 'A sparse account merge lost its file-key fingerprint'}
if ($preservedAccount.fields.displayName.stringValue -ne 'Owner fixture') {throw 'A sparse account merge was not applied'}
$replacementFingerprint = @{fields=@{accountUid=@{stringValue=$first.localId};createdAt=@{integerValue='1'};fileKeyFingerprintV2=@{stringValue=('cd' * 32)}}} | ConvertTo-Json -Depth 5
Assert-FingerprintDenied 'Patch' $fingerprintUri $headers $replacementFingerprint
$missingFingerprint = @{fields=@{accountUid=@{stringValue=$first.localId};createdAt=@{integerValue='1'}}} | ConvertTo-Json -Depth 5
Assert-FingerprintDenied 'Patch' $fingerprintUri $headers $missingFingerprint
$emptyAccount = @{fields=@{}} | ConvertTo-Json -Depth 5
Assert-FingerprintDenied 'Patch' "${fingerprintUri}?updateMask.fieldPaths=fileKeyFingerprintV2" $headers $emptyAccount
foreach ($blockedHeaders in @(@{Authorization="Bearer $($second.idToken)"}, @{})) {
    foreach ($method in @('Get','Patch','Delete')) {
        Assert-FingerprintDenied $method $fingerprintUri $blockedHeaders $fingerprintAccount
    }
}
# Existing accounts without a marker can register their initial valid fingerprint.
$legacyAccountUri = "http://127.0.0.1:8080/v1/projects/$project/databases/(default)/documents/accounts/$($second.localId)"
$legacyAccountHeaders = @{Authorization="Bearer $($second.idToken)"}
$legacyAccountBody = @{fields=@{accountUid=@{stringValue=$second.localId};createdAt=@{integerValue='1'}}} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Patch -Uri $legacyAccountUri -Headers $legacyAccountHeaders -ContentType 'application/json' -Body $legacyAccountBody | Out-Null
$initialFingerprint = @{fields=@{fileKeyFingerprintV2=@{stringValue=$fingerprintValue}}} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Patch -Uri "${legacyAccountUri}?updateMask.fieldPaths=fileKeyFingerprintV2" -Headers $legacyAccountHeaders -ContentType 'application/json' -Body $initialFingerprint | Out-Null
# Malformed values must also be rejected on the very first account creation.
$fingerprintFresh = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fixture-key' -ContentType 'application/json' -Body $authBody
$freshAccountUri = "http://127.0.0.1:8080/v1/projects/$project/databases/(default)/documents/accounts/$($fingerprintFresh.localId)"
$freshAccountHeaders = @{Authorization="Bearer $($fingerprintFresh.idToken)"}
foreach ($malformed in @('', 'ab', ('AB' * 32), ('gg' * 32), ('a' * 65))) {
    $malformedAccount = @{fields=@{accountUid=@{stringValue=$fingerprintFresh.localId};fileKeyFingerprintV2=@{stringValue=$malformed}}} | ConvertTo-Json -Depth 5
    Assert-FingerprintDenied 'Patch' $freshAccountUri $freshAccountHeaders $malformedAccount
}
$numericFingerprint = @{fields=@{accountUid=@{stringValue=$fingerprintFresh.localId};fileKeyFingerprintV2=@{integerValue='123'}}} | ConvertTo-Json -Depth 5
Assert-FingerprintDenied 'Patch' $freshAccountUri $freshAccountHeaders $numericFingerprint
Write-Output 'Account fingerprints validate initial registration, preserve sparse merges and reject replacement, removal and unauthorized access.'

$base = 'http://localhost:8081'

function Call-Api {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [string]$Token,
        $Body
    )

    $headers = @{}
    if ($Token) {
        $headers['Authorization'] = "Bearer $Token"
    }

    $requestBody = ''
    if ($null -ne $Body) {
        $requestBody = $Body | ConvertTo-Json -Depth 20 -Compress
    }

    try {
        if ($requestBody) {
            $resp = Invoke-WebRequest -UseBasicParsing -Method $Method -Uri ($base + $Path) -Headers $headers -ContentType 'application/json' -Body $requestBody -TimeoutSec 30
        } else {
            $resp = Invoke-WebRequest -UseBasicParsing -Method $Method -Uri ($base + $Path) -Headers $headers -TimeoutSec 30
        }
        return [pscustomobject]@{
            name = $Name
            method = $Method
            path = $Path
            request = $requestBody
            status = [int]$resp.StatusCode
            response = $resp.Content
        }
    } catch {
        if ($_.Exception.Response) {
            $r = $_.Exception.Response
            $txt = ''
            try {
                $sr = New-Object IO.StreamReader($r.GetResponseStream())
                $txt = $sr.ReadToEnd()
            } catch {}
            return [pscustomobject]@{
                name = $Name
                method = $Method
                path = $Path
                request = $requestBody
                status = [int]$r.StatusCode
                response = $txt
            }
        }
        return [pscustomobject]@{
            name = $Name
            method = $Method
            path = $Path
            request = $requestBody
            status = -1
            response = $_.Exception.Message
        }
    }
}

$out = @()

$loginBody = @{ email = 'admin@testmgmt.com'; password = 'Admin@123' }
$login = Call-Api -Name 'auth.login' -Method 'POST' -Path '/api/v1/auth/login' -Token $null -Body $loginBody
$out += $login
if ($login.status -ne 200) {
    $out | ConvertTo-Json -Depth 8
    exit 0
}

$token = ($login.response | ConvertFrom-Json).token

$out += Call-Api -Name 'users.profile.get' -Method 'GET' -Path '/api/v1/users/profile' -Token $token -Body $null
$out += Call-Api -Name 'users.profile.put' -Method 'PUT' -Path '/api/v1/users/profile' -Token $token -Body @{ fullName = 'System Admin'; team = 'Platform' }

$projectCode = 'P' + (Get-Random -Minimum 1000 -Maximum 9999)
$projCreate = Call-Api -Name 'projects.create' -Method 'POST' -Path '/api/v1/projects' -Token $token -Body @{ code = $projectCode; name = "Project $projectCode"; description = 'api sweep' }
$out += $projCreate
$projectId = ($projCreate.response | ConvertFrom-Json).projectId

$out += Call-Api -Name 'projects.list' -Method 'GET' -Path '/api/v1/projects' -Token $token -Body $null
$out += Call-Api -Name 'projects.get' -Method 'GET' -Path ("/api/v1/projects/$projectId") -Token $token -Body $null
$out += Call-Api -Name 'projects.update' -Method 'PUT' -Path ("/api/v1/projects/$projectId") -Token $token -Body @{ name = "Project ${projectCode} Updated"; description = 'updated' }

$tcCreate = Call-Api -Name 'testcases.create' -Method 'POST' -Path '/api/v1/testcases' -Token $token -Body @{
    title = 'TC Login'
    description = 'Verify login'
    preconditions = 'user active'
    projectId = $projectId
    priority = 'HIGH'
    steps = @(
        @{ stepNumber = 1; action = 'Open login'; expectedResult = 'Login visible' },
        @{ stepNumber = 2; action = 'Submit valid creds'; expectedResult = 'Dashboard' }
    )
}
$out += $tcCreate
$testCaseId = ($tcCreate.response | ConvertFrom-Json).testCaseId

$out += Call-Api -Name 'testcases.list' -Method 'GET' -Path ("/api/v1/testcases?projectId=$projectId&page=0&size=20") -Token $token -Body $null
$out += Call-Api -Name 'testcases.get' -Method 'GET' -Path ("/api/v1/testcases/$testCaseId") -Token $token -Body $null
$out += Call-Api -Name 'testcases.update' -Method 'PUT' -Path ("/api/v1/testcases/$testCaseId") -Token $token -Body @{
    title = 'TC Login Updated'
    description = 'desc2'
    preconditions = 'pre'
    priority = 'CRITICAL'
    status = 'READY'
    steps = @(@{ stepNumber = 1; action = 'Open app'; expectedResult = 'ok' })
}

$planCreate = Call-Api -Name 'testplans.create' -Method 'POST' -Path '/api/v1/testplans' -Token $token -Body @{
    name = 'Plan API'
    projectId = $projectId
    description = 'plan'
    sprintId = 'SPR-API'
    startDate = '2026-05-09'
    endDate = '2026-05-30'
    testCaseIds = @($testCaseId)
}
$out += $planCreate
$planId = ($planCreate.response | ConvertFrom-Json).planId

$out += Call-Api -Name 'testplans.list' -Method 'GET' -Path ("/api/v1/testplans?projectId=$projectId&page=0&size=20") -Token $token -Body $null
$out += Call-Api -Name 'testplans.get' -Method 'GET' -Path ("/api/v1/testplans/$planId") -Token $token -Body $null
$out += Call-Api -Name 'testplans.addTestcases' -Method 'POST' -Path ("/api/v1/testplans/$planId/testcases") -Token $token -Body @{ testCaseIds = @($testCaseId) }

$cycleCreate = Call-Api -Name 'testplans.addCycle' -Method 'POST' -Path ("/api/v1/testplans/$planId/cycles") -Token $token -Body @{ name = 'Cycle API'; environment = 'UAT' }
$out += $cycleCreate
$cycleId = ($cycleCreate.response | ConvertFrom-Json).cycleId

$out += Call-Api -Name 'testplans.cycles' -Method 'GET' -Path ("/api/v1/testplans/$planId/cycles") -Token $token -Body $null

$runCreate = Call-Api -Name 'testruns.create' -Method 'POST' -Path '/api/v1/testruns' -Token $token -Body @{
    testPlanId = $planId
    cycleId = $cycleId
    environment = 'UAT'
    buildVersion = '1.0.1'
}
$out += $runCreate
$runId = ($runCreate.response | ConvertFrom-Json).runId

$out += Call-Api -Name 'testruns.get' -Method 'GET' -Path ("/api/v1/testruns/$runId") -Token $token -Body $null

$exec = Call-Api -Name 'testruns.execute' -Method 'PUT' -Path ("/api/v1/testruns/$runId/execute") -Token $token -Body @{
    testCaseId = $testCaseId
    status = 'PASSED'
    actualResult = 'passed'
    comment = 'ok'
    durationSecs = 7
}
$out += $exec
$executionId = ($exec.response | ConvertFrom-Json).executionId

$defCreate = Call-Api -Name 'defects.create' -Method 'POST' -Path '/api/v1/defects' -Token $token -Body @{
    projectId = $projectId
    title = 'Defect API'
    description = 'found'
    severity = 'HIGH'
    priority = 'P2'
    linkedTestRun = $runId
    linkedTestCase = $testCaseId
    linkedExecutionId = $executionId
    environment = 'UAT'
    buildVersion = '1.0.1'
    module = 'Auth'
}
$out += $defCreate
$defectId = ($defCreate.response | ConvertFrom-Json).defectId

$out += Call-Api -Name 'defects.list' -Method 'GET' -Path ("/api/v1/defects?projectId=$projectId&page=0&size=20") -Token $token -Body $null
$out += Call-Api -Name 'defects.status' -Method 'PATCH' -Path ("/api/v1/defects/$defectId/status") -Token $token -Body @{ status = 'OPEN'; comment = 'triaged' }
$out += Call-Api -Name 'defects.jiraSync' -Method 'POST' -Path ("/api/v1/defects/$defectId/jira-sync") -Token $token -Body $null

$out += Call-Api -Name 'reports.executionSummary' -Method 'GET' -Path ("/api/v1/reports/execution-summary?projectId=$projectId") -Token $token -Body $null
$out += Call-Api -Name 'reports.defectSummary' -Method 'GET' -Path ("/api/v1/reports/defect-summary?projectId=$projectId") -Token $token -Body $null
$out += Call-Api -Name 'reports.trends' -Method 'GET' -Path ("/api/v1/reports/trends?projectId=$projectId&metricType=defects&groupBy=week") -Token $token -Body $null

$out += Call-Api -Name 'exports.execution' -Method 'GET' -Path ("/api/v1/reports/execution-summary/export?runId=$runId&format=pdf") -Token $token -Body $null
$out += Call-Api -Name 'exports.defect' -Method 'GET' -Path ("/api/v1/reports/defect-summary/export?projectId=$projectId") -Token $token -Body $null

$tmpUserEmail = 'api' + (Get-Random -Minimum 1000 -Maximum 9999) + '@testmgmt.com'
$register = Call-Api -Name 'auth.register' -Method 'POST' -Path '/api/v1/auth/register' -Token $null -Body @{
    username = 'user' + (Get-Random -Minimum 1000 -Maximum 9999)
    email = $tmpUserEmail
    password = 'Test@12345'
    fullName = 'Temp User'
    team = 'QA'
}
$out += $register
$tmpUserId = ($register.response | ConvertFrom-Json).userId

$out += Call-Api -Name 'users.list' -Method 'GET' -Path '/api/v1/users' -Token $token -Body $null
$out += Call-Api -Name 'users.role' -Method 'PATCH' -Path ("/api/v1/users/$tmpUserId/role") -Token $token -Body @{ role = 'MANAGER' }
$out += Call-Api -Name 'users.deactivate' -Method 'DELETE' -Path ("/api/v1/users/$tmpUserId") -Token $token -Body $null

$out += Call-Api -Name 'auth.changePassword' -Method 'POST' -Path '/api/v1/auth/change-password' -Token $token -Body @{ currentPassword = 'Admin@123'; newPassword = 'Admin@123' }
$out += Call-Api -Name 'defects.jiraWebhook' -Method 'POST' -Path '/api/v1/defects/jira-webhook' -Token $token -Body @{ issue = @{ key = 'PROJ-1'; fields = @{ status = 'OPEN' } } }

$out += Call-Api -Name 'testcases.delete' -Method 'DELETE' -Path ("/api/v1/testcases/$testCaseId") -Token $token -Body $null
$out += Call-Api -Name 'projects.deactivate' -Method 'DELETE' -Path ("/api/v1/projects/$projectId") -Token $token -Body $null

$out | ConvertTo-Json -Depth 8

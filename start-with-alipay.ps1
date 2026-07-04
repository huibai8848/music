# =============================================================
# 音乐平台 - 支付宝沙箱支付启动脚本
# 从密钥文件读取并设置环境变量，然后启动后端
#
# 使用步骤：
# 1. 将支付宝沙箱的 应用私钥 保存到 应用私钥RSA2048-敏感数据，请妥善保管.txt
# 2. 将支付宝公钥保存到 支付宝公钥.txt
# 3. 启动 ngrok: .\ngrok.exe http 8888
# 4. 复制 ngrok 生成的公网地址，修改下面 $env:ALIPAY_NOTIFY_URL
# 5. 运行本脚本
# =============================================================

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  音乐平台 - 支付宝沙箱支付启动" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

# 停止可能存在的旧进程
Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 1

# 项目根目录
$ProjectRoot = "E:\07_Data\IdeaProjects\music"

# ---- 基础配置 ----
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:SERVER_PORT = "8888"
$env:ALIPAY_APP_ID = "9021000165631979"

# ---- 前端地址（支付完成后后端将浏览器重定向至此） ----
$env:ALIPAY_FRONTEND_URL = "http://localhost:5173"

# ---- ngrok 公网地址（支付宝异步通知回调） ----
# 启动 ngrok 后，将下方地址替换为 ngrok 生成的真实地址
# 注意：每次重启 ngrok 地址都会变化，需要同步更新
$env:ALIPAY_NOTIFY_URL = "https://elliptic-geography-varnish.ngrok-free.dev/api/payment/alipay/notify"

# ---- 读取密钥文件 ----
# 应用私钥（PKCS8 格式，从支付宝开放平台沙箱环境获取）
$privateKeyPath = Join-Path $ProjectRoot "应用私钥RSA2048-敏感数据，请妥善保管.txt"
$env:ALIPAY_MERCHANT_PRIVATE_KEY = Get-Content $privateKeyPath -Raw

# 支付宝公钥（从支付宝开放平台沙箱环境获取）
$publicKeyPath = Join-Path $ProjectRoot "支付宝公钥.txt"
$env:ALIPAY_PUBLIC_KEY = Get-Content $publicKeyPath -Raw

Write-Host "✅ APP_ID: 9021000165631979" -ForegroundColor Green
Write-Host "✅ 应用私钥: 已加载（" -NoNewline -ForegroundColor Green
Write-Host ($env:ALIPAY_MERCHANT_PRIVATE_KEY.Length.ToString() + " 字符") -NoNewline -ForegroundColor Yellow
Write-Host "）" -ForegroundColor Green
Write-Host "✅ 支付宝公钥: 已加载（" -NoNewline -ForegroundColor Green
Write-Host ($env:ALIPAY_PUBLIC_KEY.Length.ToString() + " 字符") -NoNewline -ForegroundColor Yellow
Write-Host "）" -ForegroundColor Green
Write-Host ""

Write-Host "正在启动后端..." -ForegroundColor Yellow

# 启动后端
$jarPath = Join-Path $ProjectRoot "target\music-0.0.1-SNAPSHOT.jar"
Start-Process -NoNewWindow -FilePath "java" -ArgumentList "-jar", $jarPath -RedirectStandardOutput (Join-Path $ProjectRoot "backend.log") -RedirectStandardError (Join-Path $ProjectRoot "backend_err.log")

Write-Host ""
Write-Host "⏳ 等待后端启动..." -ForegroundColor Yellow
Start-Sleep -Seconds 8

# 检查是否启动成功
$log = Get-Content (Join-Path $ProjectRoot "backend.log") -Tail 2
if ($log -match "Started MusicApplication") {
    Write-Host "✅ 后端启动成功！端口: 8888" -ForegroundColor Green
    Write-Host ""
    Write-Host "访问地址:" -ForegroundColor Cyan
    Write-Host "  http://localhost:5173  - 前端（需同时运行 npm run dev）" -ForegroundColor White
    Write-Host "  http://localhost:8888  - 后端" -ForegroundColor White
    Write-Host ""
    Write-Host "支付测试:" -ForegroundColor Cyan
    Write-Host "  登录后打开 http://localhost:5173/profile 点击「升级会员」" -ForegroundColor White
} else {
    Write-Host "⚠️ 后端似乎还未就绪，检查 backend.log 中..." -ForegroundColor Yellow
    Get-Content (Join-Path $ProjectRoot "backend.log") -Tail 5
}

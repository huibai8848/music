# =============================================================
# ngrok 内网穿透启动脚本
# 将本地后端服务（端口 8888）暴露到公网
# 用于支付宝沙箱异步通知回调
# =============================================================

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  音乐平台 - ngrok 内网穿透" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "正在启动 ngrok，将 localhost:8888 映射到公网..." -ForegroundColor Yellow
Write-Host ""

# 启动 ngrok
.\ngrok.exe http 8888

# 启动后，在 ngrok 控制台可以看到公网地址，例如：
#   https://xxxx-xx-xx-xx-xx.ngrok.io
#
# 复制这个地址，然后更新 application.yml 中的：
#   alipay.notify-url = https://xxxx-xx-xx-xx-xx.ngrok.io/api/payment/alipay/notify
#   alipay.return-url = https://xxxx-xx-xx-xx-xx.ngrok.io/api/payment/alipay/return
#
# 或者设置环境变量：
#   $env:ALIPAY_NOTIFY_URL="https://xxxx-xx-xx-xx-xx.ngrok.io/api/payment/alipay/notify"
#   $env:ALIPAY_RETURN_URL="https://xxxx-xx-xx-xx-xx.ngrok.io/api/payment/alipay/return"
#
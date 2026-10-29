# Android ISO-DEP / HCE 测试

仅用于验证手机是否能够以 Android HCE 方式模拟一个自定义 ISO-DEP 应用。**不复制校园卡、UID、密钥或支付功能。**

## 手机上云端构建

1. 在仓库 Actions 页运行 `Build Android APK`（或者推送到 main/master 自动触发）。
2. 等待任务完成，从本次运行的 Artifacts 下载 `hce-demo-debug-apk`，解压后安装 `app-debug.apk`。
3. 在手机上开启 NFC，打开应用检查 HCE 是否受支持。
4. 需要第二台可控的 ISO-DEP 读卡设备向测试 AID `F0010203040506` 发送 `00A4040007F0010203040506`，成功时回复 `9000`。

注意：部分手机需要在系统 NFC 设置中允许应用作为卡模拟服务；某些 ROM 会限制非默认服务或锁屏状态下的 HCE。此项目未经本地 Android SDK 编译验证，云端构建结果为准。

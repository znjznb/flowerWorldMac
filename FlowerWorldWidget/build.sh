#!/bin/bash
# 花花世界 macOS Menu Bar App Build Script
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "============================================"
echo "  花花世界 macOS Menu Bar App - 构建脚本"
echo "============================================"

# Step 1: Check Xcode
if ! xcodebuild -version &>/dev/null; then
    echo "❌ 错误: 未找到 Xcode。请先从 App Store 安装 Xcode。"
    exit 1
fi
echo "✅ Xcode 已安装"

# Step 2: Check/Install xcodegen
if ! command -v xcodegen &> /dev/null; then
    echo "正在安装 xcodegen (用于生成 Xcode 项目)..."
    if command -v brew &> /dev/null; then
        brew install xcodegen
    else
        echo "❌ 错误: 未找到 Homebrew 和 xcodegen。"
        echo "请手动安装: brew install xcodegen"
        exit 1
    fi
fi
echo "✅ xcodegen 已安装"

# Step 3: Generate Xcode project
echo "正在生成 Xcode 项目..."
xcodegen generate
echo "✅ Xcode 项目已生成"

# Step 4: Build
echo "正在编译应用..."
xcodebuild -project "花花世界.xcodeproj" \
           -scheme FlowerWorldWidget \
           -configuration Release \
           -derivedDataPath build/DerivedData \
           build

APP_PATH=$(find "build/DerivedData/Build/Products/Release" -name "*.app" -type d 2>/dev/null | head -1)

if [ -d "$APP_PATH" ]; then
    echo ""
    echo "============================================"
    echo "✅ 构建成功！"
    echo "应用路径: $SCRIPT_DIR/$APP_PATH"
    echo "============================================"
    echo ""
    echo "将应用拖入 Applications 文件夹即可安装:"
    echo "  cp -R \"$APP_PATH\" /Applications/"
    echo ""
    echo "首次打开需要右键 → 打开（因未签名）"
else
    echo "❌ 构建似乎失败了，请检查上方错误信息。"
    exit 1
fi

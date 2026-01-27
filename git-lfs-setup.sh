#!/bin/bash

# 自动配置 Git LFS 并检测大文件

set -e

# 初始化 Git LFS
if ! git lfs &> /dev/null; then
    echo "Git LFS 未安装，请先安装 Git LFS: https://git-lfs.github.com/"
    exit 1
fi

git lfs install

echo "✅ 已初始化 Git LFS"

# 检查当前仓库中超过 100MB 的大文件
LARGE_FILES=$(git rev-list --objects --all | git cat-file --batch-check="%(objectname) %(objecttype) %(rest)" | sort -k3 -n | awk '$2 == "blob" && $4 > 100*1024*1024 {print $1 " " $3}')

if [ -z "$LARGE_FILES" ]; then
    echo "🎉 没有检测到超过 100MB 的大文件！"
    exit 0
fi

echo "🚨 检测到以下超过 100MB 的大文件："
echo "$LARGE_FILES"

# 自动配置 Git LFS 追踪大文件类型
while read -r line; do
    FILE_NAME=$(echo "$line" | awk '{print $2}')
    EXTENSION="${FILE_NAME##*.}"
    git lfs track "*.$EXTENSION"
done <<< "$LARGE_FILES"

echo "✅ 已配置 Git LFS 追踪规则"
git add .gitattributes
git commit -m "Configure Git LFS for large files"
echo "✅ 已提交 .gitattributes 配置"

echo "📢 提醒：你已经成功配置了 Git LFS。如果你遇到问题，可以使用 git-lfs-cleanup.sh 来清理大文件历史。"

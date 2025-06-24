#!/bin/bash

# Turms 文档快速搜索脚本

DOCS_DIR="/home/icyyaww/program/meetboy/docs-organized"

echo "=== Turms 项目文档快速搜索工具 ==="
echo ""

if [ $# -eq 0 ]; then
    echo "用法:"
    echo "  $0 <关键词>               # 在所有文档中搜索关键词"
    echo "  $0 --list                # 列出所有文档"
    echo "  $0 --category <分类>     # 列出指定分类的文档"
    echo "  $0 --tree               # 显示文档树形结构"
    echo ""
    echo "可用分类:"
    echo "  architecture  - 架构文档"
    echo "  services      - 服务文档"
    echo "  development   - 开发文档"
    echo "  api          - API文档"
    echo "  deployment   - 部署文档"
    echo "  analysis     - 分析报告"
    echo "  project      - 项目文档"
    exit 1
fi

case "$1" in
    --list)
        echo "📋 所有文档列表:"
        find "$DOCS_DIR" -name "*.md" -type f | sort
        ;;
    --category)
        if [ -z "$2" ]; then
            echo "❌ 请指定分类名称"
            exit 1
        fi
        if [ -d "$DOCS_DIR/$2" ]; then
            echo "📁 $2 分类文档:"
            find "$DOCS_DIR/$2" -name "*.md" -type f | sort
        else
            echo "❌ 分类 '$2' 不存在"
        fi
        ;;
    --tree)
        echo "🌳 文档树形结构:"
        tree "$DOCS_DIR" -I "*.sh"
        ;;
    *)
        echo "🔍 搜索关键词: '$1'"
        echo ""
        grep -r -l -i "$1" "$DOCS_DIR"/*.md "$DOCS_DIR"/**/*.md 2>/dev/null | while read file; do
            echo "📄 $(basename "$file")"
            echo "   路径: $file"
            echo "   匹配行:"
            grep -n -i --color=always "$1" "$file" | head -3
            echo ""
        done
        ;;
esac
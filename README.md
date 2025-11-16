# Blink Mod

一个为Minecraft 1.21.8添加填充器工具的模组，使用Architectury框架同时支持Forge和Fabric。

## 功能

- **填充器物品**: 一个强大的工具，可以快速填充选定区域
- **区域选择**: 右键设置第一个位置，Shift+右键设置第二个位置
- **方块选择**: 副手持方块+Shift+左键设置要填充的方块类型
- **智能填充**: 左键执行填充操作
- **生存模式支持**: 在生存模式下会消耗背包中的方块

## 使用方法

1. 获得填充器物品（在创造模式物品栏的"Blink"标签页中）
2. 右键点击第一个位置
3. Shift+右键点击第二个位置
4. 在副手持有要填充的方块，然后Shift+左键设置填充方块
5. 左键执行填充操作

## 构建项目

```bash
# 构建所有平台
./gradlew build

# 仅构建Fabric版本
./gradlew :fabric:build

# 仅构建Forge版本
./gradlew :forge:build
```

## 开发环境

- Minecraft: 1.21.8
- Architectury API: 17.0.8
- Fabric Loader: 0.16.9
- Fabric API: 0.110.5+1.21.8
- Forge: 1.21.8-54.0.25

## 许可证

MIT License
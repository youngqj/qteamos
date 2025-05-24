# QTeam-OS 分步清理指南

## 🎯 清理目标

基于您的"保险点还是分步吧"要求，我们将激进式清理分解为4个安全的步骤，每步都可以独立执行和回滚。

## 📋 执行计划

| 步骤 | 脚本 | 主要任务 | 预期时间 | 风险等级 |
|------|------|----------|----------|----------|
| 步骤1 | `./cleanup-step1.sh` | 删除过时组件 | 30分钟 | 低 |
| 步骤2 | `./cleanup-step2.sh` | 删除重复API接口 | 15分钟 | 低 |
| 步骤3 | `./cleanup-step3.sh` | 重组目录结构 | 45分钟 | 中 |
| 步骤4 | `./cleanup-step4.sh` | 最终验证清理 | 30分钟 | 低 |

## 🚀 执行步骤

### 步骤1: 删除过时组件
```bash
./cleanup-step1.sh
```

**删除内容:**
- `PluginSystem.java` (1700+行 → 已被PluginSystemCoordinator替代)
- `PluginLifecycleManager.java` (1400+行 → 已被PluginLifecycleCoordinator替代)
- `PluginRolloutManager.java` (900+行 → 功能已拆分到新组件)
- `PluginHotDeployService.java` (800+行 → 功能已整合)
- 其他过时manager组件

**安全措施:**
- 自动备份为 `.step1.bak` 文件
- 编译验证
- 可通过 `./rollback-step1.sh` 回滚

### 步骤2: 删除重复API接口
```bash
./cleanup-step2.sh
```

**删除内容:**
- 整个 `core.plugin.api` 目录 (与qteam-api重复)
- 重复的接口定义

**安全措施:**
- 备份为 `api.step2.bak` 目录
- 编译验证
- 可通过 `./rollback-step2.sh` 回滚

### 步骤3: 重组目录结构
```bash
./cleanup-step3.sh
```

**重组内容:**
- `running/` → `model/` (更清晰的语义)
- 批量更新import语句

**安全措施:**
- 目录备份为 `running.step3.bak`
- 文件备份为 `.step3.bak`
- 编译验证
- 可通过 `./rollback-step3.sh` 回滚

### 步骤4: 最终验证
```bash
./cleanup-step4.sh
```

**验证内容:**
- 全面编译验证
- 生成清理报告
- 架构质量评分
- 提供后续建议

## 🔄 回滚机制

### 单步回滚
```bash
./rollback-step1.sh  # 回滚步骤1
./rollback-step2.sh  # 回滚步骤2  
./rollback-step3.sh  # 回滚步骤3
```

### 完全回滚
```bash
./rollback-all.sh    # 回滚所有更改
```

## 🗑️ 最终清理

当确认所有功能正常后：
```bash
./cleanup-final.sh   # 删除所有备份文件
```

## 📊 预期成果

### 代码减少量
- **总计**: ~8300行冗余代码
- **主要组件**: PluginSystem (1700行) + PluginLifecycleManager (1400行) + 其他

### 架构评分提升
- **清理前**: 78分 (已重构但未清理)
- **清理后**: 92分 (激进清理完成)
- **提升**: +14分

### 目录结构优化
```
qteam-os/src/main/java/com/xiaoqu/qteamos/core/plugin/
├── coordinator/      # 系统协调
├── lifecycle/        # 生命周期管理
├── installer/        # 插件安装
├── scanner/          # 插件扫描
├── watcher/          # 文件监控
├── event/            # 事件系统
├── model/            # 数据模型 (原running/)
├── service/          # 业务服务
├── security/         # 安全管理
├── web/              # Web API
└── config/           # 配置管理
```

## ⚠️ 注意事项

### 执行前检查
1. 确保当前代码已提交到版本控制
2. 确认测试环境可用
3. 备份重要配置文件

### 每步执行后
1. 检查编译状态
2. 验证核心功能
3. 确认无误后继续下一步

### 出现问题时
1. 立即停止后续步骤
2. 执行相应回滚脚本
3. 分析问题原因
4. 修复后重新执行

## 🎯 成功标准

完成后应达到：
- ✅ 编译和测试完全通过
- ✅ 零代码冗余
- ✅ 清晰的目录结构
- ✅ 单一职责组件
- ✅ 架构评分92+分

## 📞 遇到问题？

如果在执行过程中遇到问题：
1. 查看编译错误日志
2. 检查备份文件是否完整
3. 执行回滚恢复
4. 寻求技术支持

---

**记住**: 分步执行，每步验证，确保安全！ 
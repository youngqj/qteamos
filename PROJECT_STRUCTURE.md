# QTeam项目结构规范

## 📁 标准目录结构

```
QTeam/
├── qteam-api/              # 核心接口定义层
├── qteam-common/           # 公共组件层  
├── qteam-sdk/              # SDK层（给插件开发者使用）
├── qteam-os/               # 核心实现层
├── plugin-demos/           # 示例插件（仅用于学习参考）
├── plugins/                # 🏭 生产环境插件目录
├── plugins-dev/            # 🧪 测试开发环境插件目录
├── plugins-temp/           # 📥 生产环境临时目录（新插件投放点）
├── plugins-temp-dev/       # 📥 测试环境临时目录（开发插件投放点）
├── docs/                   # 项目文档
├── scripts/                # 项目脚本
├── plugin-data/            # 插件运行时数据
│   ├── cache/              # 插件缓存（Git忽略）
│   ├── temp/               # 临时文件（Git忽略）
│   └── configs/            # 插件配置
├── pom.xml                 # Maven父工程配置
├── README.md               # 项目说明
└── .gitignore              # Git忽略规则
```

## 🔄 插件热部署机制

### 部署流程
```
新插件.jar → plugins-temp*/ → 系统扫描 → 配置入库 → mv到plugins*/ → 系统加载
```

### 目录职责
- **plugins-temp/**: 生产环境插件投放点，系统扫描此目录
- **plugins-temp-dev/**: 测试环境插件投放点，开发时使用
- **plugins/**: 生产环境正式插件目录，已配置完成的插件
- **plugins-dev/**: 测试环境正式插件目录，开发测试插件
- **plugin-demos/**: 示例插件，用于学习和参考

### 环境隔离
- **生产环境**: `plugins-temp/` → `plugins/`
- **测试环境**: `plugins-temp-dev/` → `plugins-dev/`

## 🚫 禁止的目录/文件

以下目录和文件**不应该**出现在项目根目录：

### 🗑️ 临时文件
- `logs/` - 日志文件应该由应用配置管理
- `build/` - 编译产物应该被自动清理
- `target/` - Maven编译产物
- `analysis_results/` - 代码分析结果

### 💻 IDE配置文件
- `.idea/` - IntelliJ IDEA配置
- `.vscode/` - VS Code配置
- `.cursor/` - Cursor配置

### 📝 散乱文档
- `每日进展.txt` - 应放入docs/目录
- `plugin-refactoring-plan.md` - 应放入docs/目录
- 任何临时文档 - 应整理到docs/目录

### ❌ 错误的插件目录
- **不要创建其他插件目录**，只使用标准的四个插件目录
- **不要直接操作plugins/和plugins-dev/**，应通过临时目录投放

## 📋 命名规范

### 目录命名
- 使用小写字母和连字符：`qteam-api`, `plugin-demos`
- 不使用下划线或空格
- 描述性但简洁：`docs` 而不是 `documentation`

### 文件命名
- Java文件：使用PascalCase - `PluginManager.java`
- 配置文件：使用小写连字符 - `application.yml`
- 文档文件：使用大写连字符 - `PROJECT_STRUCTURE.md`
- 脚本文件：使用小写连字符 - `project-cleanup.sh`

## 🔧 维护原则

### 1. **单一职责原则**
- 每个目录只负责一个明确的功能
- 避免在根目录堆积文件

### 2. **分层原则**
- api → common → sdk → os 的依赖顺序
- 不允许反向依赖

### 3. **清理原则**
- 定期运行 `./project-cleanup.sh`
- 编译后及时清理临时文件
- 文档及时整理到docs目录

### 4. **版本控制原则**
- 所有临时文件和编译产物不提交
- IDE配置文件不提交
- 仅提交源代码和必要配置

## 🛠️ 开发工作流

### 新建文件时
1. 确定文件应该放置的正确目录
2. 遵循命名规范
3. 如果是临时文件，确保在.gitignore中

### 重构时
1. 保持目录结构清晰
2. 及时删除废弃文件
3. 更新相关文档

### 插件开发时
1. 示例插件放在`plugin-demos/`
2. 生产插件放在`plugins/`
3. 不要创建临时插件目录

## 🚨 常见问题

### Q: 我的插件应该放在哪里？
A: 
- 学习用的示例插件 → `plugin-demos/`
- 生产环境插件 → `plugins/`
- 不要创建新的插件目录

### Q: 日志文件去哪了？
A: 日志文件现在由应用配置管理，不再在项目根目录产生logs文件夹

### Q: 我的文档应该放在哪里？
A: 所有文档统一放在`docs/`目录下，根目录只保留README.md

### Q: IDE配置文件为什么被删了？
A: IDE配置文件是个人的，不应该提交到版本控制。每个开发者应该使用自己的IDE设置。

## 🎯 清理命令

定期执行以下命令保持项目整洁：

```bash
# 运行自动清理脚本
./project-cleanup.sh

# 手动清理Maven编译产物
mvn clean

# 检查项目结构
tree -I 'target|node_modules|.git' -L 2
```

---

**记住：保持项目结构清晰是团队协作的基础！** 🤝 
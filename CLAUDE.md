## Repository Guidelines

本项目用于监控ClaudeCode agent的工作情况，使用中文

### 开发环境

Jdk21 (路径: D:\soft\jdk21)

SpringBoot3.0

构建命令: JAVA_HOME="D:/soft/jdk21" ./gradlew

### 依赖清单

- jackson-dataformat-yaml: YAML 解析（CostCalculationService 加载 model-pricing.yml）

后续引入新的依赖自动在这里添加

##编码规则

   1.所有方法必须要有注释

2. 新增的方法需要有对应的单元测试
3. commit信息需要使用中文
4. 不确定的需求需要和用户讨论，不要擅自决定
5. 用户纠正过的行为自动记录到CLAUDE.md,不重复犯错
6. 多次提到 使用 澄清的自动沉淀成skills
7. 仓库地址https://github.com/Sinpher/claude-monitoring.git，每天自动将当天的内容提交到仓库
8. commit作者使用用户的


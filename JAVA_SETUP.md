# Java 17 安装配置指南

## 问题说明

当前系统使用的是 Java 8，但 Spring Boot 3.x 项目需要 Java 17 或更高版本。

## 解决方案

### 1. 下载 Java 17

推荐使用以下任一发行版：

- **Eclipse Temurin (Adoptium)** - 推荐
  - 下载地址：https://adoptium.net/temurin/releases/?version=17
  - 选择 Windows x64 版本，下载 `.msi` 安装包

- **Oracle JDK 17**
  - 下载地址：https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html

- **Amazon Corretto 17**
  - 下载地址：https://aws.amazon.com/corretto/

### 2. 安装 Java 17

1. 运行下载的安装程序
2. 按照向导完成安装（建议安装到默认路径）
3. 安装完成后，Java 17 通常会被安装到：
   - `C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot`
   - 或 `C:\Program Files\Java\jdk-17`

### 3. 配置环境变量

#### 方法一：通过系统设置（推荐）

1. 右键"此电脑" -> "属性"
2. 点击"高级系统设置"
3. 点击"环境变量"
4. 在"系统变量"中：
   - 新建或编辑 `JAVA_HOME`，设置为：`C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot`（根据实际安装路径）
   - 编辑 `Path`，确保 `%JAVA_HOME%\bin` 在最前面（或删除旧的 Java 8 路径）

#### 方法二：通过 PowerShell（临时，仅当前会话）

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

### 4. 验证安装

打开新的 PowerShell 窗口，运行：

```powershell
java -version
javac -version
```

应该显示：
```
java version "17.0.x" ...
javac 17.0.x
```

### 5. 重新编译项目

配置完成后，在项目根目录运行：

```bash
mvn clean install -DskipTests
```

## 注意事项

- 如果系统中有多个 Java 版本，确保 `JAVA_HOME` 和 `Path` 指向 Java 17
- 修改环境变量后，需要**重新打开** PowerShell/命令行窗口才能生效
- 如果使用 IDE（如 IntelliJ IDEA、Eclipse），也需要在 IDE 中配置使用 Java 17

## 快速检查命令

```powershell
# 检查当前 Java 版本
java -version

# 检查 JAVA_HOME
echo $env:JAVA_HOME

# 检查 Java 安装路径
where java
```


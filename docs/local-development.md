# 本地开发

## 当前端口

| 服务 | 地址 |
| --- | --- |
| Web | `http://127.0.0.1:32588` |
| Java API | `http://127.0.0.1:8791` |
| MySQL | `127.0.0.1:3307` |

## 本地 MySQL

开发实例的数据目录：

```text
.local/mysql/data
```

该目录不进入 Git。首次初始化：

```bash
mkdir -p .local/mysql/data
/usr/local/mysql/bin/mysqld --no-defaults \
  --initialize-insecure \
  --basedir=/usr/local/mysql \
  --datadir="$PWD/.local/mysql/data" \
  --log-error="$PWD/.local/mysql/initialize.log"
```

启动：

```bash
/usr/local/mysql/bin/mysqld --no-defaults \
  --basedir=/usr/local/mysql \
  --datadir="$PWD/.local/mysql/data" \
  --port=3307 \
  --socket="$PWD/.local/mysql/mysql.sock" \
  --pid-file="$PWD/.local/mysql/mysql.pid" \
  --log-error="$PWD/.local/mysql/mysqld.log" \
  --bind-address=127.0.0.1 \
  --mysqlx=OFF
```

数据库与用户：

```sql
CREATE DATABASE IF NOT EXISTS frametalk
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'frametalk_app'@'127.0.0.1'
  IDENTIFIED BY '<local-password>';

GRANT ALL PRIVILEGES ON frametalk.*
  TO 'frametalk_app'@'127.0.0.1';
```

`frametalk-boot/src/main/resources/application-local.yml` 保存本机开发连接信息，并已被 Git 忽略。

## 后端

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17)
JAVA_HOME="$JAVA_HOME" mvn clean verify
JAVA_HOME="$JAVA_HOME" "$JAVA_HOME/bin/java" \
  -jar frametalk-boot/target/frametalk-boot-0.1.0-SNAPSHOT.jar
```

默认启用 `local` profile，API 端口为 `8791`。

## 前端

```bash
cd frametalk-web
pnpm install
pnpm dev
```

页面验证：

```bash
pnpm build
pnpm visual:check
```

## 本地验证账号

```text
邮箱：creator@frametalk.local
密码：由本地开发数据库决定，不写入仓库
```

该账号只存在于当前本地开发数据库。新注册账号的邮箱验证码写入 Java 后端日志，搜索：

```text
FrameTalk development email code
```

## SMTP 邮件

本地默认 `FRAMETALK_MAIL_MODE=log`，不会向真实邮箱发信。

启用 SMTP 前需要配置：

```text
FRAMETALK_MAIL_MODE=smtp
FRAMETALK_MAIL_HOST
FRAMETALK_MAIL_PORT
FRAMETALK_MAIL_USERNAME
FRAMETALK_MAIL_PASSWORD
FRAMETALK_MAIL_FROM
FRAMETALK_MAIL_FROM_NAME
FRAMETALK_MAIL_AUTH
FRAMETALK_MAIL_SSL
```

QQ 邮箱、163 邮箱等需要使用 SMTP 授权码，不使用网页登录密码。

## 使用已有 MySQL

不使用本地 `3307` 实例时，修改未提交的 `application-local.yml`，或设置：

```text
FRAMETALK_DB_URL
FRAMETALK_DB_USERNAME
FRAMETALK_DB_PASSWORD
```

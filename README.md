![LOGO icon](https://raw.githubusercontent.com/oldmanpushcart/images/master/greys/greys-logo-readme.png)

# 相关文档

* [关于软件](https://github.com/oldmanpushcart/greys-anatomy/wiki)
* [程序安装](https://github.com/oldmanpushcart/greys-anatomy/wiki/installing)
* [入门说明](https://github.com/oldmanpushcart/greys-anatomy/wiki/Getting-Start)
* [常见问题](https://github.com/oldmanpushcart/greys-anatomy/wiki/FAQ)


# 程序安装：

- SH

  ```
  curl -sLk http://ompc.oss.aliyuncs.com/greys/install.sh|sh
  ```

## 最新版本

### **VERSION :** 1.5.4.8

### 改动说明

本次是一个常规维护版本升级，主要是增强现有功能。

脚本全面拥抱BASH，降低维护成本，毕竟现在看来BASH的用户群体还是比KSH多啊，我不能老是抱着KSH的大腿不放。

### 改动清单

- 安装脚本、启动脚本从KSH全面切换到BASH

- 不在维护BAT脚本

- 废弃`greys`的启动命令，转为`greys.sh`，更精准的表达启动含义

- 更友好的错误信息提示内容

- `tt`命令增加`-s`参数，现在你可以搜索所有的tt内容啦

### 版本号说明

`大版本`.`主版本`.`小版本`.`漏洞修复`

* 大版本

  这个版本更新说明程序架构体系进行了重大升级，比如之前的0.1版升级到1.0版本，整个软件的架构从单机版升级到了SOCKET多机版。并将Greys的性质进行的确定：Java版的HouseMD，但要比前辈们更强。

* 主版本

  程序的架构设计进行重大改造，但不影响用户对这款软件的定位。

* 小版本

  增加新的命令和功能

* 漏洞修复

  对现有版本进行漏洞修复和增强
  
  - `大版本`、`主版本`、之间不做任何向下兼容的承诺，即`0.1`版本的Client不保证一定能正常访问`1.0`版本的Server。

  - `小版本`不兼容的版本会在版本升级中指出

  - `漏洞修复`保证向下兼容

## 维护者

* [李夏驰](http://www.weibo.com/vlinux)
* [姜小逸又胖了](http://weibo.com/chengtd)

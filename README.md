![LOGO icon](https://raw.githubusercontent.com/oldmanpushcart/images/master/greys/greys-logo-readme.png)

>
线上系统为何经常出错？数据库为何屡遭黑手？业务调用为何频频失败？连环异常堆栈案，究竟是那次调用所为？
数百台服务器意外雪崩背后又隐藏着什么？是软件的扭曲还是硬件的沦丧？
走进科学带你了解Greys, Java线上问题诊断工具。

# 相关文档

* [关于软件](https://github.com/oldmanpushcart/greys-anatomy/wiki/Home)
* [程序安装](https://github.com/oldmanpushcart/greys-anatomy/wiki/installing)
* [入门说明](https://github.com/oldmanpushcart/greys-anatomy/wiki/Getting-Start)
* [常见问题](https://github.com/oldmanpushcart/greys-anatomy/wiki/FAQ)
* [更新记事](https://github.com/oldmanpushcart/greys-anatomy/wiki/Chronicle)
* [详细文档](https://github.com/oldmanpushcart/greys-anatomy/wiki/greys-pdf)

# 程序安装

- 远程安装

  ```shell
  curl -sLk http://ompc.oss.aliyuncs.com/greys/install.sh|sh
  ```
  
- 远程安装(短链接)
  
  ```shell
  curl -sLk http://t.cn/R2QbHFc|sh
  ```

## 最新版本

### **VERSION :** 1.7.1.0

更新内容

- 去掉了所有命令的`-S`参数，默认`-S`参数永远生效，这意味着你在watch一个接口或者抽象类时能自动关联到其所有的实现和子类

- 所有命令进行类匹配时，能向上查找到其父类所声明的方法。之前只能匹配到当前类所声明的方法，遇到子类调用父类的情况则匹配不上，容易造成使用者的误解

- 增加`ptrace`命令，该命令为`trace`命令的增强版，但内部实现完全不同。

  1. `trace`的工作原理非常容易让增强后的方法超过JVM方法64K的限制，`ptrace`则绕过这个问题
  1. `ptrace`的`-t`参数能利用`tt`命令记录下指定需然路径上的方法入参和返回值，并在`tt`命令看到这些调用记录，极大方便跟踪定位问题
  1. `ptrace`的使用会带来一定的性能开销，如果渲染路径匹配上的类、方法过多，会在渲染过程中大量消耗CPU资源、造成系统长时间停顿。所以建议`tracing-path-pattern`匹配的方法越少越好

- `monitor`命令增加`MIN-RT`、`MAX-RT`两个指标，方便排除`RT`判断的干扰
  
- 修复`trace`命令之前在处理方法抛出异常时会导致展示的链路错误、链路耗时为负数的情况
  
- 重构部分内核，为年后发布`1.8.0.0`版本支持GP协议([GREYS-PROTOCOL](https://github.com/oldmanpushcart/greys-anatomy/wiki/GREYS-PROTOCOL))做准备  
  
- 优化性能，表达式用`Unsafe`、`ThreadLocal`、`LRUCache`做性能开销优化


### 版本号说明

`主版本`.`大版本`.`小版本`.`漏洞修复`

* 主版本

  这个版本更新说明程序架构体系进行了重大升级，比如之前的0.1版升级到1.0版本，整个软件的架构从单机版升级到了SOCKET多机版。并将Greys的性质进行的确定：Java版的HouseMD，但要比前辈们更强。

* 大版本

  程序的架构设计进行重大改造，但不影响用户对这款软件的定位。

* 小版本

  增加新的命令和功能

* 漏洞修复

  对现有版本进行漏洞修复和增强
  
  - `主版本`、`大版本`、之间不做任何向下兼容的承诺，即`0.1`版本的Client不保证一定能正常访问`1.0`版本的Server。

  - `小版本`不兼容的版本会在版本升级中指出

  - `漏洞修复`保证向下兼容

# 维护者

* [李夏驰](http://www.weibo.com/vlinux)
* [姜小逸又胖了](http://weibo.com/chengtd)


# 程序编译

- 打开终端

  ```shell
  git clone git@github.com:oldmanpushcart/greys-anatomy.git
  cd greys-anatomy/bin
  ./greys-packages.sh
  ```
  
- 程序执行

  在`target/`目录下生成对应版本的release文件，比如当前版本是`1.7.0.4`，则生成文件`target/greys-1.7.0.4-bin.zip`
  
  程序在本地编译时会主动在本地安装当前编译的版本，所以编译完成后即相当在本地完成了安装。
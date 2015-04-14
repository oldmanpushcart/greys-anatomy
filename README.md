![LOGO icon](https://raw.githubusercontent.com/oldmanpushcart/images/master/greys/greys-logo-readme.png)

# 相关文档

* [关于软件](https://github.com/oldmanpushcart/greys-anatomy/wiki)
* [程序安装](https://github.com/oldmanpushcart/greys-anatomy/wiki/installing)
* [入门说明](https://github.com/oldmanpushcart/greys-anatomy/wiki/Getting-Start)
* [常见问题](https://github.com/oldmanpushcart/greys-anatomy/wiki/FAQ)


# 程序安装：

```shell
curl -sLk http://ompc.oss.aliyuncs.com/greys/install.sh|ksh
```

## 最新版本

### **VERSION :** 1.5.4.6

### 改动说明

随着越来越多的人使用，很多人都提出了宝贵的意见，这些意见我都一一记下来并在随后的版本中进行修改。最近一直有几个建议，但我觉的对Greys一贯所推崇的geek式的交互模式存在标记大的争议，其中有一项我斟酌了很久

> 用快速简单的通配符表达式取代华而不实的正则表达式

终于，在好几个人同时反馈之后，将会在这个版本支持`通配符表达式`，而且默认将启用`通配符表达式`，`正则表达式`将通过参数`-E`强行指定后才能启用。

### 改动清单

- 以下命令将不在默认支持`正则表达式`，而是通过`-E`参数才能强制开启，开启之后的匹配模式和原来用法一致。
  
  - `jstack`
  - `monitor`
  - `profiler`
  - `sc`
  - `sm`
  - `tt`
  - `watch`
    
- 静态构造函数在Greys的各个输出场景中，由原来的`类名`调整为`<cinit>`
  
- 构造函数在Greys的各个输出场景中，由原来的`类名`调整为`<init>`


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

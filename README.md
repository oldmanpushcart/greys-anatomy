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

### **VERSION :** 1.5.4.7

### 改动说明

本次升级改变`sc`命令有一个`-s`参数无法继续向下兼容变成了`-S`，但由于这个参数使用频率和变化不大（从小写变成大写），所以仍然以小版本升级迭代上来。

之所以要发布这个升级版主要是解决昨天协助测试排查线上问题时，有一个接口返回了几十条信息，本来想用OGNL表达式酷炫一把，但由于Greys在解析参数编写有BUG，导致无法生效，这次发布重点解决的是这个问题。

### 改动清单

- 以下命令将支持参数`-S`，开启之后将能主动匹配上对应类及其子类。
  
  - `jstack`
  - `monitor`
  - `sc`
  - `tt`
  - `watch`
  
- `sc`命令的`-s`(小写)参数变更为`-S`(大写)  

- OGNL表达式支持引号`'`或`"`的复杂写法，比如

  ```
  tt -i 1000 -w 'params[0].addresses.{? 500 == #this.addressId}'
  ```

- 增加`bash`的安装和启动脚本，感谢同事简离的贡献，这下齐昊不会笑我土了。

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

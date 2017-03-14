![LOGO icon](https://raw.githubusercontent.com/oldmanpushcart/images/master/greys/greys-logo-readme.png)

>
线上系统为何经常出错？数据库为何屡遭黑手？业务调用为何频频失败？连环异常堆栈案，究竟是哪次调用所为？
数百台服务器意外雪崩背后又隐藏着什么？是软件的扭曲还是硬件的沦丧？
走进科学带你了解Greys, Java线上问题诊断工具。

# 相关文档

* [关于软件](https://github.com/oldmanpushcart/greys-anatomy/wiki/Home)
* [程序安装](https://github.com/oldmanpushcart/greys-anatomy/wiki/installing)
* [入门说明](https://github.com/oldmanpushcart/greys-anatomy/wiki/Getting-Started)
* [常见问题](https://github.com/oldmanpushcart/greys-anatomy/wiki/FAQ)
* [更新记事](https://github.com/oldmanpushcart/greys-anatomy/wiki/Chronicle)
* [详细文档](https://github.com/oldmanpushcart/greys-anatomy/wiki/greys-pdf)
* [ENGLISH-README](https://github.com/oldmanpushcart/greys-anatomy/blob/master/Greys_en.md)

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

### **VERSION :** 1.7.6.4

1. contation-express增加`#cost`变量，影响命令`stack`、`watch`

    > PS：其它命令其实很早就支持用`#cost`变量作为拦截过滤的条件，单位`ms`，`stack`、`watch`这两个命令属于遗漏这次修复

2. `ptrace`、`trace`、`watch`、`tt`等命令增加对中间件跟踪号的支持

    > 在很多大公司中,会有比较多的中间件调用链路渲染技术用来记录和支撑分布式调用场景下的系统串联，用于串联各个系统调用的一般是一个全局唯一的跟踪号。在阿里巴巴中间件中，我们用的是EagleEye

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
  
  
# 写在后边

## 心路感悟

我编写和维护这款软件已经5年了，5年中Greys也从`0.1`版本一直重构到现在的`1.7`。在这个过程中我得到了许多人的帮助与建议，并在年底我计划发布`2.0`版本，将开放Greys的底层通讯协议，支持websocket访问。

多年的问题排查经验我没有过多的分享，一个Java程序员个中的苦闷也无从分享，一切我都融入到了这款软件的命令中，希望这些沉淀能帮助到可能需要到的你少走一些弯路，同时我也非常期待你们对她的反馈，这样我将感到非常开心和有成就感。

## 帮助我们

Greys的成长需要大家的帮助。

- **分享你使用Greys的经验**

   我非常希望能得到大家的使用反馈和经验分享，如果你有，请将分享文章敏感信息脱敏之后邮件给我：[oldmanpushcart@gmail.com](mailto:oldmanpushcart@gmail.com)，我将会分享给更多的同行。

- **帮助我完善代码或文档**

  一款软件再好，也需要详细的帮助文档；一款软件再完善，也有很多坑要埋。今天我的精力非常有限，希望能得到大家共同的帮助。

- **如果你喜欢这款软件，欢迎打赏一杯咖啡**

  嗯，说实话，我是指望用这招来买辆玛莎拉蒂...当然是个玩笑～你们的鼓励将会是我的动力，钱不在乎多少，重要的是我将能从中得到大家善意的反馈，这将会是我继续前进的动力。
  
  ![alipay](https://raw.githubusercontent.com/oldmanpushcart/images/master/alipay-vlinux.png)

## 联系我们

有问题阿里同事可以通过旺旺找到我，阿里外的同事可以通过[我的微博](http://weibo.com/vlinux)联系到我。今晚的杭州大雪纷飞，明天西湖应该非常的美丽，大家晚安。

菜鸟-杜琨（dukun@alibaba-inc.com）
  

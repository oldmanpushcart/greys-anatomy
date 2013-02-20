greys-anatomy 是一个java进程执行过程中的异常诊断工具。
在不中断程序执行的情况下轻松完成问题排查工作。

## 特征功能
- 交互方式：命令行交互，支持命令参数Tab提示补全。

- 功能：
 - 查看加载类，方法信息
 - 方法执行监控（调用量，成功失败率，响应时间）
 - 方法执行数据观测（参数，返回结果，异常信息等）
 - 方法执行数据自定义观测（js脚本）
 - 查看方法调用堆栈

- 特点：
 - 安装使用便捷，仅一个jar包。
 - 无需编写复杂的btrace脚本，使用命令的方式就可以轻松处理常见线上问题
 - 开源项目，可定制命令。
    
## 安装
- 命令行下载安装：
 - curl -sLk https://raw.github.com/chengtongda/greys-anatomy/master/bin/install.sh|ksh  
- 手动下载安装（直接解压即可）：
 - http://pan.baidu.com/share/link?shareid=285557&uk=3039715289

## 入门
- 启动

    ./greys -pid 9999 -port 3658
  - 参数：-pid 代理进程id 
         -port 链接端口号（默认3658）	

- 命令
 - search-class   查看加载的类信息

        参数：-class 查找的类名称
		      -is-super 是否带父类搜索
			     -is-detail 是否查看类详细信息
			  
 - search-method  查看加载的方法信息

        参数：-class  查找的方法所属类名称
			  -method 查找的方法名称
			  -is-detail 是否查看方法详细信息

 - monitor   方法执行监控

        参数：-class  需要监控的方法所属类名称
			  -method 需要监控的方法名称
			  -cycle  监控周期（单位s）

 - watch   方法执行数据观测

        参数：-class  需要观测的方法的类名称
			  -method 需要观测的方法名称
			  -exp  观测数据表达式（js表达式）
			  -watch-point  观测点（方法执行和方法执行后/before,finish）
	
		watch可获取打Advice数据，变量名为p
 - javascript   方法执行自定义观测

        参数：-class  需要观测的方法的类名称 -method 需要观测的方法名称 -file  js脚本路径
        
            支持接口（只需在js脚本中实现这些接口即可）：
                before/success/exception/finished/   入参：Advice p,Output output,TLS tls
                create/destory    入参：Output output,TLS tls				

 - 通用参数: -o 输出结果重定向

- 相关类结构
```
Advice
   |-Target target (探测目标)
   |     |-Class<?> targetClass (探测目标类)
   |     |
   |     |-Object targetThis  (探测目标实例)
   |	 |
   |	 |-TargetBehavior targetBehavior   (探测方法/构造器)
   |			  |-String name
   |-Object[] parameters   (调用参数)
   |-Object returnObj      (返回值)
   |-Throwable throwException   (抛出异常)
```
```
OutPut
   |-println(String msg)   (打印)
```
```
TLS  (threadLocal)
  |-put(String key,Object value)
  |
  |-get(String key)
```

- 快捷键
 - 中断:ctrl+d
 - 退出:ctrl+c

- 命令示例
 - search-class
```
   ga?>search-class -class .*TestApp.* -is-super false -is-detail true
 	 class info : com.googlecode.greys.test.TestApp
		 ---------------------------------------------------------------
			code-source : /home/jiangyi/workspace/test.jar
				   name : com.googlecode.greys.test.TestApp
			simple-name : TestApp
			   modifier : public
			 annotation : 
			 interfaces :     super-class : java.lang.Object
		   class-loader : sun.misc.Launcher$AppClassLoader@11b86e7
							`-->sun.misc.Launcher$ExtClassLoader@35ce36

		---------------------------------------------------------------
		done. classes result: match-class=1;
```
 - search-method
```
 	ga?>search-method -class .*TestApp.* -method .*hello.* -is-detail true 
		method info : com.googlecode.greys.test.TestApp->helloMethod
		---------------------------------------------------------------
		declaring-class : com.googlecode.greys.test.TestApp
			   modifier : public
				   name : helloMethod
			 annotation : 
			return-type : void
				 params : com.googlecode.greys.test.TestApp

		---------------------------------------------------------------
		done. method result: match-class=1; match-method=1
```

 - monitor
 ```
 	ga?>monitor -class .*TestApp.* -method .*hello.* -cycle 10
		---------------------------------------------------------------
		done. probe:c-Cnt=1,m-Cnt=1

		+---------------------+-----------------------------------+-------------+-------+---------+------+------+-----------+
		|           timestamp |                             class |    behavior | total | success | fail |   rt | fail-rate |
		+---------------------+-----------------------------------+-------------+-------+---------+------+------+-----------+
		| 2013-02-19 23:46:22 | com.googlecode.greys.test.TestApp | helloMethod |     5 |       5 |    0 | 0.00 |     0.00% |
		+---------------------+-----------------------------------+-------------+-------+---------+------+------+-----------+

		+---------------------+-----------------------------------+-------------+-------+---------+------+------+-----------+
		|           timestamp |                             class |    behavior | total | success | fail |   rt | fail-rate |
		+---------------------+-----------------------------------+-------------+-------+---------+------+------+-----------+
		| 2013-02-19 23:46:32 | com.googlecode.greys.test.TestApp | helloMethod |    12 |      12 |    0 | 0.00 |     0.00% |
		+---------------------+-----------------------------------+-------------+-------+---------+------+------+-----------+
```
		
 - watch
```
		ga?>watch -class .*TestApp.* -method .*hello.* -exp p.parameters[0].str -watch-point before
		---------------------------------------------------------------
		done. probe:c-Cnt=1,m-Cnt=1

		helloworld!
		hellogreys
		testParameter[0]
```

 - javascript
```
	js脚本,在方法执行前，打印入参对象的str字段。方法执行后打印返回结果
	function before(p,o,t){
		o.println('this is before function:firstParameterStr='+p.parameters[0].str+'\n');
	}
	function finished(p,o,t){
		o.println('this is finished function:returnObj='+p.returnObj+'\n');
	} 
```
```
	命令
	ga?>javascript -class .*TestApp.* -method .*hello.* -file /home/jiangyi/greys/testjs
	---------------------------------------------------------------
	done. probe:c-Cnt=1,m-Cnt=1
	this is before function:firstParameterStr=hello
	this is finished function:returnObj=world!
	this is before function:firstParameterStr=nihao
	this is finished function:returnObj=world!
```
## 吐槽反馈
  文档看不懂，与木有！
  功能不完善啊，有木有！
  发现一坨bug啊，有木有！
  我佛慈悲，吐槽之后请反馈给我^_^~ 
  mailTo: jiangyi.ctd@taobao.com/chengtongda@163.com

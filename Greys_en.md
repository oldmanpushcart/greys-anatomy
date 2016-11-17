### Greys online diagnosis tools on Java
---
> why there has tons of exception in production server?
>
> who change the production data
>
> what's the reason of system calling failed
>
> What's the root cause of a recurse exception
>
> Hundreds production servers breakdown just like an avalanche, what happens? the murder of the server is application itself or the hardware?

All these questions will be answered if you come to learn the Greys.

#### The birth
earlier time, we use BTrace to diagnosis online issue. In the time of praising how powerful BTrace is, we also cause online accidents due to unstable BTrace scripts. So in 2012, Taobao developer team deliveried HouseMD, combine some usual BTrace scripts feature together. But HouseMD is written by Scala, which we were not familiar with. And sometimes we had to admire the HouseMD but can't add any customized features.
Then the Greys is born! 

PS: so far Greys support JDK 6+ ONLY, and didn't have Windows version.

#### What's Greys
Greys is a JVM diagnosis tool in process level, without interruptting the JVM execution it could do lots of JVM tuning and diagnosis work.

> Same as HouseMD, the name of 'Greys' coming from the USA TV show "Grey's Anatomy" with respect. Some coding ideas also refer from HouseMD and BTrace too.

#### Target User
> Sometimes you need to locate an online issue, there has parameters but no logs, so writing code, build it and deploy it, hours past and your problem still have no progress. 
> 
> You try to avoid this situation, so you put debug level log in your parameters and return values, but this time, the issue is caused by external module.
> 
> Suddenly there has a performance issue online, and you can't find out the root cause, just use JStack repeatly, try to get a chance, does there have a better choice?

if you was bothered by above problems, then you are the target user of Greys, it implement diagnosising with the JDK 6 feature, Instrumentation, enhance your working classes dynamically, and get what you want.

#### Highlights
- ClassLoader Isolation

we spent lots of effert on classloader isolation during the design and coding phase. You can use Greys anywhere and no need to warry about the class interference or conflict.

- Runtime loading

If your JVM is running on JDK 6+, and the user has same privilege with JVM, then you can run Greys without restart JVM, it's runtime loading and diagnosising.

- Command for usual JVM problems

The biggest difference between Greys and BTrace or HouseMD is, I build my personal develop and diagnosis experience in this tool, and make them be common command, sharing it.

- Expression supporting

Comparing with BTrace, HouseMD is more powerful to define a class or method to be intercepted, but it can't view the intercepted object detail, condition filtering either. User could create the BTrace scripts by themselves, which is more flexible for filtering or viewing inner object, but script is hard to learn and maintain.

Using expression in Greys take the advantages and more convenient, by the way OGNL expression is supported currently;

- Multiple user access

The biggest problem of remote debugging is, only one user could connect to the debugging port, and the breakpoint would break every signle thread if the setting of debugging is unsuited, and the business flow will be affected.

The idea of Greys is being an observer, the breakpoint will not break any flow, but user could watch the detail data which was intercepted by the breakpoint.

- High performance

Using ASM library to enhance java bytecode, the core data structure was optimized for the actual scenario, you can use Greys even the environment load is tight.  

- Pure Java 

The purpose of Greys is being "Professional JVM problem locating tool", since it's for JVM, and I also want to share my Java experience and ideas, make more Java programer get benefits.

So Greys is written by 100% pure Java, and any code contribution is welcome!

#### Inappropriate Case / Limitation

Greys is not suitable for every case, and for the belowing scenarios, you should use more professional tools.

- Performance cost tuning

Performance tuning requires professional tools, I use JProfiler mostly. Although Greys has little performance cost, but the analyzing is too simple, only for performance cost locating, and professional tools is better.

- Remote debugging

Greys can only substitute some remote debugging functionality, for example variable watching, but it's not a real debugging tool, which could step into or watching local variable. 

- Large scale deployment

Same as BTrace, Greys need higher privilege, so large scale deployment is too dangerous for hacking. In future we plan to add permisson management module.

- Class anaylsis on JDK lib

JDK lib is in rt.jar, and will be loaded into BootStrapClassLoader after JVM lunched(HotSpotJVM). Because Greys also need to run on JVM, so by default we shutdown the enhancing of JDK classes.
Except of Springframework, Ibatis, Tomcat and other third class library.

- Other`

BTrace, HouseMD, Greys such tools all affect PermGen and CodeCache, so if your application is sensitive on them, please do not use it.

#### Our motto

Let code do the coding things.

#### Features

- Commandline interactive

  - Commandline
	
	In most cases, we are analyze problem remotely(if it's local debugging is better), and most Java application will run in Linux/BSD or other Unix like system, so commandline is the first choice, and the best too. 
	
  - UI

  In version 2.x.x.x we will support web accessing, using HTTP and websocket to communicate with background service, ETA this year.
  
- Usage
  
  - Watching informace of JVM loaded classes and method
  - Method execution monitoring
	 - QPS, successful rate, response time  
  - Method execution context watching  
     - Parameter, return value, exception, execution replay supported  
  - Performance cost  
	 - trace method in execution tree, check the calling stack and cost time.   
  - Watching method stacktrace
- Advantages
  - Pure Java implementation, open-sourcing
  - Easily deployment, only a jar
  - Diagnosis without restart JVM
  - Variable/Parameters watching
  - OGNL expression, conditional filter, detail data watching
  - Combine common command together, monitor、trace ie.
  - Time tunel, command 'tt' could record every single method execution detail by time
  - Multiple user supporting
  
  >


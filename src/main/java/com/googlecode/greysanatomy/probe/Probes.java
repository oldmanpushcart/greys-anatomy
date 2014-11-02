package com.googlecode.greysanatomy.probe;

import com.googlecode.greysanatomy.probe.Advice.Target;
import javassist.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.googlecode.greysanatomy.probe.ProbeJobs.getJobListeners;
import static com.googlecode.greysanatomy.probe.ProbeJobs.isListener;
import static java.lang.String.format;
import static javassist.Modifier.*;

/**
 * 探测点触发者<br/>
 * 在埋的点中，一共有4种探测点，他们分别对应<br/>
 * fucntion f()
 * {
 * // probe:_before()
 * try {
 * do something...
 * // probe:_success()
 * } catch(Throwable t) {
 * // probe:_throws();
 * throw t;
 * } finally {
 * // probe:_finish();
 * }
 * <p/>
 * }
 *
 * @author vlinux
 */
public class Probes {

    private static final Logger logger = LoggerFactory.getLogger("greysanatomy");

    private static final String jobsClass = "com.googlecode.greysanatomy.probe.ProbeJobs";
    private static final String probesClass = "com.googlecode.greysanatomy.probe.Probes";

    private static Target newTarget(String targetClassName, String targetBehaviorName, Object targetThis) {
        return new Target(targetClassName, targetBehaviorName, targetThis);
    }

    public static void doBefore(String id, String targetClassName, String targetBehaviorName, Object targetThis, Object[] args) {
        if (isListener(id, AdviceListener.class)) {
            try {
                Advice p = new Advice(newTarget(targetClassName, targetBehaviorName, targetThis), args, false);
                ((AdviceListener) getJobListeners(id)).onBefore(p);
            } catch (Throwable t) {
                logger.warn("error at doBefore", t);
            }
        }
    }

    public static void doSuccess(String id, String targetClassName, String targetBehaviorName, Object targetThis, Object[] args, Object returnObj) {
        if (isListener(id, AdviceListener.class)) {
            try {
                Advice p = new Advice(newTarget(targetClassName, targetBehaviorName, targetThis), args, false);
                p.setReturnObj(returnObj);
                ((AdviceListener) getJobListeners(id)).onSuccess(p);
            } catch (Throwable t) {
                logger.warn("error at onSuccess", t);
            }
            doFinish(id, targetClassName, targetBehaviorName, targetThis, args, returnObj, null);
        }

    }

    public static void doException(String id, String targetClassName, String targetBehaviorName, Object targetThis, Object[] args, Throwable throwException) {
        if (isListener(id, AdviceListener.class)) {
            try {
                Advice p = new Advice(newTarget(targetClassName, targetBehaviorName, targetThis), args, false);
                p.setThrowException(throwException);
                ((AdviceListener) getJobListeners(id)).onException(p);
            } catch (Throwable t) {
                logger.warn("error at onException", t);
            }
            doFinish(id, targetClassName, targetBehaviorName, targetThis, args, null, throwException);
        }

    }

    public static void doFinish(String id, String targetClassName, String targetBehaviorName, Object targetThis, Object[] args, Object returnObj, Throwable throwException) {
        if (isListener(id, AdviceListener.class)) {
            try {
                Advice p = new Advice(newTarget(targetClassName, targetBehaviorName, targetThis), args, true);
                p.setThrowException(throwException);
                p.setReturnObj(returnObj);
                ((AdviceListener) getJobListeners(id)).onFinish(p);
            } catch (Throwable t) {
                logger.warn("error at onFinish", t);
            }
        }
    }


    /**
     * 是否过滤掉当前探测的目标
     *
     * @param cc
     * @param cb
     * @return
     */
    private static boolean isIngore(CtClass cc, CtBehavior cb) {

        final int ccMod = cc.getModifiers();
        final int cbMod = cb.getModifiers();

        if (isInterface(ccMod)
                || isAbstract(cbMod)
                || cc.getName().startsWith("com.googlecode.greysanatomy.")) {
            return true;
        }

        return false;

    }


    /**
     * 埋点探测器
     *
     * @param id
     * @param cc
     * @param cb
     * @throws CannotCompileException
     * @throws NotFoundException
     */
    public static void mine(String id, CtClass cc, CtBehavior cb) throws CannotCompileException, NotFoundException {

        if (isIngore(cc, cb)) {
            return;
        }

        // 目标实例,如果是静态方法，则为null
        final String javassistThis = isStatic(cb.getModifiers()) ? "null" : "this";

        // 埋点通知
        if (isListener(id, AdviceListener.class)) {
            // 构造函数在这里是不能做insertBefore的,所以构造函数的before是做在doCache中
            if (cb.getMethodInfo().isMethod()) {
                mineProbeForMethod(cb, id, cc.getName(), cb.getName(), javassistThis);
            } else if (cb.getMethodInfo().isConstructor()) {
                mineProbeForConstructor(cb, id, cc.getName(), cb.getName(), javassistThis);
            }
        }

    }

    private static void mineProbeForConstructor(CtBehavior cb, String id, String targetClassName, String targetBehaviorName, String javassistThis) throws CannotCompileException, NotFoundException {
        cb.addCatch(format("{if(%s.isJobAlive(\"%s\")){%s.doBefore(\"%s\",\"%s\",\"%s\",%s,$args);%s.doException(\"%s\",\"%s\",\"%s\",%s,$args,$e);}throw $e;}",
                        jobsClass, id,
                        probesClass, id, targetClassName, targetBehaviorName, javassistThis,
                        probesClass, id, targetClassName, targetBehaviorName, javassistThis),
                ClassPool.getDefault().get("java.lang.Throwable"));

        // TODO : 奇怪，为啥这里要doBefore两次?
        cb.insertAfter(format("{if(%s.isJobAlive(\"%s\")){%s.doBefore(\"%s\",\"%s\",\"%s\",%s,$args);%s.doSuccess(\"%s\",\"%s\",\"%s\",%s,$args,($w)$_);}}",
                jobsClass, id,
                probesClass, id, targetClassName, targetBehaviorName, javassistThis,
                probesClass, id, targetClassName, targetBehaviorName, javassistThis));

    }

    private static void mineProbeForMethod(CtBehavior cb, String id, String targetClassName, String targetBehaviorName, String javassistThis) throws CannotCompileException, NotFoundException {

        cb.insertBefore(format("{if(%s.isJobAlive(\"%s\"))%s.doBefore(\"%s\",\"%s\",\"%s\",%s,$args);}",
                jobsClass, id,
                probesClass, id, targetClassName, targetBehaviorName, javassistThis));

        cb.addCatch(format("{if(%s.isJobAlive(\"%s\"))%s.doException(\"%s\",\"%s\",\"%s\",%s,$args,$e);throw $e;}",
                        jobsClass, id,
                        probesClass, id, targetClassName, targetBehaviorName, javassistThis),
                ClassPool.getDefault().get("java.lang.Throwable"));

        cb.insertAfter(format("{if(%s.isJobAlive(\"%s\"))%s.doSuccess(\"%s\",\"%s\",\"%s\",%s,$args,($w)$_);}",
                jobsClass, id,
                probesClass, id, targetClassName, targetBehaviorName, javassistThis));
    }

}

package com.github.ompc.greys.core.js;

import com.github.ompc.greys.core.util.GaStringUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.script.*;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * JavaScript测试用例
 * Created by vlinux on 16/2/16.
 */
@Ignore
public class JavaScriptTestCase {

    protected ScriptEngine scriptEngine;
    protected Compilable compilable;
    protected Invocable invocable;

    @Before
    public void before() throws IOException, ScriptException {
        final ScriptEngineManager mgr = new ScriptEngineManager();
        this.scriptEngine = mgr.getEngineByMimeType("application/javascript");
        this.compilable = (Compilable) scriptEngine;
        this.invocable = (Invocable) scriptEngine;
        loadJavaScriptSupport();
    }

    /*
     * 加载JavaScriptSupport
     */
    private void loadJavaScriptSupport() throws IOException, ScriptException {
        compilable.compile(IOUtils.toString(
                GaStringUtils.class.getResourceAsStream("/com/github/ompc/greys/core/res/javascript/javascript-support.js"),
                Charset.forName("UTF-8")
        )).eval();

        compilable.compile(IOUtils.toString(
                GaStringUtils.class.getResourceAsStream("/com/github/ompc/greys/core/res/javascript/greys-module.js"),
                Charset.forName("UTF-8")
        )).eval();
    }

    @After
    public void after() {
        this.scriptEngine = null;
        this.compilable = null;
        this.invocable = null;
    }

    @Test
    public void test_compile_javascript_support_success() throws IOException, ScriptException, NoSuchMethodException {

        invocable.invokeFunction("__greys_load", "/tmp/logger.js", "UTF-8");
        invocable.invokeFunction("__greys_module_returning", null,null,null);

    }

}

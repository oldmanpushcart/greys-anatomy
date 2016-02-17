package com.github.ompc.greys.core.js;

import com.github.ompc.greys.core.util.GaStringUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.script.*;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * JavaScript测试用例
 * Created by vlinux on 16/2/16.
 */
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
        final String javascriptSupportContent = IOUtils.toString(
                GaStringUtils.class.getResourceAsStream("/com/github/ompc/greys/core/res/javascript/javascript-support2.js"),
                Charset.forName("UTF-8")
        );
        compilable.compile(javascriptSupportContent).eval();
    }

    @After
    public void after() {
        this.scriptEngine = null;
        this.compilable = null;
        this.invocable = null;
    }

    @Test
    public void test_compile_javascript_support_success() throws IOException, ScriptException {




    }

}

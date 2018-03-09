package com.github.ompc.greys.protocol.v1;

import com.github.ompc.greys.protocol.GpParameterizedBaseTestCase;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

public class GpV100TestCase extends GpParameterizedBaseTestCase {

    @Parameterized.Parameters(name="{0}")
    public static List<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"GpText.json"},
                {"GpThanks.json"},
                {"GpClassInfo__java_lang_String.json"},
                {"GpClassInfo__javax_swing_JFrame.json"},
        });
    }

    public GpV100TestCase(String name) {
        super("v1/"+name);
    }

}

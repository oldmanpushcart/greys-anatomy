package com.github.ompc.greys.protocol;

public class GpConstants {

    public static final String EMPTY_STRING = "";
    public static final int EMPTY_CLASS_TYPE = 0 ;

    public static final String GP_VERSION_1_0_0 = "1.0.0";

    private static final int CLASS_TYPE = 0x01;
    public static final int CLASS_TYPE_INTERFACE = CLASS_TYPE << 0;
    public static final int CLASS_TYPE_ANNOTATION = CLASS_TYPE << 1;
    public static final int CLASS_TYPE_ENUM = CLASS_TYPE << 2;
    public static final int CLASS_TYPE_ANONYMOUS = CLASS_TYPE << 3;
    public static final int CLASS_TYPE_ARRAY = CLASS_TYPE << 4;
    public static final int CLASS_TYPE_LOCAL = CLASS_TYPE << 5;
    public static final int CLASS_TYPE_MEMBER = CLASS_TYPE << 6;
    public static final int CLASS_TYPE_PRIMITIVE = CLASS_TYPE << 7;
    public static final int CLASS_TYPE_SYNTHETIC = CLASS_TYPE << 8;

    public static final int markClassType(final boolean except, final int classTypeMask) {
        return except
                ? classTypeMask
                : 0;
    }


}

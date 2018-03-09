package com.github.ompc.greys.protocol;

import com.github.ompc.greys.protocol.impl.v1.*;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static com.github.ompc.greys.protocol.GpConstants.GP_VERSION_1_0_0;
import static com.github.ompc.greys.protocol.GpType.*;

public class GpSerializer {

    private static Map<GpType, Class<?>> gpTypeClassMapV100 = new HashMapBuilder<GpType, Class<?>>()
            .building(BEHAVIOR_INFO, BehaviorInfo.class)
            .building(CLASS_INFO, ClassInfo.class)
            .building(PROGRESS, Process.class)
            .building(TERMINATE, Terminate.class)
            .building(TEXT, Text.class)
            .building(THANKS, Thanks.class)
            .building(TRACE, Trace.class);

    private static Map<String, Map<GpType, Class<?>>> gpVersionMap = new HashMapBuilder<String, Map<GpType, Class<?>>>()
            .building(GP_VERSION_1_0_0, gpTypeClassMapV100);

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .registerTypeHierarchyAdapter(GreysProtocol.class, new JsonDeserializer<GreysProtocol<?>>() {
                @Override
                public GreysProtocol<?> deserialize(final JsonElement jsonElement,
                                                    final Type type,
                                                    final JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                    final JsonObject jsonObject = jsonElement.getAsJsonObject();
                    final String version = jsonObject.get("version").getAsString();
                    final Map<GpType, Class<?>> gpTypeClassMap = gpVersionMap.get(version);
                    if (null == gpTypeClassMap) {
                        throw new JsonParseException("unsupported version : " + version);
                    }
                    final GpType gpType = jsonDeserializationContext.deserialize(jsonObject.get("type"), GpType.class);
                    final Class<?> contentClass = gpTypeClassMap.get(gpType);
                    if (null == contentClass) {
                        throw new JsonParseException("unsupported type : " + jsonObject.get("type"));
                    }
                    final Object content = jsonDeserializationContext.deserialize(jsonObject.get("content"), contentClass);
                    return new GreysProtocol<Object>(version, gpType, content);
                }
            })
            .create();

    public static String serialize(final GreysProtocol<?> gp) {
        return gson.toJson(gp);
    }

    public static <T> GreysProtocol<T> deserialize(final String json) {
        return gson.fromJson(json, GreysProtocol.class);
    }

    static class HashMapBuilder<K, V> extends HashMap<K, V> {

        HashMapBuilder<K, V> building(K k, V v) {
            put(k, v);
            return this;
        }

    }

}

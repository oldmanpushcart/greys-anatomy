package com.github.ompc.greys.protocol;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

@Ignore
@RunWith(Parameterized.class)
public class GpParameterizedBaseTestCase {

    private final String path;

    public GpParameterizedBaseTestCase(String name) {
        this.path = "/com/github/ompc/greys/protocol/" + name;
    }

    @Test
    public void test_for_gpJsonExisted() throws IOException {
        final String gpJson = getJsonWithOutException(path);
        Assert.assertNotNull(gpJson);
    }

    @Test
    public void test_for_deserializeGp() throws IOException {
        final String gpJson = getJsonWithOutException(path);
        final GreysProtocol<?> gp = deserializeWithOutException(gpJson);
        Assert.assertNotNull(gp);
    }

    @Test
    public void test_for_serializeGp() throws IOException {
        final String gpJson = getJsonWithOutException(path);
        final GreysProtocol<?> gp = deserializeWithOutException(gpJson);
        final String gpJsonClone = serializeWithOutException(gp);
        Assert.assertNotNull(gpJsonClone);
    }

    @Test
    public void test_for_fullEquals() throws IOException {
        final String gpJson = getJsonWithOutException(path);
        final GreysProtocol<?> gp = deserializeWithOutException(gpJson);
        final String gpJsonClone = serializeWithOutException(gp);
        GreysProtocol<?> gpClone = deserializeWithOutException(gpJsonClone);
        assertEquals("serialize gpJson:" + path + ", but not the equals after serialize/deserialize",
                gp, gpClone);
    }

    private String getJsonWithOutException(final String path) throws IOException {
        final InputStream is = getClass().getResourceAsStream(path);
        try {
            return IOUtils.toString(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private <T> GreysProtocol<T> deserializeWithOutException(final String gpJson) {
        return GpSerializer.deserialize(gpJson);
    }

    private String serializeWithOutException(final GreysProtocol<?> gp) {
        final String gpJson = GpSerializer.serialize(gp);
        Assert.assertNotNull(gpJson);
        return gpJson;
    }

}

package com.googlecode.greysanatomy.util;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class HostUtilsTest {

    @Test
    public void testGetAllLocalHostIP() throws Exception {
        Set<String> ips = HostUtils.getAllLocalHostIP();
        assertTrue(ips.size() > 1);
    }
}
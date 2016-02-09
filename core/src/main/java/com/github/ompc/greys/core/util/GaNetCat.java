package com.github.ompc.greys.core.util;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * GaNetCat封装<br/>
 * 这个代码不要看,不要看,不要看...不是我写的...
 * Created by vlinux on 16/2/4.
 */
public class GaNetCat {

    public static void main(String... args) throws IOException {

        final InputStream is = System.in;
        final OutputStream os = System.out;
        final Socket socket = new Socket();
        try {

            socket.connect(new InetSocketAddress(args[0], Integer.valueOf(args[1])));
            final InputStream nis = socket.getInputStream();
            final OutputStream nos = socket.getOutputStream();

            final byte[] dataArray = new byte[1024];
            int length = 0;
            // do write
            do {
                length = is.read(dataArray);
                if (length <= 0) {
                    break;
                }
                nos.write(dataArray, 0, length);
                nos.flush();
            } while (length > 0);

            // do read
            do {
                length = nis.read(dataArray);
                if (length == 1
                        && dataArray[0] == 0x04) {
                    os.flush();
                    break;
                }
                if (length <= 0) {
                    break;
                }
                os.write(dataArray, 0, length);
            } while (length > 0);

        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);

            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
        }

    }

}

package com.googlecode.greysanatomy.server;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * GaServer²Ù×÷µÄ¸½¼þ
 * Created by vlinux on 15/5/3.
 */
public class GaAttachment {

    private final int bufferSize;
    private final GaSession gaSession;

    private LineDecodeState lineDecodeState;
    private ByteBuffer lineByteBuffer;


    public GaAttachment(int bufferSize, GaSession gaSession) {
        this.lineByteBuffer = ByteBuffer.allocate(bufferSize);
        this.bufferSize = bufferSize;
        this.lineDecodeState = LineDecodeState.READ_CHAR;
        this.gaSession = gaSession;
    }

    public LineDecodeState getLineDecodeState() {
        return lineDecodeState;
    }


    public void setLineDecodeState(LineDecodeState lineDecodeState) {
        this.lineDecodeState = lineDecodeState;
    }

    public void put(byte data) {
        if (lineByteBuffer.hasRemaining()) {
            lineByteBuffer.put(data);
        } else {
            final ByteBuffer newLineByteBuffer = ByteBuffer.allocate(lineByteBuffer.capacity() + bufferSize);
            lineByteBuffer.flip();
            newLineByteBuffer.put(lineByteBuffer);
            newLineByteBuffer.put(data);
            this.lineByteBuffer = newLineByteBuffer;
        }
    }

    public String clearAndGetLine(Charset charset) {
        lineByteBuffer.flip();
        final byte[] dataArray = new byte[lineByteBuffer.limit()];
        lineByteBuffer.get(dataArray);
        final String line = new String(dataArray, charset);
        lineByteBuffer.clear();
        return line;
    }

    public GaSession getGaSession() {
        return gaSession;
    }

}

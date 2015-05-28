package com.github.ompc.greys.advisor;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * ASM代码锁<br/>
 * Created by vlinux on 15/5/28.
 */
public class AsmCodeLock implements CodeLock, Opcodes {

    private final AdviceAdapter aa;

    // 锁记数
    private int lock;

    // 代码匹配索引
    private int index = 0;

    private int[] BEGIN_IDENTI_CODES = new int[]{
            ACONST_NULL, POP,
            ACONST_NULL, ACONST_NULL, POP2,
            ACONST_NULL, ACONST_NULL, ACONST_NULL, POP2, POP,
            ACONST_NULL, ACONST_NULL, ACONST_NULL, ACONST_NULL, POP2, POP2
    };

    private int[] END_IDENTI_CODES = new int[]{
            ACONST_NULL, ACONST_NULL, ACONST_NULL, ACONST_NULL, POP2, POP2,
            ACONST_NULL, ACONST_NULL, ACONST_NULL, POP2, POP,
            ACONST_NULL, ACONST_NULL, POP2,
            ACONST_NULL, POP,
    };


    public AsmCodeLock(AdviceAdapter aa) {
        this.aa = aa;
    }

    private void pushNull() {
        aa.push((Type) null);
    }

    @Override
    public void code(int code) {

        final int[] codes = isLock() ? END_IDENTI_CODES : BEGIN_IDENTI_CODES;

        if (index >= codes.length) {
            reset();
            return;
        }

        if (codes[index] != code) {
            reset();
            return;
        }

        if (++index == codes.length) {
            if (isLock()) {
                lock--;
            } else {
                lock++;
            }
            reset();
        }

    }

    /*
     * 重置索引<br/>
     * 一般在代码序列判断失败时，则会对索引进行重置，冲头开始匹配特征序列
     */
    private void reset() {
        index = 0;
    }


    private void asm(int opcode) {
        switch (opcode) {

            case ACONST_NULL: {
                pushNull();
                break;
            }

            case POP: {
                aa.pop();
                break;
            }

            case POP2: {
                aa.pop2();
                break;
            }

        }
    }

    /**
     * 锁定序列
     */
    private void lock() {
        for (int op : BEGIN_IDENTI_CODES) {
            asm(op);
        }
    }

    /*
     * 解锁序列
     */
    private void unLock() {
        for (int op : END_IDENTI_CODES) {
            asm(op);
        }
    }

    @Override
    public boolean isLock() {
        return lock > 0;
    }

    @Override
    public void lock(Block block) {
        lock();
        try {
            block.code();
        } finally {
            unLock();
        }
    }

}

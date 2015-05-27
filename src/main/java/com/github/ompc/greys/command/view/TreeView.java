package com.github.ompc.greys.command.view;

import java.util.ArrayList;
import java.util.List;

/**
 * 树形控件
 * Created by vlinux on 15/5/26.
 */
public class TreeView implements View {

    private static final String STEP_FIRST_CHAR  = "`---";
    private static final String STEP_NORMAL_CHAR = "+---";
    private static final String STEP_HAS_BOARD   = "|   ";
    private static final String STEP_EMPTY_BOARD = "    ";

    // 根节点
    private final Node root;

    // 当前节点
    private Node current;

    public TreeView(String title) {
        this.root = new Node(title);
        this.current = root;
    }

    @Override
    public String draw() {

        final StringBuilder treeSB = new StringBuilder();
        recursive(0, true, "", root, new Callback() {

            @Override
            public void callback(int deep, boolean isLast, String prefix, Node node) {
                treeSB.append(prefix).append(isLast ? STEP_FIRST_CHAR : STEP_NORMAL_CHAR).append(node.data).append("\n");
            }

        });

        return treeSB.toString();
    }

    /**
     * 递归遍历
     */
    private void recursive(int deep, boolean isLast, String prefix, Node node, Callback callback) {
        callback.callback(deep, isLast, prefix, node);
        if (!node.isLeaf()) {
            final int size = node.children.size();
            for (int index = 0; index < size; index++) {
                final boolean isLastFlag = index == size - 1;
                final String currentPrefix = isLast ? prefix + STEP_EMPTY_BOARD : prefix + STEP_HAS_BOARD;
                recursive(
                        deep + 1,
                        isLastFlag,
                        currentPrefix,
                        node.children.get(index),
                        callback
                );
            }
        }
    }


    /**
     * 创建一个分支节点
     *
     * @param data 节点数据
     * @return this
     */
    public TreeView begin(String data) {
        current = new Node(current, data);
        return this;
    }

    /**
     * 结束一个分支节点
     *
     * @return this
     */
    public TreeView end() {
        if (current.isRoot()) {
            throw new IllegalStateException("current node is root.");
        }
        current = current.parent;
        return this;
    }


    /**
     * 树节点
     */
    private class Node {

        final Node parent;

        /**
         * 节点数据
         */
        final String data;

        /**
         * 子节点
         */
        final List<Node> children = new ArrayList<Node>();

        /**
         * 构造树节点(根节点)
         */
        private Node(String data) {
            this.parent = null;
            this.data = data;
        }

        /**
         * 构造树节点
         *
         * @param parent 父节点
         * @param data   节点数据
         */
        private Node(Node parent, String data) {
            this.parent = parent;
            this.data = data;
            parent.children.add(this);
        }

        /**
         * 是否根节点
         *
         * @return true / false
         */
        boolean isRoot() {
            return null == parent;
        }

        /**
         * 是否叶子节点
         *
         * @return true / false
         */
        boolean isLeaf() {
            return children.isEmpty();
        }

    }


    /**
     * 遍历回调接口
     */
    private interface Callback {

        void callback(int deep, boolean isLast, String prefix, Node node);

    }


    public static void main(String... args) {

        final TreeView view = new TreeView("TEST");

        view
                .begin("SayService:say()")
                .begin("SayService:say1()")
                .begin("SayService:say2()")
                .begin("PrintWriter:println()")
                .begin("PrintWriter:println()").end()
                .begin("PrintWriter:println()").end()
                .end()
                .begin("PrintWriter:println()").end()
                .end()
                .begin("PrintWriter:println()").end()
                .begin("PrintWriter:println()").end()
                .end()
                .end()
                .begin("PrintWriter:println()").end()

        ;

        System.out.println(view.draw());

    }

}

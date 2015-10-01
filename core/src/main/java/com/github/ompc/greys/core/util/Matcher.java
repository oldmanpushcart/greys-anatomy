package com.github.ompc.greys.core.util;

import java.util.Arrays;
import java.util.List;

import static com.github.ompc.greys.core.util.GaCheckUtils.isEquals;

/**
 * 匹配器
 * Created by vlinux on 15/5/17.
 */
public interface Matcher {

    /**
     * 是否匹配
     *
     * @param target 目标字符串
     * @return 目标字符串是否匹配表达式
     */
    boolean matching(String target);

    /**
     * 组关系枚举
     */
    enum GroupRelation {

        /**
         * 与
         */
        AND,

        /**
         * 或
         */
        OR
    }

    abstract class GroupMatcher implements Matcher {

        private final GroupRelation relation;
        private final List<Matcher> matcherList;

        public GroupMatcher(GroupRelation relation, List<Matcher> matcherList) {
            this.relation = relation;
            this.matcherList = matcherList;
        }

        @Override
        public boolean matching(String target) {

            // and
            if (relation.equals(GroupRelation.AND)) {
                for (Matcher matcher : matcherList) {
                    if (!matcher.matching(target)) {
                        return false;
                    }
                }
                return true;
            }

            // or
            else if (relation.equals(GroupRelation.OR)) {
                for (Matcher matcher : matcherList) {
                    if (matcher.matching(target)) {
                        return true;
                    }
                }
                return false;
            }

            // others
            return false;

        }
    }

    class GroupAndMatcher extends GroupMatcher {

        public GroupAndMatcher(Matcher... matcherArray) {
            super(GroupRelation.AND, Arrays.asList(matcherArray));
        }
    }

    class GroupOrMatcher extends GroupMatcher {

        public GroupOrMatcher(Matcher... matcherArray) {
            super(GroupRelation.OR, Arrays.asList(matcherArray));
        }
    }


    /**
     * 字符串全匹配
     */
    class EqualsMatcher implements Matcher {

        private final String pattern;

        public EqualsMatcher(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean matching(String target) {
            return isEquals(target, pattern);
        }
    }

    /**
     * 正则表达式匹配
     */
    class RegexMatcher implements Matcher {

        private final String pattern;

        public RegexMatcher(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean matching(String target) {
            return null != target
                    && null != pattern
                    && target.matches(pattern);
        }
    }


    /**
     * 通配符表达式匹配
     */
    class WildcardMatcher implements Matcher {

        private final String pattern;

        public WildcardMatcher(String pattern) {
            this.pattern = pattern;
        }


        @Override
        public boolean matching(String target) {
            return match(target, pattern, 0, 0);
        }

        /**
         * Internal matching recursive function.
         */
        private boolean match(String string, String pattern, int stringStartNdx, int patternStartNdx) {
            int pNdx = patternStartNdx;
            int sNdx = stringStartNdx;
            int pLen = pattern.length();
            if (pLen == 1) {
                if (pattern.charAt(0) == '*') {     // speed-up
                    return true;
                }
            }
            int sLen = string.length();
            boolean nextIsNotWildcard = false;

            while (true) {

                // check if end of string and/or pattern occurred
                if ((sNdx >= sLen)) {   // end of string still may have pending '*' callback pattern
                    while ((pNdx < pLen) && (pattern.charAt(pNdx) == '*')) {
                        pNdx++;
                    }
                    return pNdx >= pLen;
                }
                if (pNdx >= pLen) {         // end of pattern, but not end of the string
                    return false;
                }
                char p = pattern.charAt(pNdx);    // pattern char

                // perform logic
                if (!nextIsNotWildcard) {

                    if (p == '\\') {
                        pNdx++;
                        nextIsNotWildcard = true;
                        continue;
                    }
                    if (p == '?') {
                        sNdx++;
                        pNdx++;
                        continue;
                    }
                    if (p == '*') {
                        char pnext = 0;           // next pattern char
                        if (pNdx + 1 < pLen) {
                            pnext = pattern.charAt(pNdx + 1);
                        }
                        if (pnext == '*') {         // double '*' have the same effect as one '*'
                            pNdx++;
                            continue;
                        }
                        int i;
                        pNdx++;

                        // find recursively if there is any substring from the end of the
                        // line that matches the rest of the pattern !!!
                        for (i = string.length(); i >= sNdx; i--) {
                            if (match(string, pattern, i, pNdx)) {
                                return true;
                            }
                        }
                        return false;
                    }
                } else {
                    nextIsNotWildcard = false;
                }

                // check if pattern char and string char are equals
                if (p != string.charAt(sNdx)) {
                    return false;
                }

                // everything matches for now, continue
                sNdx++;
                pNdx++;
            }
        }

    }

}

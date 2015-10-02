package com.github.ompc.greys.core.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
     * 关系枚举
     */
    enum RelationEnum {

        /**
         * 与
         */
        AND,

        /**
         * 或
         */
        OR
    }

    /**
     * 关系匹配
     */
    abstract class RelationMatcher implements Matcher {

        private final RelationEnum relation;
        private final List<Matcher> matcherList;

        public RelationMatcher(RelationEnum relation, List<Matcher> matcherList) {
            this.relation = relation;
            this.matcherList = matcherList;
        }

        @Override
        public boolean matching(String target) {

            // and
            if (relation.equals(RelationEnum.AND)) {
                for (Matcher matcher : matcherList) {
                    if (!matcher.matching(target)) {
                        return false;
                    }
                }
                return true;
            }

            // or
            else if (relation.equals(RelationEnum.OR)) {
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

    /**
     * 与关系匹配
     */
    class RelationAndMatcher extends RelationMatcher {
        public RelationAndMatcher(Matcher... matcherArray) {
            super(RelationEnum.AND, Arrays.asList(matcherArray));
        }
    }

    /**
     * 或关系匹配
     */
    class RelationOrMatcher extends RelationMatcher {
        public RelationOrMatcher(Matcher... matcherArray) {
            super(RelationEnum.OR, Arrays.asList(matcherArray));
        }
    }

    /**
     * 永远匹配
     */
    class TrueMatcher implements Matcher {

        @Override
        public boolean matching(String target) {
            return true;
        }
    }

    /**
     * 带缓存的匹配
     */
    class CacheMatcher implements Matcher {

        private final Matcher matcher;
        private final Map<String, Boolean> cacheMap;

        public CacheMatcher(Matcher matcher, Map<String, Boolean> cacheMap) {
            this.matcher = matcher;
            this.cacheMap = cacheMap;
        }

        @Override
        public boolean matching(String target) {

            final Boolean valueInCache = cacheMap.get(target);
            if (null == valueInCache) {
                final boolean value = matcher.matching(target);
                cacheMap.put(target, value);
                return value;
            } else {
                return valueInCache;
            }

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
     * 模式匹配
     */
    class PatternMatcher implements Matcher {

        private final Matcher matcher;

        public PatternMatcher(boolean isRegEx, String pattern) {
            this.matcher = isRegEx
                    ? new Matcher.RegexMatcher(pattern)
                    : new Matcher.WildcardMatcher(pattern);
        }

        @Override
        public boolean matching(String target) {
            return matcher.matching(target);
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

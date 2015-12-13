package com.github.ompc.greys.core.util.matcher;

/**
 * 模版匹配
 * Created by oldmanpushcart@gmail.com on 15/10/31.
 */
public class PatternMatcher implements Matcher<String> {

    /**
     * 匹配策略
     */
    public enum Strategy {

        /**
         * 通配符匹配
         */
        WILDCARD,

        /**
         * 正则表达式匹配
         */
        REGEX,

        /**
         * 字符串全匹配
         */
        EQUALS

    }

    // 匹配器
    private final Matcher<String> matcher;

    /**
     * 正则/通配符匹配切换构造函数<br/>
     * 这个构造函数的出现主要是迎合Command中的使用需要
     *
     * @param isRegex 是否正则表达式匹配
     * @param pattern 匹配模版
     */
    public PatternMatcher(final boolean isRegex, final String pattern) {
        this(isRegex ? Strategy.REGEX : Strategy.WILDCARD, pattern);
    }

    /**
     * 通用构造函数
     *
     * @param strategy 匹配策略
     * @param pattern  匹配模版
     */
    public PatternMatcher(Strategy strategy, String pattern) {
        switch (strategy) {
            case WILDCARD: {
                this.matcher = new WildcardMatcher(pattern);
                break;
            }
            case REGEX: {
                this.matcher = new RegexMatcher(pattern);
                break;
            }
            case EQUALS: {
                this.matcher = new EqualsMatcher<String>(pattern);
                break;
            }
            default: {
                throw new IllegalArgumentException("unKnow strategy:" + strategy);
            }
        }
    }

    @Override
    public boolean matching(String target) {
        return matcher.matching(target);
    }

    /**
     * 正则表达式匹配
     */
    class RegexMatcher implements Matcher<String> {

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
    class WildcardMatcher implements Matcher<String> {

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

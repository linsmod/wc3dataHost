package com.linsmod.jass;


import com.linsmod.common.Delegate;
import com.linsmod.common.LinqList;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;

public class RuleDef implements StackHolder {
    public String input;
    RuleDef program = null;
    Map<String, Delegate> rules= new HashMap<>();

    int i = 0;
    int ln = 1;
    String line;
    int inlineOffset;

    public Line nextLine() {
        StringBuilder line = new StringBuilder();
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '\r' || c == '\n') {
                return new Line(line, ln++);
            } else {
                line.append(c);
            }
        }
        return null;
    }

    Stack<ParsingDelegate> delegates = new Stack<>();
    ParsingDelegate currentPath;

    public Stack<ParsingDelegate> getDelegates() {
        return delegates;
    }

    public void setDelegates(Stack<ParsingDelegate> delegates) {
        this.delegates = delegates;
    }

    public ParsingDelegate getCurrentPath() {
        return currentPath;
    }

    public void setCurrentPath(ParsingDelegate currentPath) {
        this.currentPath = currentPath;
    }

    RuleDef RULE(String name, Delegate d) {
        ParsingDelegate parsingDelegate = new ParsingDelegate(this) {
            @Override
            public void apply() {
                d.apply();
            }
        };
        this.rules.put(name, parsingDelegate);
        return this;
    }

    void MANY(Delegate d) {
        currentPath.commandList.add(new ParsingDelegate(this) {
            @Override
            public void apply() {
                try {
                    while (true) {
                        d.apply();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    void OR(Delegate... d) {
        currentPath.commandList.add(new ParsingDelegate(this) {
            @Override
            public void apply() {
                for (int j = 0; j < d.length; j++) {
                    try {
                        d[i].apply();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    void SUBRULE(Delegate d, String label) {
        currentPath.commandList.add(new ParsingDelegate(this) {
            @Override
            public void apply() {
                d.apply();
            }
        });
    }

    void SUBRULE(Delegate d) {
        SUBRULE(d, null);
    }

    void CONSUME(TokenDef d, String label) {
        currentPath.commandList.add(new ParsingDelegate(this) {
            @Override
            public void apply() {
                Matcher matcher = d.pattern.matcher(currentPath.currentLine);
                if (matcher.find(inlineOffset)) {
                    currentPath.collector.add(new CandyToken(d, matcher.start(), matcher.end()));
                }
            }
        });
    }

    void CONSUME(TokenDef d) {
        CONSUME(d, null);
    }

    void OPTION(Delegate d, String label) {
        currentPath.commandList.add(new ParsingDelegate(this) {
            @Override
            public void apply() {
                try {
                    d.apply();
                } catch (
                        Exception e) {
                    e.printStackTrace();
                }
            }
        }.label(label));
    }

    void OPTION(Delegate d) {
        OPTION(d, null);
    }

    Delegate REF(String title, String label) {
        return new ParsingDelegate(this) {
            @Override
            public void apply() {
                rules.get(title);
            }
        }.label(label);
    }

    Delegate REF(String title) {
        return REF(title, null);
    }
}

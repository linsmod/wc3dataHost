package com.linsmod.jass;

import com.linsmod.common.Delegate;
import com.linsmod.common.LinqList;

import java.util.function.Function;

public abstract class ParsingDelegate implements Delegate {
    private final StackHolder ruleDef;
    protected CharSequence currentLine;
    private int i;
    private int ln;
    private Function<Integer, Character> charAt;
    private int length;
    private String sLabel;

    public ParsingDelegate(StackHolder ruleDef) {

        this.ruleDef = ruleDef;
    }

    @Override
    public void apply() {

    }

    public void pushStack() {
        this.ruleDef.getDelegates().push(this);
        this.ruleDef.setCurrentPath(this);
    }

    public void popStack() {
        ParsingDelegate pop = this.ruleDef.getDelegates().pop();
        this.ruleDef.setCurrentPath(pop);
    }

    public void run() {
        pushStack();
        apply();
        this.commandList.autoList().foreach(x -> x.run());
        popStack();
    }

    public void lock(RuleDef position) {
        this.i = position.i;
        this.ln = position.ln;
        this.length = position.input.length();
        this.charAt = (i) -> position.input.charAt(i);
    }

    public Line nextLine() {
        StringBuilder line = new StringBuilder();
        while (i < length) {
            char c = this.charAt.apply(i);
            if (c == '\r' || c == '\n') {
                return new Line(line, ln++);
            } else {
                line.append(c);
            }
        }
        return null;
    }

    LinqList<CandyToken> collector = new LinqList<>();
    LinqList<ParsingDelegate> commandList = new LinqList<>();

    public ParsingDelegate label(String label) {
        this.sLabel = label;
        return this;
    }
}

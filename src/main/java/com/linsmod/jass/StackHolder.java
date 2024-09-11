package com.linsmod.jass;

import java.util.Stack;

public interface StackHolder {
    public Stack<ParsingDelegate> getDelegates();

    public void setDelegates(Stack<ParsingDelegate> delegates);

    public ParsingDelegate getCurrentPath();

    public void setCurrentPath(ParsingDelegate currentPath);
}

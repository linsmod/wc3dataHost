package com.linsmod.jass;

public class CandyToken {
    private final TokenDef tokenDef;
    private final int start;
    private final int end;

    public CandyToken(TokenDef tokenDef, int start, int end) {
        this.tokenDef = tokenDef;

        this.start = start;
        this.end = end;
    }
}

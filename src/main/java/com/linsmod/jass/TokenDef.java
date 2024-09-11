package com.linsmod.jass;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenDef {
    final String name;
    final String r;
    final Pattern pattern;
    boolean bSkip;
    boolean bLinebreak;

    public TokenDef(String name, String r) {
        this.name = name;
        this.r = r;
        this.pattern = Pattern.compile(r);
    }

    public TokenDef skip() {
        this.bSkip = true;
        return this;
    }

    public TokenDef linebreak() {
        this.bLinebreak = true;
        return this;
    }

    public void findAll(String input, ArrayList<CandyToken> tokens) {
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            tokens.add(new CandyToken(this,matcher.start(), matcher.end()));
        }
    }
}
package com.linsmod.jass;

import com.linsmod.common.Delegate;
import com.linsmod.common.LinqList;

import java.util.ArrayList;

public class JassParser {
    private static LinqList<TokenDef> tokenDefs = new LinqList<>();

    public static void main(String[] args) {

        new JassParser().parse("");
    }

    void parse(String input) {
        $.input = input;
        ParsingDelegate program = (ParsingDelegate)$.rules.get("program");
        program.run();
    }

    RuleDef $ = new RuleDef();

    JassParser() {
        $.RULE("program", () ->
                $.MANY(() ->
                        $.OR(
                                () -> $.SUBRULE($.REF("typeDefinition")),
                                () -> $.SUBRULE($.REF("nativeBlock")),
                                () -> $.SUBRULE($.REF("globalsBlock")),
                                () -> $.SUBRULE($.REF("functionBlock"))
                        )
                )
        );
        $.RULE("typeDefinition", () -> {
            $.CONSUME(TYPE);
            $.CONSUME(ID);
            $.CONSUME(EXTENDS);
            $.CONSUME(ID);
        });

        $.RULE("type", () ->
                $.OR(
                        () -> $.CONSUME(NOTHING),
                        () -> {
                            $.CONSUME(ID);
                            $.OPTION(() -> $.CONSUME(ARRAY));
                        }));
        $.RULE("globalVar", () -> {
            $.OPTION(() -> $.CONSUME(CONSTANT), "constant");
            // typeName
            $.SUBRULE($.REF("type"), "type");
            // identifier
            $.CONSUME(ID, "label");
            $.OPTION(() -> {
                $.CONSUME(EQUALS);
                $.SUBRULE($.REF("expression"));
            });
        });
        $.RULE("localVars", () -> {
            $.CONSUME(LOCAL);
            // typeName
            $.SUBRULE($.REF("type"), "type");
            // identifier
            $.CONSUME(ID, "label");
            $.OPTION(() -> {
                $.CONSUME(EQUALS);
                $.SUBRULE($.REF("expression"));
            });
        });

        $.RULE("expression", () -> {
            String left = "left";
            String op = "op";
            String right = "right";
            $.OR(() -> $.SUBRULE($.REF("ParentheticalExpression"), left),
                    () -> $.SUBRULE($.REF("NotExpression"), left),
                    () -> $.SUBRULE($.REF("NegateExpression"), left),
                    () -> $.CONSUME(IDLITERAL, left),
                    () -> $.CONSUME(STRINGLITERAL, left),
                    () -> $.CONSUME(HEX_CONSTANT, left),
                    () -> $.CONSUME(DOLLAR_HEX_CONSTANT, left),
                    () -> $.CONSUME(RAWCODE, left),
                    () -> $.SUBRULE($.REF("jassValueOf"), left),
                    () -> $.SUBRULE($.REF("FunctionReferenceExpression"), left),
                    () -> $.CONSUME(INTEGER, left),
                    () -> $.CONSUME(NULL, left),
                    () -> $.CONSUME(FALSE, left),
                    () -> $.CONSUME(TRUE, left),
                    () -> $.CONSUME(REAL, left));
            $.OPTION(() -> {
                $.OR(
                        () -> $.CONSUME(AND, op),
                        () -> $.CONSUME(OR, op),
                        () -> $.CONSUME(EQUALSEQUALS, op),
                        () -> $.CONSUME(LESSOREQUALS, op),
                        () -> $.CONSUME(GREATEROREQUALS, op),
                        () -> $.CONSUME(LESS, op),
                        () -> $.CONSUME(GREATER, op),
                        () -> $.CONSUME(NOTEQUALS, op),
                        () -> $.CONSUME(PLUS, op),
                        () -> $.CONSUME(MINUS, op),
                        () -> $.CONSUME(MULT, op),
                        () -> $.CONSUME(DIV, op));
                $.SUBRULE($.REF("expression"), right);
            });
        });

        $.RULE("StringLiteralExpression", () -> {
            $.CONSUME(STRINGLITERAL);
        });
        $.RULE("HexIntegerLiteralExpression", () -> {
            $.CONSUME(HEX_CONSTANT);
        });
        $.RULE("DollarHexIntegerLiteralExpression", () -> {
            $.CONSUME(DOLLAR_HEX_CONSTANT);
        });

        $.RULE("FunctionReferenceExpression", () -> {
            $.CONSUME(FUNCTION);
            $.CONSUME(ID);
        });

        $.RULE("jassValueOf", () -> {
            $.CONSUME(ID);
            $.OPTION(() -> {
                $.OR(
                        () -> $.SUBRULE($.REF("arrayAccess")),

                        () -> {
                            $.CONSUME(LPAREN);
                            $.SUBRULE($.REF("argList"));
                            $.CONSUME(RPAREN);
                        },

                        () -> {
                            $.CONSUME(LPAREN);
                            $.CONSUME(RPAREN);
                        });
            });
        });

        $.RULE("functionCallExpression", () -> {
            $.CONSUME(ID);
            $.CONSUME(LPAREN);
            $.OPTION(() -> $.SUBRULE($.REF("argList")));
            $.CONSUME(RPAREN);
        });


        $.RULE("arrayAccess", () -> {
            $.CONSUME(LSQUAREPAREN);
            $.SUBRULE($.REF("expression"));
            $.CONSUME(RSQUAREPAREN);
        });

        $.RULE("ParentheticalExpression", () -> {
            $.CONSUME(LPAREN);
            $.SUBRULE($.REF("expression"));
            $.CONSUME(RPAREN);
        });

        $.RULE("NotExpression", () -> {
            $.CONSUME(NOT);
            $.SUBRULE($.REF("expression"));
        });

        $.RULE("NegateExpression", () -> {
            $.CONSUME(MINUS);
            $.SUBRULE($.REF("expression"));
        });


        $.RULE("argList", () -> {
            $.SUBRULE($.REF("expression"));
            $.MANY(() -> {
                $.CONSUME(COMMA);
                $.SUBRULE($.REF("expression"));
            });
        });
        String label = "statements";
        $.RULE("statement", () -> {
            $.OR(
                    () -> $.SUBRULE($.REF("callStatement"), label),
                    () -> $.SUBRULE($.REF("setStatement"), label),
                    () -> $.SUBRULE($.REF("returnStatement"), label),
                    () -> $.SUBRULE($.REF("exitWhenStatement"), label),
                    () -> $.SUBRULE($.REF("localVars"), label),
                    () -> $.SUBRULE($.REF("loopStatement"), label),
                    () -> $.SUBRULE($.REF("ifStatement"), label));
        });
        $.RULE("callStatement", () -> {
            $.CONSUME(CALL);
            $.SUBRULE($.REF("functionCallExpression"));
        });


        $.RULE("setStatement", () -> {
            $.CONSUME(SET);
            $.CONSUME(ID);
            $.OPTION(() -> $.SUBRULE($.REF("arrayAccess")));
            $.CONSUME(EQUALS);
            $.SUBRULE($.REF("expression"));
        });

        $.RULE("returnStatement", () -> {
            $.CONSUME(RETURN);
            $.OPTION(() -> $.SUBRULE($.REF("expression")));
        });

        $.RULE("exitWhenStatement", () -> {
            $.CONSUME(EXITWHEN);
            $.SUBRULE($.REF("expression"));
        });

        $.RULE("loopStatement", () -> {
            $.CONSUME(LOOP);
            $.MANY(() -> $.SUBRULE($.REF("statement")));
            $.CONSUME(ENDLOOP);
        });
        $.RULE("ifStatement", () -> {
            $.CONSUME(IF);
            $.SUBRULE($.REF("expression"), "condition");
            $.CONSUME(THEN);
            $.MANY(() -> $.SUBRULE($.REF("statement"), "statements"));
            $.MANY(() -> $.SUBRULE($.REF("elseIfStatement"), "elseIfStatement"));
            $.MANY(() -> $.SUBRULE($.REF("elseStatement"), "elseStatement"));
            $.CONSUME(ENDIF);
        });
        $.RULE("elseIfStatement", () -> {
            $.CONSUME(ELSEIF);
            $.SUBRULE($.REF("expression"), "condition");
            $.CONSUME(THEN);
            $.MANY(() -> $.SUBRULE($.REF("statement"), "statements"));
        });

        $.RULE("elseStatement", () -> {
            $.CONSUME(ELSE);
            $.MANY(() -> $.SUBRULE($.REF("statement"), "statements"));
        });

        $.RULE("param", () -> {
            $.OR(

                    () -> {
                        $.SUBRULE($.REF("type"));
                        $.CONSUME(ID);
                    },
                    () -> $.CONSUME(NOTHING));
        });
        $.RULE("paramList", () -> {
            $.SUBRULE($.REF("param"));
            $.MANY(() -> {
                $.CONSUME(COMMA);
                $.SUBRULE($.REF("param"));
            });
        });
        $.RULE("globalsBlock", () -> {
            $.CONSUME(GLOBALS);
            $.MANY(() -> $.SUBRULE($.REF("globalVar"), "globalVars"));
            $.CONSUME(ENDGLOBALS);
        });

        $.RULE("nativeBlock", () -> {
            $.OPTION(() -> $.CONSUME(CONSTANT));
            $.CONSUME(NATIVE);
            $.CONSUME(ID);
            $.CONSUME(TAKES);
            $.SUBRULE($.REF("paramList"));
            $.CONSUME(RETURNS);
            $.SUBRULE($.REF("type"), "returnType");
        });


        $.RULE("functionBlock", () -> {
            $.OPTION(() -> $.CONSUME(CONSTANT));
            $.CONSUME(FUNCTION);
            $.CONSUME(ID, "functionName");
            $.CONSUME(TAKES);
            $.SUBRULE($.REF("paramList"));
            $.CONSUME(RETURNS);
            $.SUBRULE($.REF("type"), "returnType");
            $.MANY(() -> $.SUBRULE($.REF("statement"), "statements"));
            $.CONSUME(ENDFUNCTION);
        });
    }

    static TokenDef createToken(String name, String r) {
        TokenDef tokenDef = new TokenDef(name, r);
        tokenDefs.add(tokenDef);
        return tokenDef;
    }

    public static TokenDef comment = createToken("comment", "//.*").skip();
    public static TokenDef whitespace = createToken("whitespace", "\s+").skip();
    public static TokenDef RAWCODE = createToken("RAWCODE", "('''.*?''')");
    public static TokenDef COMMA = createToken("COMMA", ",");
    public static TokenDef IF = createToken("IF", "if");
    public static TokenDef THEN = createToken("THEN", "then");
    public static TokenDef ELSEIF = createToken("ELSEIF", "elseif");
    public static TokenDef ELSE = createToken("ELSE", "else");
    public static TokenDef ENDIF = createToken("ENDIF", "endif");
    public static TokenDef GLOBALS = createToken("GLOBALS", "globals");
    public static TokenDef ENDGLOBALS = createToken("ENDGLOBALS", "endglobals");
    public static TokenDef FUNCTION = createToken("FUNCTION", "function");
    public static TokenDef TAKES = createToken("TAKES", "takes");
    public static TokenDef TYPE = createToken("TYPE", "type");
    public static TokenDef ARRAY = createToken("ARRAY", "array");
    public static TokenDef NULL = createToken("NULL", "null");
    public static TokenDef TRUE = createToken("TRUE", "true");
    public static TokenDef FALSE = createToken("FALSE", "false");
    public static TokenDef CONSTANT = createToken("CONSTANT", "constant");
    public static TokenDef HEX_CONSTANT = createToken("HEX_CONSTANT", "0x(([0-9]|[a-f]|[A-F])*)");
    public static TokenDef DOLLAR_HEX_CONSTANT = createToken("DOLLAR_HEX_CONSTANT", "\\$(([0-9]|[A-F])*)");
    public static TokenDef RETURNS = createToken("RETURNS", "returns");
    public static TokenDef RETURN = createToken("RETURN", "return");
    public static TokenDef EXTENDS = createToken("EXTENDS", "extends");
    public static TokenDef NATIVE = createToken("NATIVE", "native");
    public static TokenDef ENDFUNCTION = createToken("ENDFUNCTION", "endfunction");
    public static TokenDef AND = createToken("AND", "and(?=[\\s()])");
    public static TokenDef OR = createToken("OR", "or(?=[\\s()])");
    public static TokenDef LOCAL = createToken("LOCAL", "local");
    public static TokenDef SET = createToken("SET", "set(?=\\s)");
    public static TokenDef LOOP = createToken("LOOP", "loop(?=\\s)");
    public static TokenDef EXITWHEN = createToken("EXITWHEN", "exitwhen");
    public static TokenDef ENDLOOP = createToken("ENDLOOP", "endloop");
    public static TokenDef NOTHING = createToken("NOTHING", "nothing");
    public static TokenDef NOT = createToken("NOT", "not");
    public static TokenDef CALL = createToken("CALL", "call(?=\\s)");
    public static TokenDef MULT = createToken("MULT", "\\*");
    public static TokenDef DIV = createToken("DIV", "\\/");
    public static TokenDef PLUS = createToken("PLUS", "\\+");
    public static TokenDef MINUS = createToken("MINUS", "-");
    public static TokenDef ID = createToken("ID", "[a-zA-Z]\\w*");
    public static TokenDef LPAREN = createToken("LPAREN", "\\(");
    public static TokenDef RPAREN = createToken("RPAREN", "\\)");
    public static TokenDef LSQUAREPAREN = createToken("LSQUAREPAREN", "\\[");
    public static TokenDef RSQUAREPAREN = createToken("RSQUAREPAREN", "\\]");
    public static TokenDef NOTEQUALS = createToken("NOTEQUALS", "!=");
    public static TokenDef EQUALSEQUALS = createToken("EQUALSEQUALS", "==");
    public static TokenDef LESSOREQUALS = createToken("LESSOREQUALS", "<=");
    public static TokenDef GREATEROREQUALS = createToken("GREATEROREQUALS", ">=");
    public static TokenDef EQUALS = createToken("EQUALS", "=");
    public static TokenDef LESS = createToken("LESS", "<");
    public static TokenDef GREATER = createToken("GREATER", ">");
    public static TokenDef IDLITERAL = createToken("IDLITERAL", "'.*?'");
    public static TokenDef STRINGLITERAL = createToken("STRINGLITERAL", "\"[\\s\\S]*?\"").linebreak();
    public static TokenDef REAL = createToken("REAL", "[0-9]+\\.[0-9]+");
    public static TokenDef INTEGER = createToken("INTEGER", "[0-9]+");
    public static TokenDef NL = createToken("NL", "[\r\n]+");
}

package net.axther.serverCore.particle.script;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Recursive descent parser that converts expression strings into evaluable
 * {@link ParticleScript.Expression} trees.
 *
 * <p>Supported grammar (highest to lowest precedence):
 * <ol>
 *   <li>Atoms: numbers, variables, function calls, parenthesised sub-expressions</li>
 *   <li>Unary minus</li>
 *   <li>{@code ^} (power, right-associative)</li>
 *   <li>{@code * / %}</li>
 *   <li>{@code + -}</li>
 * </ol>
 *
 * <p>Built-in functions: sin, cos, tan, abs, min, max, sqrt, rand, floor, ceil, lerp
 */
public final class ScriptParser {

    private ScriptParser() {}

    // ---- Token types ----

    private enum TokenType {
        NUMBER, IDENTIFIER, PLUS, MINUS, STAR, SLASH, PERCENT, CARET,
        LPAREN, RPAREN, COMMA, EOF
    }

    private record Token(TokenType type, String value) {}

    // ---- Tokeniser ----

    private static List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int len = input.length();

        while (i < len) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }

            switch (c) {
                case '+' -> { tokens.add(new Token(TokenType.PLUS, "+")); i++; }
                case '-' -> { tokens.add(new Token(TokenType.MINUS, "-")); i++; }
                case '*' -> { tokens.add(new Token(TokenType.STAR, "*")); i++; }
                case '/' -> { tokens.add(new Token(TokenType.SLASH, "/")); i++; }
                case '%' -> { tokens.add(new Token(TokenType.PERCENT, "%")); i++; }
                case '^' -> { tokens.add(new Token(TokenType.CARET, "^")); i++; }
                case '(' -> { tokens.add(new Token(TokenType.LPAREN, "(")); i++; }
                case ')' -> { tokens.add(new Token(TokenType.RPAREN, ")")); i++; }
                case ',' -> { tokens.add(new Token(TokenType.COMMA, ",")); i++; }
                default -> {
                    if (Character.isDigit(c) || c == '.') {
                        int start = i;
                        while (i < len && (Character.isDigit(input.charAt(i)) || input.charAt(i) == '.')) i++;
                        tokens.add(new Token(TokenType.NUMBER, input.substring(start, i)));
                    } else if (Character.isLetter(c) || c == '_') {
                        int start = i;
                        while (i < len && (Character.isLetterOrDigit(input.charAt(i)) || input.charAt(i) == '_')) i++;
                        tokens.add(new Token(TokenType.IDENTIFIER, input.substring(start, i)));
                    } else {
                        // skip unknown character
                        i++;
                    }
                }
            }
        }

        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }

    // ---- Parser state ----

    private static final class Parser {
        private final List<Token> tokens;
        private int pos;

        Parser(List<Token> tokens) {
            this.tokens = tokens;
            this.pos = 0;
        }

        Token peek() {
            return tokens.get(pos);
        }

        Token consume() {
            return tokens.get(pos++);
        }

        boolean match(TokenType type) {
            if (peek().type == type) { consume(); return true; }
            return false;
        }

        // ---- Grammar rules ----

        /** entry: additive EOF */
        ParticleScript.Expression parseExpression() {
            ParticleScript.Expression expr = parseAdditive();
            // ignore any remaining tokens
            return expr;
        }

        /** additive: multiplicative (('+' | '-') multiplicative)* */
        ParticleScript.Expression parseAdditive() {
            ParticleScript.Expression left = parseMultiplicative();
            while (true) {
                if (peek().type == TokenType.PLUS) {
                    consume();
                    ParticleScript.Expression right = parseMultiplicative();
                    ParticleScript.Expression l = left;
                    left = ctx -> l.evaluate(ctx) + right.evaluate(ctx);
                } else if (peek().type == TokenType.MINUS) {
                    consume();
                    ParticleScript.Expression right = parseMultiplicative();
                    ParticleScript.Expression l = left;
                    left = ctx -> l.evaluate(ctx) - right.evaluate(ctx);
                } else {
                    break;
                }
            }
            return left;
        }

        /** multiplicative: power (('*' | '/' | '%') power)* */
        ParticleScript.Expression parseMultiplicative() {
            ParticleScript.Expression left = parsePower();
            while (true) {
                if (peek().type == TokenType.STAR) {
                    consume();
                    ParticleScript.Expression right = parsePower();
                    ParticleScript.Expression l = left;
                    left = ctx -> l.evaluate(ctx) * right.evaluate(ctx);
                } else if (peek().type == TokenType.SLASH) {
                    consume();
                    ParticleScript.Expression right = parsePower();
                    ParticleScript.Expression l = left;
                    left = ctx -> {
                        double d = right.evaluate(ctx);
                        return d == 0 ? 0.0 : l.evaluate(ctx) / d;
                    };
                } else if (peek().type == TokenType.PERCENT) {
                    consume();
                    ParticleScript.Expression right = parsePower();
                    ParticleScript.Expression l = left;
                    left = ctx -> {
                        double d = right.evaluate(ctx);
                        return d == 0 ? 0.0 : l.evaluate(ctx) % d;
                    };
                } else {
                    break;
                }
            }
            return left;
        }

        /** power: unary ('^' power)? -- right-associative */
        ParticleScript.Expression parsePower() {
            ParticleScript.Expression base = parseUnary();
            if (peek().type == TokenType.CARET) {
                consume();
                ParticleScript.Expression exponent = parsePower(); // right-assoc recursion
                return ctx -> Math.pow(base.evaluate(ctx), exponent.evaluate(ctx));
            }
            return base;
        }

        /** unary: '-' unary | atom */
        ParticleScript.Expression parseUnary() {
            if (peek().type == TokenType.MINUS) {
                consume();
                ParticleScript.Expression operand = parseUnary();
                return ctx -> -operand.evaluate(ctx);
            }
            return parseAtom();
        }

        /** atom: NUMBER | IDENTIFIER | function_call | '(' expression ')' */
        ParticleScript.Expression parseAtom() {
            Token tok = peek();

            // Number literal
            if (tok.type == TokenType.NUMBER) {
                consume();
                double val;
                try { val = Double.parseDouble(tok.value); } catch (NumberFormatException e) { val = 0.0; }
                double constant = val;
                return ctx -> constant;
            }

            // Identifier -- could be variable or function call
            if (tok.type == TokenType.IDENTIFIER) {
                consume();
                String name = tok.value;

                // Function call: name '(' args ')'
                if (peek().type == TokenType.LPAREN) {
                    consume(); // eat '('
                    return parseFunction(name);
                }

                // Variable reference
                return ctx -> ctx.get(name);
            }

            // Parenthesised sub-expression
            if (tok.type == TokenType.LPAREN) {
                consume();
                ParticleScript.Expression inner = parseAdditive();
                match(TokenType.RPAREN); // consume ')' if present
                return inner;
            }

            // Fallback: skip token, return 0
            consume();
            return ctx -> 0.0;
        }

        /** Parse function arguments after the opening '(' has been consumed. */
        private ParticleScript.Expression parseFunction(String name) {
            // rand() -- no args
            if (name.equals("rand")) {
                match(TokenType.RPAREN);
                return ctx -> ThreadLocalRandom.current().nextDouble();
            }

            // All other functions require at least one argument
            ParticleScript.Expression arg1 = parseAdditive();

            return switch (name) {
                case "sin" -> { match(TokenType.RPAREN); yield ctx -> Math.sin(arg1.evaluate(ctx)); }
                case "cos" -> { match(TokenType.RPAREN); yield ctx -> Math.cos(arg1.evaluate(ctx)); }
                case "tan" -> { match(TokenType.RPAREN); yield ctx -> Math.tan(arg1.evaluate(ctx)); }
                case "abs" -> { match(TokenType.RPAREN); yield ctx -> Math.abs(arg1.evaluate(ctx)); }
                case "sqrt" -> { match(TokenType.RPAREN); yield ctx -> Math.sqrt(arg1.evaluate(ctx)); }
                case "floor" -> { match(TokenType.RPAREN); yield ctx -> Math.floor(arg1.evaluate(ctx)); }
                case "ceil" -> { match(TokenType.RPAREN); yield ctx -> Math.ceil(arg1.evaluate(ctx)); }
                case "min" -> {
                    match(TokenType.COMMA);
                    ParticleScript.Expression arg2 = parseAdditive();
                    match(TokenType.RPAREN);
                    yield ctx -> Math.min(arg1.evaluate(ctx), arg2.evaluate(ctx));
                }
                case "max" -> {
                    match(TokenType.COMMA);
                    ParticleScript.Expression arg2 = parseAdditive();
                    match(TokenType.RPAREN);
                    yield ctx -> Math.max(arg1.evaluate(ctx), arg2.evaluate(ctx));
                }
                case "lerp" -> {
                    match(TokenType.COMMA);
                    ParticleScript.Expression arg2 = parseAdditive();
                    match(TokenType.COMMA);
                    ParticleScript.Expression arg3 = parseAdditive();
                    match(TokenType.RPAREN);
                    yield ctx -> {
                        double a = arg1.evaluate(ctx);
                        double b = arg2.evaluate(ctx);
                        double t = arg3.evaluate(ctx);
                        return a + (b - a) * t;
                    };
                }
                default -> {
                    // Unknown function -- consume remaining args and closing paren
                    while (peek().type != TokenType.RPAREN && peek().type != TokenType.EOF) consume();
                    match(TokenType.RPAREN);
                    yield ctx -> 0.0;
                }
            };
        }
    }

    // ---- Public API ----

    /**
     * Parse an expression string into an evaluable {@link ParticleScript.Expression}.
     * Returns an expression that evaluates to {@code 0.0} if the input is null, blank or invalid.
     */
    public static ParticleScript.Expression parse(String input) {
        if (input == null || input.isBlank()) return ctx -> 0.0;
        try {
            List<Token> tokens = tokenize(input.trim());
            return new Parser(tokens).parseExpression();
        } catch (Exception e) {
            return ctx -> 0.0;
        }
    }
}

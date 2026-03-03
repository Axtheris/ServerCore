package net.axther.serverCore.particle.script;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the particle scripting language parser and expression evaluator.
 * Covers correctness, edge cases, and operator precedence.
 */
class ScriptParserTest {

    private double eval(String expr) {
        return ScriptParser.parse(expr).evaluate(new ScriptContext());
    }

    private double eval(String expr, ScriptContext ctx) {
        return ScriptParser.parse(expr).evaluate(ctx);
    }

    // --- Literals ---

    @Nested
    class Literals {
        @Test void integerLiteral() { assertEquals(42.0, eval("42")); }
        @Test void decimalLiteral() { assertEquals(3.14, eval("3.14"), 0.001); }
        @Test void leadingDot() { assertEquals(0.5, eval(".5"), 0.001); }
        @Test void zero() { assertEquals(0.0, eval("0")); }
    }

    // --- Arithmetic operators ---

    @Nested
    class Arithmetic {
        @Test void addition() { assertEquals(5.0, eval("2 + 3")); }
        @Test void subtraction() { assertEquals(1.0, eval("4 - 3")); }
        @Test void multiplication() { assertEquals(12.0, eval("3 * 4")); }
        @Test void division() { assertEquals(2.5, eval("5 / 2")); }
        @Test void modulo() { assertEquals(1.0, eval("7 % 3")); }
        @Test void power() { assertEquals(8.0, eval("2 ^ 3")); }
        @Test void unaryMinus() { assertEquals(-5.0, eval("-5")); }
        @Test void doubleNegation() { assertEquals(5.0, eval("--5")); }
    }

    // --- Operator precedence ---

    @Nested
    class Precedence {
        @Test void mulBeforeAdd() { assertEquals(14.0, eval("2 + 3 * 4")); }
        @Test void parensOverride() { assertEquals(20.0, eval("(2 + 3) * 4")); }
        @Test void powerBeforeMul() { assertEquals(24.0, eval("3 * 2 ^ 3")); }

        @Test void powerRightAssociative() {
            // 2^3^2 should be 2^(3^2) = 2^9 = 512
            assertEquals(512.0, eval("2 ^ 3 ^ 2"));
        }

        @Test void complexPrecedence() {
            // 1 + 2 * 3 ^ 2 = 1 + 2*9 = 1 + 18 = 19
            assertEquals(19.0, eval("1 + 2 * 3 ^ 2"));
        }
    }

    // --- Division/modulo safety ---

    @Nested
    class SafeDivision {
        @Test void divisionByZero() { assertEquals(0.0, eval("5 / 0")); }
        @Test void moduloByZero() { assertEquals(0.0, eval("5 % 0")); }
    }

    // --- Variables ---

    @Nested
    class Variables {
        @Test void builtInPi() { assertEquals(Math.PI, eval("pi"), 0.0001); }
        @Test void builtInE() { assertEquals(Math.E, eval("e"), 0.0001); }
        @Test void unknownVariableReturnsZero() { assertEquals(0.0, eval("foobar")); }

        @Test void customVariable() {
            ScriptContext ctx = new ScriptContext();
            ctx.set("x", 7.0);
            assertEquals(7.0, eval("x", ctx));
        }

        @Test void tickVariable() {
            ScriptContext ctx = new ScriptContext();
            ctx.set("t", 100.0);
            assertEquals(100.0, eval("t", ctx));
        }

        @Test void variableInExpression() {
            ScriptContext ctx = new ScriptContext();
            ctx.set("t", 10.0);
            ctx.set("n", 20.0);
            assertEquals(200.0, eval("t * n", ctx));
        }
    }

    // --- Built-in functions ---

    @Nested
    class Functions {
        @Test void sin() { assertEquals(0.0, eval("sin(0)"), 0.0001); }
        @Test void sinPiHalf() { assertEquals(1.0, eval("sin(pi / 2)"), 0.0001); }
        @Test void cos() { assertEquals(1.0, eval("cos(0)"), 0.0001); }
        @Test void tan() { assertEquals(0.0, eval("tan(0)"), 0.0001); }
        @Test void abs() { assertEquals(5.0, eval("abs(-5)")); }
        @Test void sqrt() { assertEquals(3.0, eval("sqrt(9)"), 0.0001); }
        @Test void floor() { assertEquals(3.0, eval("floor(3.7)")); }
        @Test void ceil() { assertEquals(4.0, eval("ceil(3.1)")); }
        @Test void min() { assertEquals(2.0, eval("min(5, 2)")); }
        @Test void max() { assertEquals(5.0, eval("max(5, 2)")); }

        @Test void lerp() {
            // lerp(0, 10, 0.5) = 0 + (10 - 0) * 0.5 = 5
            assertEquals(5.0, eval("lerp(0, 10, 0.5)"), 0.0001);
        }

        @Test void lerpEdges() {
            assertEquals(0.0, eval("lerp(0, 10, 0)"), 0.0001);
            assertEquals(10.0, eval("lerp(0, 10, 1)"), 0.0001);
        }

        @Test void rand() {
            double val = eval("rand()");
            assertTrue(val >= 0.0 && val < 1.0, "rand() should be in [0, 1): " + val);
        }

        @Test void unknownFunctionReturnsZero() { assertEquals(0.0, eval("unknown(5)")); }
    }

    // --- Null/blank/invalid inputs ---

    @Nested
    class InvalidInputs {
        @Test void nullInput() { assertEquals(0.0, ScriptParser.parse(null).evaluate(new ScriptContext())); }
        @Test void emptyString() { assertEquals(0.0, eval("")); }
        @Test void blankString() { assertEquals(0.0, eval("   ")); }
    }

    // --- Complex real-world expressions ---

    @Nested
    class RealWorldExpressions {

        @Test void spiralXExpression() {
            ScriptContext ctx = new ScriptContext();
            ctx.set("t", 10.0);
            ctx.set("i", 3.0);
            ctx.set("n", 20.0);
            // cos(t * 0.1 + i * 2 * pi / n) * 2
            double expected = Math.cos(10 * 0.1 + 3 * 2 * Math.PI / 20) * 2;
            assertEquals(expected, eval("cos(t * 0.1 + i * 2 * pi / n) * 2", ctx), 0.0001);
        }

        @Test void verticalWrapExpression() {
            ScriptContext ctx = new ScriptContext();
            ctx.set("t", 100.0);
            // t * 0.05 % 3
            double expected = (100 * 0.05) % 3;
            assertEquals(expected, eval("t * 0.05 % 3", ctx), 0.0001);
        }

        @Test void colorPulseExpression() {
            ScriptContext ctx = new ScriptContext();
            ctx.set("t", 50.0);
            // 128 + 127 * sin(t * 0.05)
            double expected = 128 + 127 * Math.sin(50 * 0.05);
            assertEquals(expected, eval("128 + 127 * sin(t * 0.05)", ctx), 0.0001);
        }
    }
}

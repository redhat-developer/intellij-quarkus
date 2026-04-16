package com.redhat.devtools.intellij.qute.lang.psi;

import com.intellij.lexer.Lexer;
import com.intellij.psi.tree.IElementType;
import org.junit.Test;

import static org.junit.Assert.*;

public class QuteLexerWithYamlTest {

    @Test
    public void testYamlFrontMatterWithRoqEnabled() {
        String text = "---\ntitle: My Page\n---\n<h1>Hello World</h1>\n";
        Lexer lexer = new QuteLexer(true); // roq support enabled
        lexer.start(text);

        // Token 0: ---\n (LANGUAGE_INJECTION_START)
        assertNotNull(lexer.getTokenType());
        assertEquals(QuteTokenType.QUTE_LANGUAGE_INJECTION_START, lexer.getTokenType());
        assertEquals(0, lexer.getTokenStart());
        assertEquals(4, lexer.getTokenEnd());
        lexer.advance();

        // Token 1: title (YAML_KEY)
        assertNotNull(lexer.getTokenType());
        assertEquals(QuteTokenType.QUTE_YAML_KEY, lexer.getTokenType());
        lexer.advance();

        // Token 2: : (YAML_COLON)
        assertNotNull(lexer.getTokenType());
        assertEquals(QuteTokenType.QUTE_YAML_COLON, lexer.getTokenType());
        lexer.advance();

        // Skip whitespace and string, go to end marker
        while (lexer.getTokenType() != null && lexer.getTokenType() != QuteTokenType.QUTE_LANGUAGE_INJECTION_END) {
            lexer.advance();
        }

        // Should find LANGUAGE_INJECTION_END
        assertNotNull(lexer.getTokenType());
        assertEquals(QuteTokenType.QUTE_LANGUAGE_INJECTION_END, lexer.getTokenType());
    }

    @Test
    public void testYamlFrontMatterWithRoqDisabled() {
        String text = "---\ntitle: My Page\n---\n<h1>Hello World</h1>\n";
        Lexer lexer = new QuteLexer(false); // roq support disabled
        lexer.start(text);

        // Should treat everything as text when roq is disabled
        assertNotNull(lexer.getTokenType());
        // Should NOT be LANGUAGE_INJECTION_START
        assertNotEquals(QuteTokenType.QUTE_LANGUAGE_INJECTION_START, lexer.getTokenType());
        // Should be text
        assertEquals("QUTE_TEXT", lexer.getTokenType().toString());
    }

    @Test
    public void testRegularQuteWithRoqEnabled() {
        String text = "<h1>{name}</h1>";
        Lexer lexer = new QuteLexer(true); // roq support enabled
        lexer.start(text);

        // Token 0: <h1> (TEXT)
        assertNotNull(lexer.getTokenType());
        assertEquals("QUTE_TEXT", lexer.getTokenType().toString());
        lexer.advance();

        // Token 1: { (START_EXPRESSION)
        assertNotNull(lexer.getTokenType());
        assertEquals(QuteTokenType.QUTE_START_EXPRESSION, lexer.getTokenType());
    }

    @Test
    public void testRegularQuteWithRoqDisabled() {
        String text = "<h1>{name}</h1>";
        Lexer lexer = new QuteLexer(false); // roq support disabled
        lexer.start(text);

        // Token 0: <h1> (TEXT)
        assertNotNull(lexer.getTokenType());
        assertEquals("QUTE_TEXT", lexer.getTokenType().toString());
        lexer.advance();

        // Token 1: { (START_EXPRESSION)
        assertNotNull(lexer.getTokenType());
        assertEquals(QuteTokenType.QUTE_START_EXPRESSION, lexer.getTokenType());
    }

    @Test
    public void testTripleDashNotAtStartWithRoqEnabled() {
        String text = "<p>Some text</p>\n---\ntitle: Test\n---\n";
        Lexer lexer = new QuteLexer(true);
        lexer.start(text);

        // First token should be TEXT, not LANGUAGE_INJECTION_START
        // because --- is not at the beginning
        assertNotNull(lexer.getTokenType());
        assertEquals("QUTE_TEXT", lexer.getTokenType().toString());
    }

    @Test
    public void testEmptyYamlFrontMatterWithRoqEnabled() {
        // Note: The YamlFrontMatterDetector doesn't recognize empty YAML (---\n---\n) as valid.
        // The second --- is treated as YAML content (ScalarString) rather than the closing marker.
        // This test verifies the current behavior.
        String text = "---\n---\n<h1>Hello</h1>";
        Lexer lexer = new QuteLexer(true);
        lexer.start(text);

        // Token 0: ---\n (LANGUAGE_INJECTION_START)
        assertNotNull(lexer.getTokenType());
        assertEquals(QuteTokenType.QUTE_LANGUAGE_INJECTION_START, lexer.getTokenType());
        lexer.advance();

        // Token 1: --- (YAML_STRING) - treated as YAML content, not as closing marker
        assertNotNull(lexer.getTokenType());
        assertEquals(QuteTokenType.QUTE_YAML_STRING, lexer.getTokenType());
        lexer.advance();

        // Token 2: \n (YAML_WHITESPACE)
        assertNotNull(lexer.getTokenType());
        assertEquals(QuteTokenType.QUTE_YAML_WHITESPACE, lexer.getTokenType());
    }

    @Test
    public void testUnclosedYamlFrontMatterWithRoqEnabled() {
        // Test the case where user is typing and hasn't closed the YAML front matter yet.
        // This should not throw "Unexpected termination offset" error.
        String text = "---\ntitle: My Page";
        Lexer lexer = new QuteLexer(true);
        lexer.start(text);

        // Token 0: ---\n (LANGUAGE_INJECTION_START)
        assertNotNull(lexer.getTokenType());
        assertEquals(QuteTokenType.QUTE_LANGUAGE_INJECTION_START, lexer.getTokenType());
        int start = lexer.getTokenStart();
        int end = lexer.getTokenEnd();
        assertEquals(0, start);
        assertEquals(4, end);
        lexer.advance();

        // Token 1: title (YAML_KEY)
        assertNotNull(lexer.getTokenType());
        assertEquals(QuteTokenType.QUTE_YAML_KEY, lexer.getTokenType());
        lexer.advance();

        // Continue to consume all tokens - should not crash
        int tokenCount = 2;
        while (lexer.getTokenType() != null) {
            tokenCount++;
            // Verify token positions are sane
            start = lexer.getTokenStart();
            end = lexer.getTokenEnd();
            assertTrue("Token start should be >= 0", start >= 0);
            assertTrue("Token end should be > start", end > start);
            assertTrue("Token end should be <= text length", end <= text.length());
            lexer.advance();
        }

        // Should have consumed several tokens
        assertTrue("Should have consumed multiple tokens", tokenCount > 2);

        // Verify lexer reached the end of the buffer
        assertEquals("Lexer should reach end of buffer", text.length(), lexer.getTokenEnd());
    }
}

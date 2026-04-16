package com.redhat.devtools.intellij.qute.lang.psi;

import com.intellij.lang.ParserDefinition;
import com.intellij.mock.MockSmartPointerManager;
import com.intellij.openapi.application.ex.PathManagerEx;
import com.intellij.psi.SmartPointerManager;
import com.intellij.testFramework.ParsingTestCase;
import com.redhat.devtools.intellij.qute.lang.QuteParserDefinition;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class QuteParsingTestCase extends ParsingTestCase  {

    public QuteParsingTestCase() {
        super("psi", "qute", new QuteParserDefinitionForTest());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        project.registerService(SmartPointerManager.class, new MockSmartPointerManager());
    }

    @Override
    protected String getTestDataPath() {
        return new File("testData/qute").getPath();
    }

    public void testHelloWorld() throws IOException { doCodeTest("<h1>Hello {http:param('name', 'Quarkus')}!</h1> "); }

    public void testSimpleText() throws IOException { doCodeTest("foo"); }

    public void testSectionWithHtml() throws IOException {
        doCodeTest("""
                <div>
                <div>
                                    {#if}
                    {#if}
                                {inject:flash.get("key")}
                <form>
                        </form>
                        {/if}
                                    {/if}
                                        </div>
                    </div>
                """);
    }

    public void testYamlFrontMatterSimple() throws IOException {
        doCodeTest("""
                ---
                title: My Page
                ---
                <h1>Hello World</h1>
                """);
    }

    public void testYamlFrontMatterWithMultipleKeys() throws IOException {
        doCodeTest("""
                ---
                title: My Page
                author: John Doe
                date: 2026-04-16
                ---
                <h1>Hello World</h1>
                """);
    }

    public void testYamlFrontMatterWithStringValues() throws IOException {
        doCodeTest("""
                ---
                title: "My Page"
                description: 'This is a description'
                ---
                <p>Content</p>
                """);
    }

    public void testYamlFrontMatterEmpty() throws IOException {
        doCodeTest("""
                ---
                ---
                <h1>Hello World</h1>
                """);
    }

    public void testYamlFrontMatterWithQuteExpressions() throws IOException {
        doCodeTest("""
                ---
                title: My Page
                ---
                <h1>{title}</h1>
                <p>{description}</p>
                """);
    }

    public void testYamlFrontMatterNotClosed() throws IOException {
        doCodeTest("""
                ---
                title: My Page
                author: John Doe
                <h1>Hello World</h1>
                """);
    }
}

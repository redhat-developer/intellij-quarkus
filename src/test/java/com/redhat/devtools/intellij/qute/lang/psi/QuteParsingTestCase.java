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
        super("psi", "qute", new QuteParserDefinition());
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
}

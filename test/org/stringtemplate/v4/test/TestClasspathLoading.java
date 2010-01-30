/*
 [The "BSD licence"]
 Copyright (c) 2009 Terence Parr
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.stringtemplate.v4.test;

import org.junit.Before;
import org.junit.Test;
import org.stringtemplate.ST;
import org.stringtemplate.STGroup;
import org.stringtemplate.STGroupDir;
import org.stringtemplate.v4.misc.Misc;

import static org.junit.Assert.assertEquals;

// THIS ONLY WORKS WHEN /tmp (or tmpdir) IS IN YOUR CLASSPATH

public class TestClasspathLoading extends BaseTest {
    //public static final String tmpdir = System.getProperty("java.io.tmpdir");
    public static final String tmpdir = "/tmp";
    public static final String newline = Misc.newline;
    public static final String dir = "yuck";

    @Before public void setup() {
        writeFile(tmpdir+"/"+dir, "a.st", "a(x) ::= <<a>>\n");
        writeFile(tmpdir+"/"+dir, "b.st", "b(x) ::= <<b>>\n");
        writeFile(tmpdir+"/"+dir, "c.st", "c(x) ::= <<c>>\n");
    }

    public static void main(String[] args) {
        //setup();
    }

    @Test
    public void test() {
        STGroup group = new STGroupDir(dir);
        ST st = group.getInstanceOf("a");
        String expected = "a";
        String result = st.render();
        assertEquals(expected, result);
    }
}
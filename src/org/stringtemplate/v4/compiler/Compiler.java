/*
 [The "BSD license"]
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
package org.stringtemplate.v4.compiler;

import org.antlr.runtime.*;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.misc.ErrorManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A compiler for a single template. */
public class Compiler {
	public static final String SUBTEMPLATE_PREFIX = "_sub";

    public static final int TEMPLATE_INITIAL_CODE_SIZE = 15;

    public static final Map<String, Interpreter.Option> supportedOptions =
        new HashMap<String, Interpreter.Option>() {
            {
                put("anchor",       Interpreter.Option.ANCHOR);
                put("format",       Interpreter.Option.FORMAT);
                put("null",         Interpreter.Option.NULL);
                put("separator",    Interpreter.Option.SEPARATOR);
                put("wrap",         Interpreter.Option.WRAP);
            }
        };

    public static final int NUM_OPTIONS = supportedOptions.size();

    public static final Map<String,String> defaultOptionValues =
        new HashMap<String,String>() {
            {
                put("anchor", "true");
                put("wrap",   "\n");
            }
        };

    public static Map<String, Short> funcs = new HashMap<String, Short>() {
        {
            put("first", Bytecode.INSTR_FIRST);
            put("last", Bytecode.INSTR_LAST);
            put("rest", Bytecode.INSTR_REST);
            put("trunc", Bytecode.INSTR_TRUNC);
            put("strip", Bytecode.INSTR_STRIP);
            put("trim", Bytecode.INSTR_TRIM);
            put("length", Bytecode.INSTR_LENGTH);
            put("strlen", Bytecode.INSTR_STRLEN);
            put("reverse", Bytecode.INSTR_REVERSE);
        }
    };

	/** Name subtemplates _sub1, _sub2, ... */
	public static int subtemplateCount = 0;

	/** The compiler needs to know how to delimit expressions.
	 *  The STGroup normally passes in this information, but we
	 *  can set some defaults.
	 */
	public char delimiterStartChar = '<'; // Use <expr> by default
	public char delimiterStopChar = '>';

	public ErrorManager errMgr;

	public Compiler() { this(STGroup.DEFAULT_ERR_MGR); }

	public Compiler(ErrorManager errMgr) { this(errMgr, '<', '>'); }

	public Compiler(char delimiterStartChar,
					char delimiterStopChar)
	{
		this(STGroup.DEFAULT_ERR_MGR, delimiterStartChar, delimiterStopChar);
	}

	/** To compile a template, we need to know what the
	 *  enclosing template is (if any) in case of regions.
	 */
	public Compiler(ErrorManager errMgr,
					char delimiterStartChar,
					char delimiterStopChar)
	{
		this.errMgr = errMgr;
		this.delimiterStartChar = delimiterStartChar;
		this.delimiterStopChar = delimiterStopChar;
	}

	public CompiledST compile(String template) {
		CompiledST code = compile(null, null, template, null);
		code.hasFormalArgs = false;
		return code;
	}

	/** Compile full template with unknown formal args. */
	public CompiledST compile(String name, String template) {
		CompiledST code = compile(name, null, template, null);
		code.hasFormalArgs = false;
		return code;
	}

	/** Compile full template with respect to a list of formal args. */
	public CompiledST compile(String name,
							  List<FormalArgument> args,
							  String template,
							  Token templateToken)
	{
		ANTLRStringStream is = new ANTLRStringStream(template);
		is.name = name;
		errMgr.context.push(templateToken);
		STLexer lexer = new STLexer(errMgr, is, delimiterStartChar, delimiterStopChar);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		STTreeBuilder p = new STTreeBuilder(tokens, errMgr);
		STTreeBuilder.templateAndEOF_return r = null;
		try {
			try {
				r = p.templateAndEOF();
			}
			catch (RecognitionException re) {
				throwSTException(tokens, p, re);
			}
			if ( p.getNumberOfSyntaxErrors()>0 || r.getTree()==null ) {
				CompiledST impl = new CompiledST();
				impl.defineFormalArgs(args);
				return impl;
			}

			System.out.println(((CommonTree)r.getTree()).toStringTree());
			CommonTreeNodeStream nodes = new CommonTreeNodeStream(r.getTree());
			nodes.setTokenStream(tokens);
			CodeGenerator gen = new CodeGenerator(nodes, errMgr, name, template);

			CompiledST impl=null;
			try {
				impl = gen.template(name,args);
			}
			catch (RecognitionException re) {
				errMgr.internalError(null, "bad tree structure", re);
			}

			return impl;
		}
		finally {
			errMgr.context.pop();
		}
	}

	public static CompiledST defineBlankRegion(CompiledST outermostImpl, String name) {
		String outermostTemplateName = outermostImpl.name;
		String mangled = STGroup.getMangledRegionName(outermostTemplateName, name);
		CompiledST blank = new CompiledST();
		blank.isRegion = true;
		blank.regionDefType = ST.RegionType.IMPLICIT;
		blank.name = mangled;
		outermostImpl.addImplicitlyDefinedTemplate(blank);
		return blank;
	}

	public static String getNewSubtemplateName() {
		subtemplateCount++;
		return SUBTEMPLATE_PREFIX+subtemplateCount;
	}

	protected void throwSTException(TokenStream tokens, Parser parser, RecognitionException re) {
		String msg = parser.getErrorMessage(re, parser.getTokenNames());
		if ( re.token.getType() == STLexer.EOF_TYPE ) {
			throw new STException("premature EOF", re);
		}
		else if ( re instanceof NoViableAltException) {
			throw new STException(
				"'"+re.token.getText()+"' came as a complete surprise to me",re);
		}
		else if ( tokens.index() == 0 ) { // couldn't parse anything
			throw new STException(
				"this doesn't look like a template: \""+tokens+"\"", re);
		}
		else if ( tokens.LA(1) == STLexer.LDELIM ) { // couldn't parse expr
			throw new STException("doesn't look like an expression", re);
		}
		else {
			throw new STException(msg, re);
		}
	}

}

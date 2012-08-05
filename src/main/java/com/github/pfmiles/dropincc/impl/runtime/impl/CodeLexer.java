/*******************************************************************************
 * Copyright (c) 2012 pf_miles.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     pf_miles - initial API and implementation
 ******************************************************************************/
package com.github.pfmiles.dropincc.impl.runtime.impl;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.pfmiles.dropincc.DropinccException;
import com.github.pfmiles.dropincc.impl.TokenType;
import com.github.pfmiles.dropincc.impl.runtime.Token;

/**
 * Lexer class for code scanning. Each scanning process should create a new
 * CodeLexer Object to use.
 * 
 * @author pf-miles
 * 
 */
public class CodeLexer extends Lexer {

    // all regex group index -> tokenType mapping
    private Map<Integer, TokenType> groupNumToType;
    // scanning code
    private String code = null;
    private Matcher matcher = null;
    // current position in the input sequence
    private int currentRealPos = 0;
    private boolean eofReturned = false;
    // does the user care about white spaces?
    private boolean whiteSpaceSensitive;

    public CodeLexer(Pattern pattern, Map<Integer, TokenType> groupNumToType, String code, boolean whiteSpaceSensitive) {
        this.groupNumToType = groupNumToType;
        this.code = code;
        this.matcher = pattern.matcher(code);
        this.whiteSpaceSensitive = whiteSpaceSensitive;
    }

    public boolean hasMoreElements() {
        if (!this.lookAheadBuf.isEmpty() || currentRealPos < code.length())
            return true;
        if (!this.eofReturned)
            return true;
        return false;
    }

    /**
     * Real next implementation with whitespace sensitive option
     */
    protected Token realNext() {
        Token t = this._realNext();
        if (!this.whiteSpaceSensitive)
            while (t != null && t.getType().equals(TokenType.WHITESPACE))
                t = this._realNext();
        return t;
    }

    private Token _realNext() {
        if (currentRealPos < code.length()) {
            if (this.matcher.find(currentRealPos)) {
                // XXX find a more efficient named-capturing group
                // implementation here(planned to bootstrap the regex engine)
                for (Map.Entry<Integer, TokenType> e : this.groupNumToType.entrySet()) {
                    int gnum = e.getKey();
                    if (gnum != -1) {
                        String txt = this.matcher.group(gnum);
                        if (txt != null) {
                            this.currentRealPos += this.matcher.end() - this.matcher.start();
                            return new Token(e.getValue(), txt);
                        }
                    }
                }
                throw new DropinccException("No token matched at position: " + this.currentRealPos + ", subsequent char: '" + this.code.charAt(currentRealPos) + "'");
            } else {
                throw new DropinccException("Unexpected char: '" + this.code.charAt(currentRealPos) + "' at position: " + this.currentRealPos);
            }
        } else if (!this.eofReturned) {
            this.eofReturned = true;
            return Token.EOF;
        } else {
            return null;
        }
    }

    public int getCurrentPosition() {
        return this.currentRealPos - this.lookAheadBuf.size();
    }

}

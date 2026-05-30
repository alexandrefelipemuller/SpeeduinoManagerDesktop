package com.speeduino.manager.io

class IoExpressionEvaluator(private val variables: Map<String, Double>) {
    private var tokens: List<Token> = emptyList()
    private var position: Int = 0

    fun evaluate(expression: String): Double {
        tokens = Tokenizer(expression).tokenize()
        position = 0
        val value = parseTernary()
        expect(TokenType.EOF)
        return value
    }

    private fun parseTernary(): Double {
        var condition = parseOr()
        if (match(TokenType.QUESTION)) {
            val whenTrue = parseTernary()
            expect(TokenType.COLON)
            val whenFalse = parseTernary()
            condition = if (condition != 0.0) whenTrue else whenFalse
        }
        return condition
    }

    private fun parseOr(): Double {
        var left = parseAnd()
        while (match(TokenType.OR)) {
            val right = parseAnd()
            left = if (left.toLong() or right.toLong() != 0L) 1.0 else 0.0
        }
        return left
    }

    private fun parseAnd(): Double {
        var left = parseEquality()
        while (match(TokenType.AND)) {
            val right = parseEquality()
            left = (left.toLong() and right.toLong()).toDouble()
        }
        return left
    }

    private fun parseEquality(): Double {
        var left = parseRelational()
        while (true) {
            left = when {
                match(TokenType.EQ) -> {
                    val right = parseRelational()
                    if (left == right) 1.0 else 0.0
                }
                match(TokenType.NEQ) -> {
                    val right = parseRelational()
                    if (left != right) 1.0 else 0.0
                }
                else -> return left
            }
        }
    }

    private fun parseRelational(): Double {
        var left = parseAdd()
        while (true) {
            left = when {
                match(TokenType.LT) -> {
                    val right = parseAdd()
                    if (left < right) 1.0 else 0.0
                }
                match(TokenType.LTE) -> {
                    val right = parseAdd()
                    if (left <= right) 1.0 else 0.0
                }
                match(TokenType.GT) -> {
                    val right = parseAdd()
                    if (left > right) 1.0 else 0.0
                }
                match(TokenType.GTE) -> {
                    val right = parseAdd()
                    if (left >= right) 1.0 else 0.0
                }
                else -> return left
            }
        }
    }

    private fun parseAdd(): Double {
        var left = parseMul()
        while (true) {
            left = when {
                match(TokenType.PLUS) -> left + parseMul()
                match(TokenType.MINUS) -> left - parseMul()
                else -> return left
            }
        }
    }

    private fun parseMul(): Double {
        var left = parseUnary()
        while (true) {
            left = when {
                match(TokenType.MUL) -> left * parseUnary()
                match(TokenType.DIV) -> left / parseUnary()
                match(TokenType.MOD) -> left % parseUnary()
                else -> return left
            }
        }
    }

    private fun parseUnary(): Double {
        return when {
            match(TokenType.MINUS) -> -parseUnary()
            match(TokenType.PLUS) -> parseUnary()
            else -> parsePrimary()
        }
    }

    private fun parsePrimary(): Double {
        val token = advance()
        return when (token.type) {
            TokenType.NUMBER -> token.value
            TokenType.IDENTIFIER -> variables[token.text] ?: 0.0
            TokenType.LPAREN -> {
                val value = parseTernary()
                expect(TokenType.RPAREN)
                value
            }
            else -> 0.0
        }
    }

    private fun match(type: TokenType): Boolean {
        if (peek().type == type) {
            position++
            return true
        }
        return false
    }

    private fun expect(type: TokenType) {
        if (peek().type != type) {
            return
        }
        position++
    }

    private fun advance(): Token {
        val token = peek()
        position = (position + 1).coerceAtMost(tokens.lastIndex)
        return token
    }

    private fun peek(): Token = tokens.getOrElse(position) { Token(TokenType.EOF, "", 0.0) }

    private data class Token(
        val type: TokenType,
        val text: String,
        val value: Double
    )

    private enum class TokenType {
        NUMBER,
        IDENTIFIER,
        PLUS,
        MINUS,
        MUL,
        DIV,
        MOD,
        AND,
        OR,
        EQ,
        NEQ,
        LT,
        LTE,
        GT,
        GTE,
        QUESTION,
        COLON,
        LPAREN,
        RPAREN,
        EOF
    }

    private class Tokenizer(private val input: String) {
        private var index = 0

        fun tokenize(): List<Token> {
            val tokens = mutableListOf<Token>()
            while (index < input.length) {
                val char = input[index]
                when {
                    char.isWhitespace() -> index++
                    char.isDigit() || char == '.' -> tokens.add(readNumber())
                    char.isLetter() || char == '_' -> tokens.add(readIdentifier())
                    else -> {
                        tokens.add(readOperator())
                    }
                }
            }
            tokens.add(Token(TokenType.EOF, "", 0.0))
            return tokens
        }

        private fun readNumber(): Token {
            val start = index
            while (index < input.length && (input[index].isDigit() || input[index] == '.')) {
                index++
            }
            val text = input.substring(start, index)
            val value = text.toDoubleOrNull() ?: 0.0
            return Token(TokenType.NUMBER, text, value)
        }

        private fun readIdentifier(): Token {
            val start = index
            while (index < input.length && (input[index].isLetterOrDigit() || input[index] == '_' )) {
                index++
            }
            val text = input.substring(start, index)
            return Token(TokenType.IDENTIFIER, text, 0.0)
        }

        private fun readOperator(): Token {
            val char = input[index]
            return when (char) {
                '+' -> token(TokenType.PLUS)
                '-' -> token(TokenType.MINUS)
                '*' -> token(TokenType.MUL)
                '/' -> token(TokenType.DIV)
                '%' -> token(TokenType.MOD)
                '&' -> token(TokenType.AND)
                '|' -> token(TokenType.OR)
                '(' -> token(TokenType.LPAREN)
                ')' -> token(TokenType.RPAREN)
                '?' -> token(TokenType.QUESTION)
                ':' -> token(TokenType.COLON)
                '=' -> {
                    if (peek('=', 1)) {
                        index += 2
                        Token(TokenType.EQ, "==", 0.0)
                    } else {
                        token(TokenType.EQ)
                    }
                }
                '!' -> {
                    if (peek('=', 1)) {
                        index += 2
                        Token(TokenType.NEQ, "!=", 0.0)
                    } else {
                        index++
                        Token(TokenType.NEQ, "!=", 0.0)
                    }
                }
                '<' -> {
                    if (peek('=', 1)) {
                        index += 2
                        Token(TokenType.LTE, "<=", 0.0)
                    } else {
                        index++
                        Token(TokenType.LT, "<", 0.0)
                    }
                }
                '>' -> {
                    if (peek('=', 1)) {
                        index += 2
                        Token(TokenType.GTE, ">=", 0.0)
                    } else {
                        index++
                        Token(TokenType.GT, ">", 0.0)
                    }
                }
                else -> {
                    index++
                    Token(TokenType.EOF, "", 0.0)
                }
            }
        }

        private fun token(type: TokenType): Token {
            val text = input[index].toString()
            index++
            return Token(type, text, 0.0)
        }

        private fun peek(expected: Char, offset: Int): Boolean {
            val target = index + offset
            return target < input.length && input[target] == expected
        }
    }
}

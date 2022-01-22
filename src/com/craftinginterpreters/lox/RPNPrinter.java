/*
package com.craftinginterpreters.lox;

public class RPNPrinter implements Expr.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return RPN(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return RPN("grouping", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return RPN(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitListExpr(Expr.List expr) {
        return RPN("exprlist", expr.exprs.toArray(Expr[]::new));
    }

    @Override
    public String visitTernaryExpr(Expr.Ternary expr) {
        return RPN("ternary", expr._if, expr._then, expr._else);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return RPN(expr.name.lexeme);
    }

    public String RPN(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        for (Expr expr : exprs) {
            builder.append(expr.accept(this));
            builder.append(" ");
        }
        builder.append(name).append(" ");

        return builder.toString();
    }

    public static void main(String[] args) {
        Expr expression = new Expr.Binary(
                new Expr.Unary(
                        new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(123)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(
                        new Expr.Literal(45.67)));

        System.out.println(new RPNPrinter().print(expression));
    }
}
*/

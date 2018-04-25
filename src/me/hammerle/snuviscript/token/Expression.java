package me.hammerle.snuviscript.token;

import java.util.LinkedList;

public abstract class Expression 
{
    public static class Grouping extends Expression
    {
        private final Expression exp;
        
        public Grouping(Expression exp)
        {
            this.exp = exp;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append('(');
            sb.append(exp);
            sb.append(')');
            return sb.toString();
        }
    }
    
    public static abstract class PostFunction extends Expression
    {
        private final String name;
        private final LinkedList<Expression> exp = new LinkedList<>();
        private final char start;
        private final char end;
        
        public PostFunction(String name, char start, char end)
        {
            this.name = name;
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(name);
            sb.append(start);
            exp.forEach(e -> 
            {
                sb.append(e);
                sb.append(", ");
            });
            if(!exp.isEmpty())
            {
                sb.delete(sb.length() - 2, sb.length());
            }
            sb.append(end);
            return sb.toString();
        }
        
        public void addExpression(Expression exp)
        {
            this.exp.add(exp);
        }
    }
    
    public static class Function extends PostFunction
    {
        public Function(String name)
        {
            super(name, '(', ')');
        }
    }
    
    public static class Array extends PostFunction
    {
        public Array(String name)
        {
            super(name, '[', ']');
        }
    }
    
    public static class Binary extends Expression
    {
        private final Expression left;
        private final Token t;
        private final Expression right;
        
        public Binary(Expression left, Token t, Expression right)
        {
            this.left = left;
            this.t = t;
            this.right = right;
        }

        @Override
        public String toString() 
        {
            StringBuilder sb = new StringBuilder();
            sb.append('(');
            sb.append(left);
            sb.append(' ');
            sb.append(t);
            sb.append(' ');
            sb.append(right);
            sb.append(')');
            return sb.toString();
        } 
    }
    
    public static class PreUnary extends Expression
    {
        private final Token t;
        private final Expression right;
        
        public PreUnary(Token t, Expression right)
        {
            this.t = t;
            this.right = right;
        }
        
        @Override
        public String toString() 
        {
            StringBuilder sb = new StringBuilder();
            sb.append('(');
            sb.append(t);
            sb.append(right);
            sb.append(')');
            return sb.toString();
        } 
    }
    
    public static class PostUnary extends Expression
    {
        private final Expression left;
        private final Token t;
        
        public PostUnary(Expression left, Token t)
        {
            this.left = left;
            this.t = t;
        }
        
        @Override
        public String toString() 
        {
            StringBuilder sb = new StringBuilder();
            sb.append('(');
            sb.append(left);
            sb.append(t);
            sb.append(')');
            return sb.toString();
        } 
    }
    
    public static class Literal extends Expression
    {
        private final Object o;
        
        public Literal(Object o)
        {
            this.o = o;
        }

        @Override
        public String toString() 
        {
            if(o != null)
            {
                if(o.getClass() == String.class)
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append('"');
                    sb.append(o);
                    sb.append('"');
                    return sb.toString();
                }
                else
                {
                    return o.toString();
                }
            }
            return "null";
        }  
    }
}

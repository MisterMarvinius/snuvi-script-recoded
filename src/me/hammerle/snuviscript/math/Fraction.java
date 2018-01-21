package me.hammerle.snuviscript.math;

import java.util.TreeSet;

public final class Fraction extends Number implements Comparable<Fraction>
{
    private final long numerator;
    private final long denominator;
    
    public Fraction(long numerator, long denominator)
    {
        if(denominator == 0)
        {
            throw new ArithmeticException();
        }
        
        if(denominator != 1)
        {
            long divisor = getGreatestCommonDivisor(numerator, denominator);
            if(divisor != 1)
            {
                denominator /= divisor;
                numerator /= divisor;
            }
        }
        
        // short fraction
        if(denominator < 0)
        {
            this.denominator = -denominator;
            this.numerator = -numerator;
        }
        else
        {
            this.denominator = denominator;
            this.numerator = numerator;
        }
    }
    
    public Fraction(long numerator)
    {
        this(numerator, 1);
    }
    
    private static final long DOUBLE_FACTOR = 10000000000l;
    
    public static Fraction fromDouble(double d)
    {
        if(d == (long) d)
        {
            return new Fraction((long) d);
        }
        return new Fraction(Math.round(d * DOUBLE_FACTOR), DOUBLE_FACTOR);
    }
    
    // -------------------------------------------------------------------------
    // constants, chain fractions
    // ------------------------------------------------------------------------- 
    
    public static final Fraction PI = Fraction.getChainFraction(
              //3,7,15,1,292,1,1,1,2,1,3,1,14,2,1,1,2,2,2,2,1,84,2,1,1,15,3,13,1,4,2,6,6
                3,7,15,1,292,1,1,1,2,1,3,1,14,2,1);
    public static final Fraction E = Fraction.getChainFraction(
              //2,1,2,1,1,4,1,1,6,1,1,8,1,1,10,1,1,12,1,1,14,1,1,16,...
                2,1,2,1,1,4,1,1,6,1,1,8,1,1,10,1,1,12,1,1,14);
    
    public static Fraction getChainFraction(int... ints)
    {
        if(ints.length == 1)
        {
            return new Fraction(ints[0]);
        }
        return new Fraction(ints[0]).add(getChainFractionPart(1, ints));
    }
    
    private static Fraction getChainFractionPart(int index, int... ints)
    {
        if(index + 1 == ints.length)
        {
            return new Fraction(1, ints[index]);
        }
        return new Fraction(ints[index]).add(getChainFractionPart(index + 1, ints)).invert();
    }
    
    // -------------------------------------------------------------------------
    // basic calculating
    // ------------------------------------------------------------------------- 
    
    public Fraction add(Fraction f)
    {
        if(denominator == f.denominator)
        {
            return new Fraction(Math.addExact(numerator, f.numerator), denominator);
        }
        else
        {
            long l = getLeastCommonMultiple(denominator, f.denominator);
            return new Fraction(Math.addExact(Math.multiplyExact(numerator, l / denominator),
                    Math.multiplyExact(f.numerator, l / f.denominator)), l);
        }
    }
    
    public Fraction mul(Fraction f)
    {
        return new Fraction(Math.multiplyExact(numerator, f.numerator), 
                Math.multiplyExact(denominator, f.denominator));
    }
    
    public Fraction sub(Fraction f)
    {
        return add(f.invertSign());
    }
    
    public Fraction div(Fraction f)
    {
        return mul(f.invert());
    }
    
    // -------------------------------------------------------------------------
    // roots, power
    // ------------------------------------------------------------------------- 
    
    private static long power(long l, long power)
    {
        if(power == 1)
        {
            return l;
        }
        else if(power == 2)
        {
            return l * l;
        }
        long factor = 1;
        if(l < 0)
        {
            factor = (power & 1) == 1 ? -1 : 1;
            l = -l;
        }
        long prod = 1;
        while(power > 0)
        {
            if((power & 1) == 1)
            {
                prod = Math.multiplyExact(prod, l);
            }
            power = power >> 1;
            l = Math.multiplyExact(l, l);
        }
        if(factor == -1)
        {
            return -prod;
        }
        return prod;
    }
    
    private static long rootOfLong(long l, long root)
    {
        if(l == 0 || l == 1)
        {
            return l;
        }
        try
        {
            TreeSet<Long> tree = new TreeSet<>();
            long currentValue = l >> 1; // taking half as start value
            long an = currentValue;
            do
            {
                tree.add(currentValue);
                currentValue = currentValue - (currentValue / root) + an / power(currentValue, root - 1); 
            }
            while(!tree.contains(currentValue));
            return currentValue;
        }
        catch(ArithmeticException ex)
        {
            return 0;
        } 
    }
    
    private Fraction root(long root)
    {
        if(root == 1)
        {
            return this.copy();
        }
        // Getting nice start value
        Fraction currentValue;
        Fraction n = new Fraction(root);
        Fraction an = this.div(n);

        Fraction newFraction = new Fraction(rootOfLong(numerator, root), rootOfLong(denominator, root));
        root--;
        try
        {
            do
            {
                currentValue = newFraction;
                newFraction = currentValue.sub(currentValue.div(n)).add(an.div(currentValue.power(root)));              
            }
            while(!newFraction.equals(currentValue));
        }
        catch(ArithmeticException ex)
        {
        }
        return newFraction;
    }
    
    public Fraction power(Fraction f)
    {
        if(numerator < 0 && f.denominator != 1)
        {
            throw new ArithmeticException("root of negative fraction");
        }
        try
        {
            if(f.numerator == 0)
            {
                return new Fraction(1);
            }
            else if(f.numerator < 0)
            {
                return this.invert().power(-f.numerator).root(f.denominator);
            }
            return this.power(f.numerator).root(f.denominator);
        }
        catch(ArithmeticException ex)
        {
            return fromDouble(Math.pow(this.doubleValue(), f.doubleValue()));
        }
    }
    
    private Fraction power(long p)
    {
        if(p < 0)
        {
            p = -p;
            invert();
        }
        else if(p == 1)
        {
            return this.copy();
        }
        long prodn = 1;
        long prodd = 1;
        long n = numerator;
        long d = denominator;
        while(p > 0)
        {
            if((p & 1) == 1)
            {
                prodn = Math.multiplyExact(prodn, n);
                prodd = Math.multiplyExact(prodd, d);
            }
            p = p >> 1;
            n = Math.multiplyExact(n, n);
            d = Math.multiplyExact(d, d);
        }
        return new Fraction(prodn, prodd);
    }
    
    // -------------------------------------------------------------------------
    // inverting
    // -------------------------------------------------------------------------
    
    public Fraction invertSign()
    {
        return new Fraction(-numerator, denominator);
    }
    
    public Fraction invert()
    {
        if(numerator == 0)
        {
            throw new ArithmeticException();
        }
        else if(numerator < 0)
        {
            return new Fraction(-denominator, -numerator);
        }
        return new Fraction(denominator, numerator);
    }
    
    public boolean isNegative()
    {
        return numerator < 0;
    }
    
    public boolean isLong()
    {
        return denominator == 1;
    }
    
    // -------------------------------------------------------------------------
    // functions from math library
    // -------------------------------------------------------------------------
    
    public Fraction abs()
    {
        return new Fraction(Math.abs(numerator), denominator);
    }
    
    public Fraction acos()
    {
        return Fraction.fromDouble(Math.acos(doubleValue()));
    }
    
    public Fraction asin()
    {
        return Fraction.fromDouble(Math.asin(doubleValue()));
    }
    
    public Fraction atan()
    {
        return Fraction.fromDouble(Math.atan(doubleValue()));
    }
    
    public Fraction atan2(Fraction f)
    {
        return Fraction.fromDouble(Math.atan2(doubleValue(), f.doubleValue()));
    }
    
    public Fraction cbrt()
    {
        return this.power(new Fraction(1, 3));
    }
    
    public Fraction ceil()
    {
        return Fraction.fromDouble(Math.ceil(doubleValue()));
    }
    
    public Fraction cos()
    {
        return Fraction.fromDouble(Math.cos(doubleValue()));
    }
    
    public Fraction cosh()
    {
        return Fraction.fromDouble(Math.cosh(doubleValue()));
    }
    
    public Fraction floor()
    {
        return Fraction.fromDouble(Math.floor(doubleValue()));
    }
    
    public Fraction log()
    {
        return Fraction.fromDouble(Math.log(doubleValue()));
    }
    
    public Fraction log10()
    {
        return Fraction.fromDouble(Math.log10(doubleValue()));
    }
    
    public Fraction log1p()
    {
        return Fraction.fromDouble(Math.log1p(doubleValue()));
    }
    
    public Fraction max(Fraction f)
    {
        if(this.compareTo(f) < 0)
        {
            return f;
        }
        return this;
    }
    
    public Fraction min(Fraction f)
    {
        if(this.compareTo(f) > 0)
        {
            return f;
        }
        return this;
    }
    
    public Fraction rint()
    {
        return Fraction.fromDouble(Math.rint(doubleValue()));
    }
    
    public Fraction round()
    {
        return Fraction.fromDouble(Math.round(doubleValue()));
    }
    
    public Fraction round(int times)
    {
        if(times < 0)
        {
            throw new IllegalArgumentException("a positive number of decimal points is needed");
        }
        int factor = 1;
        while(times > 0)
        {
            factor *= 10;
            times--;
        }
        double d = doubleValue() * factor;
        return new Fraction(Math.round(d), factor);
    }
    
    public Fraction signum()
    {
        if(numerator < 0)
        {
            return new Fraction(-1);
        }
        return new Fraction(1);
    }
    
    public Fraction sin()
    {
        return Fraction.fromDouble(Math.sin(doubleValue()));
    }
    
    public Fraction sinh()
    {
        return Fraction.fromDouble(Math.sinh(doubleValue()));
    }
    
    public Fraction tan()
    {
        return Fraction.fromDouble(Math.tan(doubleValue()));
    }
    
    public Fraction tanh()
    {
        return Fraction.fromDouble(Math.tanh(doubleValue()));
    }
    
    public Fraction toDegrees()
    {
        return Fraction.fromDouble(Math.toDegrees(doubleValue()));
    }
    
    public Fraction toRadians()
    {
        return Fraction.fromDouble(Math.toRadians(doubleValue()));
    }
    
    // -------------------------------------------------------------------------
    // simplifying
    // ------------------------------------------------------------------------- 
    
    public Fraction simplify(int times)
    {
        if(denominator == 1)
        {
            return this.copy();
        }
        long d = denominator;
        long n = numerator;
        int switcher = -1;
        for(int i = 0; i < times; i++)
        {
            if(switcher == -1 && d == 1)
            {
                d = (d & 1) == 1 ? d + 1: d;
                n = (n & 1) == 1 ? n + 1 : n;
                switcher = -1;
            }
            else if(switcher == 1 && d == -1)
            {
                d = (d & 1) == 1 ? d - 1: d;
                n = (n & 1) == 1 ? n - 1 : n;
                switcher = 1;
            }
            else
            {
                d = (d & 1) == 1 ? d + switcher: d;
                n = (n & 1) == 1 ? n + switcher : n;
                switcher = -switcher;
            }
            
            if(d != 1)
            {
                long divisor = getGreatestCommonDivisor(n, d);
                //System.out.println("DIV: " + divisor);
                if(divisor != 1)
                {
                    d /= divisor;
                    n /= divisor;
                }
            }
        }
        return new Fraction(n, d);
    }
    
    private long getGreatestCommonDivisor(long i, long n)
    {
        if(i == 0)
        {
            return n;
        }
        if(i < 0)
        {
            i = -i;
        }
        if(n < 0)
        {
            n = -n;
        }
        long helper;
        while(true)
        {
            if(i < n)
            {
                helper = i;
                i = n;
                n = helper;
            }
            i = i % n;
            if(i == 0)
            {
                return n;
            }
        }
    }
    
    private long getLeastCommonMultiple(long i, long n)
    {
        return Math.abs(Math.multiplyExact(i, n)) / getGreatestCommonDivisor(i, n);
    }
    
    // -------------------------------------------------------------------------
    // basic stuff
    // ------------------------------------------------------------------------- 
    
    @Override
    public String toString() 
    {
        if(denominator == 1)
        {
            return String.valueOf(numerator);
        }
        return numerator + " / " + denominator;
    }

    private Fraction copy()
    {
        return new Fraction(numerator, denominator);
    }

    @Override
    public boolean equals(Object o) 
    {
        if(o == null)
        {
            return false;
        }
        else if(o.getClass() != Fraction.class)
        {
            return false;
        }
        Fraction f = (Fraction) o;
        return numerator == f.numerator && denominator == f.denominator;
    }

    @Override
    public int hashCode() 
    {
        int hash = 3;
        hash = 97 * hash + (int) (this.numerator ^ (this.numerator >>> 32));
        hash = 97 * hash + (int) (this.denominator ^ (this.denominator >>> 32));
        return hash;
    }

    @Override
    public int compareTo(Fraction f) 
    {
        if(f.numerator < 0 && numerator >= 0)
        {
            return 1;
        }
        else if(f.numerator >= 0 && numerator < 0)
        {
            return -1;
        }
        else
        {
            long i = f.sub(this).numerator;
            if(i == 0)
            {
                return 0;
            }
            else if(i < 0)
            {
                return 1;
            }
            else
            {
                return -1;
            }
        }
    }
    
    // -------------------------------------------------------------------------
    // bit stuff
    // -------------------------------------------------------------------------
    
    private void noFraction()
    {
        if(denominator != 1 && numerator != 0)
        {
            throw new UnsupportedOperationException("the number must not be a fraction");
        }
    }
    
    public Fraction rightShift(int times)
    {
        noFraction();
        return new Fraction(numerator >> times);
    }
    
    public Fraction leftShift(int times)
    {
        noFraction();
        return new Fraction(numerator << times);
    }
    
    public Fraction and(Fraction f)
    {
        noFraction();
        return new Fraction(numerator & f.numerator);
    }
    
    public Fraction or(Fraction f)
    {
        noFraction();
        return new Fraction(numerator | f.numerator);
    }
    
    public Fraction xor(Fraction f)
    {
        noFraction();
        return new Fraction(numerator ^ f.numerator);
    }
    
    public Fraction invertBits()
    {
        noFraction();
        return new Fraction(~numerator);
    }

    public Fraction setBit(int n)
    {
        noFraction();
        return new Fraction(numerator | 1 << n);
    }
    
    public Fraction unsetBit(int n)
    {
        noFraction();
        return new Fraction(numerator & ~(1 << n));
    }   
    
    public boolean getBit(int n)
    {
        noFraction();
        return (numerator & (1 << n)) != 0;
    }
    
    // -------------------------------------------------------------------------
    // number stuff
    // ------------------------------------------------------------------------- 

    @Override
    public int intValue() 
    {
        return (int) longValue();
    }

    @Override
    public long longValue()
    {
        return numerator / denominator;
    }

    @Override
    public float floatValue() 
    {
        return (float) doubleValue();
    }

    @Override
    public double doubleValue()
    {
        return numerator / (double) denominator;
    }
}

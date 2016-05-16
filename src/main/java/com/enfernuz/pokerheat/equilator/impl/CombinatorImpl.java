package com.enfernuz.pokerheat.equilator.impl;

import java.util.*;

import java.util.stream.Collectors;

import java.math.BigInteger;

import com.google.common.collect.*;

import com.enfernuz.pokerheat.core.util.Combinator;

/**
 *
 * @author A. Nerushev
 */
public class CombinatorImpl<E> implements Combinator<E> {

    @Override
    public Collection<Collection<E>> kCombinationsFromN(final Collection<? extends E> elements, final int k) {
        
        final int n = elements.size();
        if (n < k) {
            return ImmutableList.of();
        }

        final ImmutableSet<E> elementsSet = ImmutableSet.copyOf(elements);
        final ImmutableList<E> elementsList = ImmutableList.copyOf(elementsSet);
        
        if (n <= 32) {
            return slightlyFasterKCombinationsFromN(elementsList, k);
        }

        BigInteger x = BigInteger.ZERO;
        for (int i = 0; i < k; i++) {
            x = x.setBit(i);
        }
        
        final Collection<BigInteger> kCombinations = new LinkedList<>();
        kCombinations.add(x);
        
        //Gosper's hack
        BigInteger kCombination = x;
        final BigInteger stopper = BigInteger.ONE.shiftLeft(n);
        while (true) {
            
            kCombination = nextKCombination(kCombination, stopper);
            if (kCombination == BigInteger.ZERO) 
                break;
            kCombinations.add(kCombination);
        }

        return kCombinations.parallelStream()
                        .map( c -> combinadicToCombination(c, elementsList) )
                        .collect( Collectors.toCollection(ArrayList::new) );
        
//        for ( BigInteger c : kCombinations.build() ) {
//            
//            aResultCombination = ImmutableSet.builder();
//            for(int i = 0; i < elementsList.size(); i++) {
//                if( c.testBit(i) ) {
//                    aResultCombination.add( elementsList.get(i) ); 
//                }
//            }
//            result.add( aResultCombination.build() );
//        }
        
//        return result;
    }
    
    private static <E> ImmutableCollection<E> combinadicToCombination(final BigInteger combinadic, final List<? extends E> elementsList) {
        
        final ImmutableCollection.Builder<E> aResultCombination = ImmutableList.builder();
        for (int i = 0; i < elementsList.size(); i++) {
            if ( combinadic.testBit(i) ) {
                aResultCombination.add( elementsList.get(i) ); 
            }
        }

        return aResultCombination.build();
    }
    
    private static <E> ImmutableCollection<E> combinadicToCombination(final long combinadic, final List<? extends E> elementsList) {
        
        final ImmutableCollection.Builder<E> aResultCombination = ImmutableList.builder();
        for (int i = 0; i < elementsList.size(); i++) {
            if ( (combinadic & (1 << i)) != 0 ) {
                aResultCombination.add( elementsList.get(i) ); 
            }
        }

        return aResultCombination.build();
    }
    
    private Collection<Collection<E>> slightlyFasterKCombinationsFromN(List<? extends E> elementsList, int k) {
        
        final int n = elementsList.size();

        long firstCombination = 0L;
        for (int i = 0; i < k; i++) {
            firstCombination += 1 << i;
        }
        
        final Collection<Long> kCombinations = new LinkedList<>();
        kCombinations.add(firstCombination);
        
        long x = firstCombination;
        //Gosper's hack
        while (true) {
            
            x = nextKCombination(x, 1 << n);
            if (x == 0L) 
                break;
            kCombinations.add(x);
        }
        
        return kCombinations.parallelStream()
                        .map( c -> combinadicToCombination(c, elementsList) )
                        .collect( Collectors.toCollection(ArrayList::new) );
        
//        final Collection<Collection<E>> result = new LinkedList<>();
//        Collection<E> aResultCombination;
//        for (Long c : kCombinations) {
//            
//            aResultCombination = new ArrayList<>(k);
//            for(int i = 0; i < n; i++) {
//                if( (c & (1 << i)) != 0 ) {
//                    aResultCombination.add( elementsList.get(i) ); 
//                }
//            }
//            result.add( aResultCombination );
//        }
        
//        return result;
    }
    
    // credits to sir Ralph William Gosper, Jr.
    private static BigInteger nextKCombination(BigInteger x, BigInteger stopper) {

        final BigInteger u = x.and( x.negate() );
        final BigInteger v = x.add(u);
        
        return v.equals(stopper) ? BigInteger.ZERO : v.add( ( (v.xor(x)).divide(u) ).shiftRight(2) );
    }
    
    private static long nextKCombination(long x, long stopper) {
        
        final long u = x & -x;
        final long v = x + u;

        if (v == stopper) {
            return 0L;
        }
        
        return (v == stopper) ? 0L : ( v + ( ((v^x) / u) >> 2 ) );
    }

}

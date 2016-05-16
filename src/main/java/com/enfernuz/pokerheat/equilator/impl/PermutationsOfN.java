package com.enfernuz.pokerheat.equilator.impl;

import java.util.*;

import com.enfernuz.pokerheat.core.util.Combinator;

import com.google.common.collect.*;

/**
 * Adapted from:
 * http://stackoverflow.com/a/11123951/2031954
 * 
 * @author mrswadge
 */
public class PermutationsOfN<E> implements Combinator<E> {

    @Override
    public Collection<Collection<E>> kCombinationsFromN(Collection<? extends E> elements, int k) {
        
        final ImmutableList<E> set = ImmutableList.copyOf(elements);
        
        final int setSize = set.size();
        if (k > setSize) {
            k = setSize;
        }
        
        final Collection<Collection<E>> result = new ArrayList<>();
        final List<E> subset = Lists.newArrayListWithCapacity(k);
        
        for (int i = 0; i < k; i++) {
            subset.add(null);
        }
        
        return processLargerSubsets(result, set, subset, 0, 0, setSize);
    }

    private Collection<Collection<E>> processLargerSubsets(Collection<Collection<E>> result, List<? extends E> set, List<E> subset, int subsetSize, int nextIndex, int ss) {
        
        if( subsetSize == subset.size() ) {
            result.add( ImmutableList.copyOf(subset) );
        } else {
            for (int j = nextIndex; j < ss; j++) {
                subset.set( subsetSize, set.get(j) );
                processLargerSubsets(result, set, subset, subsetSize + 1, j + 1, ss);
            }
        }
        
        return result;
    }

//    public Collection<List<T>> permutations(List<T> list, int size) {
//        
//        final Collection<List<T>> all = new ArrayList<>();
//        
//        if( list.size() < size ) {
//            size = list.size();
//        }
//        
//        if( list.size() == size ) {
//            all.addAll(Collections2.permutations(list));
//        } else {
//            for( final List<T> p : processSubsets(list, size) ) {
//                all.addAll( Collections2.permutations(p) );
//            }
//        }
//        
//        return all;
//    }
    
}

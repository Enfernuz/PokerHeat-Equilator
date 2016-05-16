package com.enfernuz.pokerheat.equilator.impl;

import java.util.*;

import java.util.concurrent.atomic.AtomicReferenceArray;

import java.util.function.Predicate;

import java.util.stream.*;

import com.google.common.collect.*;

import com.enfernuz.pokerheat.core.*;
import com.enfernuz.pokerheat.core.PokerCombination.CombinationType;

import com.enfernuz.pokerheat.equilator.warehouse.*;

import static com.google.common.base.Preconditions.*;

/**
 *
 * @author A. Nerushev
 */
public class CombinationEvaluatorImpl implements CombinationEvaluator {
    
    private static final int TOTAL_RANKS;
    private static final int ALL_POSSIBLE_COMBINATIONS_COUNT;
    private static final Multimap<CombinationType, Long> COMBINATIONS_BINARY_MASKS;
    //works a bit faster than ConcurrentHashMap<Long, PokerCombination>
    private static final AtomicReferenceArray<PokerCombination> CACHE;
    
    private final PokerCombinationRepository combinationRepository;
    
    static {
        
        TOTAL_RANKS = Card.Rank.values().length;
        ALL_POSSIBLE_COMBINATIONS_COUNT = 2598960;
        CACHE = new AtomicReferenceArray<>(ALL_POSSIBLE_COMBINATIONS_COUNT);
        COMBINATIONS_BINARY_MASKS = HashMultimap.create();
        
        init();
    }
    
    public CombinationEvaluatorImpl(PokerCombinationRepository combinationRepository) {

        this.combinationRepository = Objects.requireNonNull(combinationRepository, "The parameter 'combinationRepository' must not be null."); 
    }
    
    private static void init() {
        
        final int pairHitZonePtr = (TOTAL_RANKS - 1) * 1;
        final int threeOfaKindHitZonePtr = (TOTAL_RANKS - 1) * 2;
        final int fourOfaKindHitZonePtr = (TOTAL_RANKS - 1) * 3;

        for ( Card.Rank rank : Card.Rank.values() ) {
            
            final int rankIndex = rank.getIndex();

            // the "Pair" binary masks
            COMBINATIONS_BINARY_MASKS.put( CombinationType.PAIR, 1L << (pairHitZonePtr + rankIndex) );

            // the "Three of a kind" binary masks
            COMBINATIONS_BINARY_MASKS.put( CombinationType.THREE_OF_A_KIND, 1L << (threeOfaKindHitZonePtr + rankIndex) );

            // the "Four of a kind" binary masks 
            COMBINATIONS_BINARY_MASKS.put( CombinationType.FOUR_OF_A_KIND, 1L << (fourOfaKindHitZonePtr + rankIndex) );
        }
        
        // the "Two pairs" and "Full House" combinations' binary masks
        for ( Card.Rank firstRank : Card.Rank.values() ) {

            final int firstPairPtr = pairHitZonePtr + firstRank.getIndex();

            for( Card.Rank secondRank : Card.Rank.values() ) {

                if( secondRank == firstRank) {
                    continue;
                }

                final int secondPairPtr = pairHitZonePtr + secondRank.getIndex();
                    
//              final BigInteger twoPairBinaryMask = 
//                            BigInteger.ONE.shiftLeft(firstPairPtr)
//                                    .add( BigInteger.ONE.shiftLeft(secondPairPtr) );
                    
                final long twoPairBinaryMask = (1L << firstPairPtr) + (1L << secondPairPtr);

                COMBINATIONS_BINARY_MASKS.put(CombinationType.TWO_PAIRS, twoPairBinaryMask);
                
                final int threeOfaKindPtr = threeOfaKindHitZonePtr + secondRank.getIndex();

//              final BigInteger fullHouseBinaryMask =
//                            BigInteger.ONE.shiftLeft(firstPairPtr)
//                                    .add( BigInteger.ONE.shiftLeft(threeOfaKindPtr) );

                final long fullHouseBinaryMask = (1L << firstPairPtr) + (1L << threeOfaKindPtr);

                COMBINATIONS_BINARY_MASKS.put(CombinationType.FULL_HOUSE, fullHouseBinaryMask);
            }

        }
        
        /*
         * The "Straight" binary masks -- all the binary views which have a series 
         * of 5 consecutive bits with value of 1 in the 1st hit zone.
         */
        
        for ( Card.Rank rank : Card.Rank.values() ) {
            
            if (rank.compareTo(Card.Rank.FIVE) < 0) {
                continue;
            }
            
            // will break with an inproper indexation in the Card.Rank, but after all we are the architects here
            // the binary mask of the highest rank in the straight
//          final BigInteger highestStraightRankBinaryView = 
//          BigInteger.ONE.shiftLeft( rank.getIndex() );
            final long highestStraightRankBinaryView = 1L << rank.getIndex();
            // the binary mask of the second highest rank in the straight, and so on
//          final BigInteger _2ndHighestRankBinaryView = 
//              highestStraightRankBinaryView.shiftRight(1);
            final long _2ndHighestRankBinaryView = highestStraightRankBinaryView >> 1;
//          final BigInteger _3rdHighestRankBinaryView = 
//              _2ndHighestRankBinaryView.shiftRight(1);
            final long _3rdHighestRankBinaryView = _2ndHighestRankBinaryView >> 1;
//          final BigInteger _4thHighestRankBinaryView = 
//              _3rdHighestRankBinaryView.shiftRight(1);
            final long _4thHighestRankBinaryView = _3rdHighestRankBinaryView >> 1;

            final long lowestStraightRankBinaryView;
            // a "wheel" straight (5432A)
            if (rank == Card.Rank.FIVE) {
                //lowestStraightRankBinaryView = BigInteger.ONE.shiftLeft( Card.Rank.ACE.getIndex() );
                lowestStraightRankBinaryView = 1L << Card.Rank.ACE.getIndex();
            } else {
                //lowestStraightRankBinaryView = _4thHighestRankBinaryView.shiftRight(1);
                lowestStraightRankBinaryView = _4thHighestRankBinaryView >> 1;
            }

//                        final BigInteger straightBinaryMask = 
//                                highestStraightRankBinaryView
//                                        .add( _2ndHighestRankBinaryView )
//                                        .add( _3rdHighestRankBinaryView )
//                                        .add( _4thHighestRankBinaryView )
//                                        .add( lowestStraightRankBinaryView );
                        
            final long straightBinaryMask = 
                    highestStraightRankBinaryView
                            + _2ndHighestRankBinaryView
                            + _3rdHighestRankBinaryView
                            + _4thHighestRankBinaryView
                            + lowestStraightRankBinaryView;
                        
            COMBINATIONS_BINARY_MASKS.put(CombinationType.STRAIGHT, straightBinaryMask);
        }
        
    }

    @Override
    public PokerCombination evaluate(Collection<? extends Card> cards) {
        
        PokerCombination result;
        
        final Long combinadic = getCombinadic(cards);
        
        // the combinadic is a non-negative long by design
        final int index = (int) (combinadic % ALL_POSSIBLE_COMBINATIONS_COUNT);
        
        result = CACHE.get(index);

        if (result != null) {
            return result;
        }
        
        final Optional<PokerCombinationEntity> maybeCombinationEntitity =
                combinationRepository.findById(combinadic);
        
        final PokerCombinationEntity entity;
        
        if ( maybeCombinationEntitity.isPresent() ) {
            entity = maybeCombinationEntitity.get();
        } else {
            
            final CombinationType combinationType = evaluateCombinationType(cards);
            long binaryView = getBinaryView(cards);
            
            switch (combinationType) {
                case ROYAL_FLUSH:
                    binaryView += (1L << 57);
                    break;
                case FLUSH_STRAIGHT:
                    binaryView += (1L << 56);
                    break;
                case FOUR_OF_A_KIND:
                    binaryView += (1L << 55);
                    break;
                case FULL_HOUSE:
                    binaryView += (1L << 54);
                    break;
                case FLUSH:
                    binaryView += (1L << 53);
                    break;
                case STRAIGHT:
                    binaryView += (1L << 52);
                    if ( IS_WHEEL_PREDICATE.test(cards) ) {
                        binaryView -= (1L << 12);
                    }
                    break;
                case THREE_OF_A_KIND:
                    binaryView += (1L << 51);
                    break;
                case TWO_PAIRS:
                    binaryView += (1L << 50);
                    break;
                case PAIR:
                    binaryView += (1L << 49);
                    break;
                case HIGH_CARD:
                    binaryView += (1L << 48);
                    break;
                default:
                    throw new AssertionError();
            }
            
            entity = new PokerCombinationEntity();
            entity.setCombinadic(combinadic);
            entity.setCombinationType(combinationType);
            entity.setBinaryView(binaryView);

            combinationRepository.saveOrUpdate(entity);
        }
        
        result = PokerCombinationProxy.of(
                new PokerCombination(entity.getCombinationType(), ImmutableSet.copyOf(cards)), 
                entity.getBinaryView()
        );
        
        CACHE.compareAndSet(index, null, result);

        return CACHE.get(index);
    }

    @Override
    public int compare(PokerCombination first, PokerCombination second) {
        
        final boolean firstIsProxy = first instanceof PokerCombinationProxy;
        final boolean secondIsProxy = second instanceof PokerCombinationProxy;
        
        final int result;
        
        if (firstIsProxy && secondIsProxy) {
            
            result = Long.compare( 
                    ((PokerCombinationProxy) first).binaryView, 
                    ((PokerCombinationProxy) second).binaryView
            );
            
        } else if (firstIsProxy && !secondIsProxy) {
            
            final long binaryView = getBinaryView( second.getCards() );
            result = Long.compare(((PokerCombinationProxy) first).binaryView, binaryView);
            
        } else if (!firstIsProxy && secondIsProxy) {
            
            final long binaryView = getBinaryView( first.getCards() );
            result = Long.compare(binaryView, ((PokerCombinationProxy) second).binaryView);
            
        } else {
            
            final long firstBinaryView = getBinaryView( first.getCards() );
            final long secondBinaryView = getBinaryView( second.getCards() );
            result = Long.compare(firstBinaryView, secondBinaryView);
        }
        
        return result;
    }
    
    public static long getCombinadic(Iterable<? extends Card> cards) {
        
        /*
        * Long is appropriate as long as the number of unique cards are no greater than Long.SIZE.
        * We'll take a leap of faith and assume that there will forever be a standard 52 cards deck.
        * In case where it won't, this method must be refactored to use BigInteger.
        */
        
        long result = 0L;
        for (Card card : cards) {
            result += ( 1L << ( card.getSuit().getIndex() * TOTAL_RANKS + card.getRank().getIndex() ) );
        }
        
        return result;
    }
    
    public static long getBinaryView(Iterable<? extends Card> cards) {
        
        final Map<Card.Rank, Integer> frequencyMap = new HashMap<>();
        Integer count;
        
        for (Card card : cards) {
            
            final Card.Rank rank = card.getRank();
            count = frequencyMap.get(rank);
            
            if (count == null) {
                frequencyMap.put(rank, 1);
            } else {
                frequencyMap.put(rank, count + 1);
            }
            
            
        }

        long binaryView = 0L;
        for( Map.Entry<Card.Rank, Integer> entry : frequencyMap.entrySet() ) {
            
            final int hitZonePtr = (entry.getValue() - 1) * TOTAL_RANKS;

            final int rankZonedHitPtr = hitZonePtr + entry.getKey().getIndex();
            
            binaryView += (1L << rankZonedHitPtr);
        }

        return binaryView;
    }
    
    private static CombinationType evaluateCombinationType(Collection<? extends Card> cards) {
        
        checkArgument(
                cards.size() == PokerCombination.COMBINATION_SIZE,
                String.format("The collection parameter 'cards' must be the size of %d (passed %d).", PokerCombination.COMBINATION_SIZE, cards.size())
        );
        
        CombinationType result = null;

        boolean isFlush = true;
        final Iterator<? extends Card> cardsIterator = cards.iterator();
        final Card.Suit flushSuit = cardsIterator.next().getSuit();
        //iterate over the rest of the cards to check if it's a flush
        while ( cardsIterator.hasNext() ) {
            if (cardsIterator.next().getSuit() != flushSuit) {
                isFlush = false;
                break;
            }
        }
                
        final StringBuilder suitImageBuilder = new StringBuilder();
        cards.stream().forEach(card -> {
            suitImageBuilder.append( card.getSuit().toString() );
        });

        final long binaryView = getBinaryView(cards);

        //check if it's a flush
        //if( FLUSH_SUIT_MASKS.contains( suitImageBuilder.toString() ) ) {
        if (isFlush) {

            //check if it's a straight
            if ( COMBINATIONS_BINARY_MASKS.get(CombinationType.STRAIGHT).contains(binaryView) ) {
                result = CombinationType.FLUSH_STRAIGHT;
            } else {
                result = CombinationType.FLUSH;
            }

        } else {

            for( Long mask : COMBINATIONS_BINARY_MASKS.get(CombinationType.FOUR_OF_A_KIND) ) {
                //if( binaryView.and(mask).equals( mask ) ) {
                if ( (binaryView & mask) == mask ) {
                    result = CombinationType.FOUR_OF_A_KIND;
                    break;
                }
            }

            if (result == null) {

                if( COMBINATIONS_BINARY_MASKS.get(CombinationType.FULL_HOUSE).contains(binaryView) ) {
                    result = CombinationType.FULL_HOUSE;
                }
            }

            if (result == null) {

                if( COMBINATIONS_BINARY_MASKS.get(CombinationType.STRAIGHT).contains(binaryView) ) {
                    result = CombinationType.STRAIGHT;
                }
            }

            if (result == null) {

                for ( Long mask : COMBINATIONS_BINARY_MASKS.get(CombinationType.THREE_OF_A_KIND) ) {
                    //if( binaryView.and(mask).equals( mask ) ) {
                    if ( (binaryView & mask) == mask ) {
                        result = CombinationType.THREE_OF_A_KIND;
                        break;
                    }
                }

            }

            if (result == null) {

                for ( Long mask : COMBINATIONS_BINARY_MASKS.get(CombinationType.TWO_PAIRS) ) {
                    //if( binaryView.and(mask).equals( mask ) ) {
                    if ( (binaryView & mask) == mask ) {
                        result = CombinationType.TWO_PAIRS;
                        break;
                    }
                }

            }

            if (result == null) {

                for ( Long mask : COMBINATIONS_BINARY_MASKS.get(CombinationType.PAIR) ) {
                    //if( binaryView.and(mask).equals( mask ) ) {
                    if ( (binaryView & mask) == mask ) {
                        result = CombinationType.PAIR;
                        break;
                    }
                }

            }

            if (result == null) {
                result = CombinationType.HIGH_CARD;
            }
        }

        return result;
    }
    
    private static final Predicate<Collection<? extends Card>> IS_WHEEL_PREDICATE = (cards) -> {
                    
        final Set<Card.Rank> ranks = 
                cards.stream()
                        .map( Card::getRank )
                        .collect( Collectors.toSet() );

        return (ranks.contains(Card.Rank.ACE) && ranks.contains(Card.Rank.TWO) );
    };
    
    private static class PokerCombinationProxy extends PokerCombination {
        
        private final long binaryView;
        private final PokerCombination target;
        
        private PokerCombinationProxy(PokerCombination target, long binaryView) {
            
            super();
            
            this.target = target;
            this.binaryView = binaryView;
        }
        
        private static PokerCombinationProxy of(PokerCombination target, long binaryView) {
            
            return new PokerCombinationProxy(target, binaryView);
        }
        
    }
}

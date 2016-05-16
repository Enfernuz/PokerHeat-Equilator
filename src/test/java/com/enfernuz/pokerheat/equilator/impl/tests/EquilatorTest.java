package com.enfernuz.pokerheat.equilator.impl.tests;

import java.util.Map;

import com.enfernuz.pokerheat.core.*;
import com.enfernuz.pokerheat.core.holdem.HoldemHand;
import com.enfernuz.pokerheat.core.util.Combinator;
import com.enfernuz.pokerheat.equilator.impl.PermutationsOfN;

import com.enfernuz.pokerheat.equilator.impl.*;
import com.enfernuz.pokerheat.equilator.warehouse.PokerCombinationRepository;

import com.google.common.collect.*;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

/**
 *
 * @author A. Nerushev
 */

@RunWith(JUnit4.class)
public class EquilatorTest {

    private static Equilator<HoldemHand> equilator;
    private static CardParser parser;
    
    private static PokerCombinationRepository repository;
    private static CombinationEvaluator evaluator;
    private static Combinator<Card> cardCombinator;
    private static HandExtractor<HoldemHand> handExtractor;
    
    @BeforeClass
    public static void commonSetup() {
        
        repository = new PokerCombinationRepositoryImpl();
        evaluator = new CombinationEvaluatorImpl(repository);
        
        cardCombinator = new PermutationsOfN<>();
        handExtractor = new HoldemHandExtractorImpl(cardCombinator);
        
        equilator = new HoldemEquilatorImpl(evaluator, cardCombinator, handExtractor);
        
        parser = CardParserImpl.INSTANCE;
    }
    
    @AfterClass
    public static void commonTearDown() {
        
        repository = null;
        evaluator = null;
        
        cardCombinator = null;
        handExtractor = null;
        
        equilator = null;
        
        parser = null;
    }
    
    
    @Test
    public void test() {
        
        final ImmutableSet<Card> cards = ImmutableSet.copyOf( parser.parse("AhKh") );
        
        System.out.println( String.format("Cards for hero: %s", cards.toString()));
        
        final HoldemHand heroHand = HoldemHand.of(cards);
        
        final Board board = Board.of();
        
        parser.parse("Jc2d9s").forEach( board::accept );
        
        long start = System.currentTimeMillis();
        final Map<HoldemHand, Equity> result = equilator.calculateEquities(board, heroHand);
        System.out.println( String.format( "Time taken: %d", System.currentTimeMillis() - start) );
        //System.out.println( String.format( "Keys: %d", result.keySet().size()) );
        
        start = System.currentTimeMillis();
        final Map<HoldemHand, Equity> result2 = equilator.calculateEquities( board, heroHand);
        System.out.println( String.format( "Time taken: %d", System.currentTimeMillis() - start) );
        //System.out.println( String.format( "Keys: %d", result2.keySet().size()) );
        
        System.out.println( result.size() );
        
//        for(Map.Entry<HoldemPokerHand, Equity> entry : result2.entrySet()) {
//            
//            System.out.println( String.format("Key: %s, Value: %s", entry.getKey(), entry.getValue()));
//        }
//        for ( final Equity equity : result2.values() ) {
//            System.out.println(equity.toString());
//        }
    }
}

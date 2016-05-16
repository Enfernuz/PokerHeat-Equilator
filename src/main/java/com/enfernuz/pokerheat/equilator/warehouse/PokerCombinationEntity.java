package com.enfernuz.pokerheat.equilator.warehouse;

import java.io.Serializable;

import javax.persistence.*;

import com.enfernuz.pokerheat.core.PokerCombination.CombinationType;

/**
 *
 * @author A. Nerushev
 */

@Entity
@Table(name = "POKER_COMBINATIONS")
public class PokerCombinationEntity implements Serializable {

    @Id
    private long combinadic;
    
    @Enumerated
    private CombinationType combinationType;
    
    private long binaryView;

    public long getCombinadic() {
        return combinadic;
    }

    public void setCombinadic(long combinadic) {
        this.combinadic = combinadic;
    }

    public CombinationType getCombinationType() {
        return combinationType;
    }

    public void setCombinationType(CombinationType combinationType) {
        this.combinationType = combinationType;
    }

    public long getBinaryView() {
        return binaryView;
    }

    public void setBinaryView(long binaryView) {
        this.binaryView = binaryView;
    }
}

package server.model.board;

import server.model.effect.EffectSurplus;
import server.model.effect.ImmediateEffect;

import java.io.Serializable;

/**
 * Created by Federico on 11/05/2017.
 * Methods implemented by LBARCELLA on 18/05/2017.
 */
public class SingleActionPlace extends ActionPlace implements Serializable {
    private FamilyMember familyMember;
    private boolean occupied;

    public SingleActionPlace(EffectSurplus effectSurplus, Integer entryForce, Integer minPlayers) {
        super(effectSurplus, entryForce, minPlayers);
        this.occupied = false;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public FamilyMember getFamilyMember() {
        return familyMember;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public void setFamilyMember(FamilyMember familyMember) {

        this.familyMember = familyMember;
        this.setOccupied(true);
    }

    public void clean(){

        this.familyMember = null;
        this.occupied = false;
    }
    
}

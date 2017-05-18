package server.model.board;

import server.model.effect.EffectSurplus;

/**
 * Created by Federico on 11/05/2017.
 */
public class BonusTile {
    private Integer id;
    private Integer productionMinForce;
    private EffectSurplus productionSurplus;
    private Integer harvestMinForce;
    private EffectSurplus harvestSurplus;



    public BonusTile(Integer id, Integer productionMinForce, EffectSurplus productionSurplus, Integer harvestMinForce, EffectSurplus harvestSurplus){
        this.id=id;
        this.harvestMinForce= harvestMinForce;
        this.harvestSurplus = harvestSurplus;
        this.productionMinForce = productionMinForce;
        this.productionSurplus = productionSurplus;
    }

    public Integer getId() {
        return id;
    }

    public Integer getHarvestMinForce() {
        return harvestMinForce;
    }

    public EffectSurplus getHarvestSurplus() {
        return harvestSurplus;
    }

    public Integer getProductionMinForce() {
        return productionMinForce;
    }

    public EffectSurplus getProductionSurplus() {
        return productionSurplus;
    }
}

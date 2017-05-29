package server.controller.game;
import exception.*;
import logger.Level;
import logger.Logger;
import netobject.request.action.*;
import server.controller.network.ClientHandler;
import server.model.*;
import server.model.board.*;
import server.model.card.developement.*;
import server.model.effect.*;
import server.model.effect.ActionType;
import server.model.valuable.Point;
import server.model.valuable.Resource;
import server.utility.BoardConfigParser;

import java.util.*;
import java.util.concurrent.BlockingQueue;

import static server.utility.BoardConfigParser.getVictoryBonusFromRanking;

/*
 * @author  ab3llini
 * @since   15/05/17.
 */

/**
 * The controller of the match.
 * Will handle the model instance reacting to game events.
 */
public class MatchController implements Runnable {

    /**
     * The model instance of the match
     */
    private Match match;

    /**
     * The instance of the board controller
     */
    private BoardController boardController;

    /**
     * This property maps each player in the model with his relative remote one
     */
    private LinkedHashMap<Player, RemotePlayer> remotePlayerMap;

    /**
     * This queue holds all the action that need processing from the active player
     */
    private BlockingQueue<ActionRequest> actionRequests;

    /**
     * Holds a reference to the player of the model who is performing the move
     */
    private Player activePlayer;

    /**
     * Describes who has the turn
     */
    private Player currentPlayer;

    /**
     * This is the match controller constructor.
     * It is called only by the lobby itself when the match starts
     * @param handlers the handlers of the model players
     */
    public MatchController(ArrayList<ClientHandler> handlers) {

        /*
         * Initialize the map
         */
        this.remotePlayerMap = new LinkedHashMap<Player, RemotePlayer>();

        /*
         * Create a temporary list of players that will be passed to the Match
         * For each handler create a map entry and add it to the temporary list
         */
        ArrayList<Player> players = new ArrayList<Player>();

        for (ClientHandler handler : handlers) {

            Player player = new Player(handler.getUsername());

            this.remotePlayerMap.put(player, handler);

            players.add(player);

        }

        /*
         * First up, create the model for the current match.
         * Note that this call will trigger every constructor in the model
         * The players are always provided
         */
        this.match = new Match(players);


        /*
         * Assign the board controller
         * Keep in mind that match.board must be initialized at this time
         */
        this.boardController = new BoardController(this.match.getBoard());


        //Init anything else in the future here..

    }

    /**
     * costruttore temporaneo usato solo per testare le classi
     * @param players
     */
    public MatchController(ArrayList<Player> players,Integer xx) {

        /*
         * First up, create the model for the current match.
         * Note that this call will trigger every constructor in the model
         * The players are always provided
         */
        this.match = new Match(players);


        /*
         * Assign the board controller
         * Keep in mind that match.board must be initialized at this time
         */
        this.boardController = new BoardController(this.match.getBoard());



        //Init anything else in the future here..

    }

    /**
     * The run method is the Runnable implementation of the match controller
     * Every match controller requires its own thread
     * This because it should be able to wait (literally) for the players to perform an action/choice
     */
    public void run() {

        while (true) {

            try {

                //This call will pause the thread until a new request will be put in the queue
                ActionRequest actionRequest = this.actionRequests.take();

                Logger.log(Level.SEVERE, "MatchController", "Parsing request");

            } catch (InterruptedException e) {

                Logger.log(Level.SEVERE, "MatchController", "Interrupted", e);

            }

        }

    }

    public void onPlayerAction(Player player, ActionRequest action) throws NotStrongEnoughException, FamilyMemberAlreadyInUseException, NotEnoughPlayersException, PlaceOccupiedException, NotEnoughResourcesException, NotEnoughPointsException, SixCardsLimitReachedException, PlayerAlreadyOccupiedTowerException {

        if(action instanceof FamilyMemberPlacementActionRequest){

            placeFamilyMember((FamilyMemberPlacementActionRequest) action,player);

        }

        if(action instanceof LeaderCardActivationActionRequest){

            activateLeaderCard((LeaderCardActivationActionRequest) action, player);

        }

        if(action instanceof RollDiceActionRequest){

            rollDices ();

        }

    }

    /**
     * This method is called from the lobby when a client leave/rejoin the match.
     * We need to disable/enable the player
     * If the value is true, the map entry gets removed, otherwise is added again
     * @param handler the player handler
     * @param value the value of the disabled state
     */
    public void disablePlayerRelativeTo(ClientHandler handler, boolean value) throws NoSuchPlayerException {

        Player target = this.match.getPlayerFromUsername(handler.getUsername());

        target.setDisabled(value);

        if (value) {

            this.remotePlayerMap.remove(target);

        }
        else {

            this.remotePlayerMap.put(target, handler);

        }

    }

    public LinkedHashMap<Player, RemotePlayer> getRemotePlayerMap() {
        return this.remotePlayerMap;
    }

    public Match getMatch() {
        return match;
    }

    /**
     * this method applies the cost of a card to a player
     * @param player
     * @param card
     * @throws NotEnoughResourcesException
     * @throws NotEnoughMilitaryPointsException
     */
    public void ApplyDvptCardCost(Player player, DvptCard card,CostOptionType costOptionType) throws NotEnoughResourcesException, NotEnoughMilitaryPointsException {

        //territory cards doesn't have cost
        if(card.getType() == DvptCardType.territory)
            return;

        int i=0;

        //some cards could have a double cost
        if(card.getCost().size()>1)
          i = costOptionType.toInt();

        //get the choosen one cost
        Cost costo = card.getCost().get(i);

        //try to apply military cost, if it does not succeed it returns an exception
        if(costo.getMilitary().getRequired() <= player.getMilitaryPoints())
            player.subtractMilitaryPoints(costo.getMilitary().getMalus());

        else{
            throw new NotEnoughMilitaryPointsException("Not enough military point to do this");}

        //check if there are enough resources to apply the cost in order to have an atomic transaction, if it does not succeed it returns an exception
        //deducts the cost of the card from the player's resources
        if(player.hasEnoughCostResources(costo)) {
            player.subtractResources(costo);
        }
        else
            throw new NotEnoughResourcesException("Not enough resources to do this");

    }

    /**
     * this method applies immediate effect of a card on the player who took it
     * @param player
     * @param card
     * @throws NotEnoughResourcesException
     * @throws NotEnoughPointsException
     */
    public void applyImmediateEffect(Player player, DvptCard card) throws NotEnoughResourcesException, NotEnoughPointsException {
        //TODO
        FamilyMemberPlacementActionRequest action;
        ImmediateEffect immediateEffect = card.getImmediateEffect();


        applyEffectSurplus(player,immediateEffect.getSurplus());


        if(immediateEffect.getEffectAction().getTarget() == ActionType.harvest)
            applyHarvestChain(player,immediateEffect.getEffectAction().getForce());

        if(immediateEffect.getEffectAction().getTarget() == ActionType.production)
            applyProductionChain(player,immediateEffect.getEffectAction().getForce());

        if(immediateEffect.getEffectAction().getTarget() == ActionType.card){
            if(immediateEffect.getEffectAction().getType() == DvptCardType.territory)
                //manda al client quale azione può essere fatta -----> BoardSectorType + Force + Discount
                ;

            if(immediateEffect.getEffectAction().getType() == DvptCardType.character)
                //manda al client quale azione può essere fatta -----> BoardSectorType + Force + Discount
                ;

            if(immediateEffect.getEffectAction().getType() == DvptCardType.building)
                //manda al client quale azione può essere fatta -----> BoardSectorType + Force + Discount
                ;

            if(immediateEffect.getEffectAction().getType() == DvptCardType.venture)
                //manda al client quale azione può essere fatta -----> BoardSectorType + Force + Discount
                ;}

                //TODO multiplier
    }

    /**
     * this method receives an action and its author and places the familiar in the correct place (if it is strong enough)
     * @param action
     * @param player
     * @throws NotStrongEnoughException
     */
    public void placeFamilyMember(FamilyMemberPlacementActionRequest action, Player player) throws NotStrongEnoughException, FamilyMemberAlreadyInUseException, NotEnoughPlayersException, PlaceOccupiedException, NotEnoughResourcesException, NotEnoughPointsException, SixCardsLimitReachedException, PlayerAlreadyOccupiedTowerException {

        FamilyMember familyMember = player.getFamilyMember(action.getColorType());

        //apply character cards permanent effect
        action = actionCharacterFilter(action,player);

        //if boardSectorType is CouncilPalace we place the family member in the council palace
        //once positioned the council palace give to the player an effectSurplus
        if (action.getActionTarget() == BoardSectorType.CouncilPalace) {
            EffectSurplus surplus = boardController.placeOnCouncilPalace(familyMember, action.getAdditionalServants(),this.match.getPlayers().size());
            applyEffectSurplus(player,surplus);
            }

        //if boardSectorType is Market we place the family member in the correct market place (from index 0 to index 3)
        //once positioned the market place give to the player an effectSurplus
        if (action.getActionTarget() == BoardSectorType.Market) {

            EffectSurplus surplus = boardController.placeOnMarket(familyMember,action.getPlacementIndex(),action.getAdditionalServants(),this.match.getPlayers().size());
            applyEffectSurplus(player,surplus);

        }

        //if boardSectorType is SingleHarvestPlace we place the family member in the single harvest place of the harvest area
        //once positioned the single harvest place give to the player an effectSurplus
        //the harvestChain (activation of all the permanent effect of territory cards that has harvest type) is also started
        if(action.getActionTarget() == BoardSectorType.SingleHarvestPlace) {

            EffectSurplus surplus = boardController.placeOnSingleHarvestPlace(familyMember,action.getAdditionalServants(),this.match.getPlayers().size());
            applyEffectSurplus(player,surplus);
            applyHarvestChain(player,familyMember.getForce() + action.getAdditionalServants());
        }

        //if boardSectorType is CompositeHarvestPlace we place the family member in the composite harvest place of the harvest area
        //once positioned the composite harvest place give to the player an effectSurplus
        //the harvestChain (activation of all the permanent effect of territory cards that has harvest type) is also started with a malus on the activation force
        if(action.getActionTarget() == BoardSectorType.CompositeHarvestPlace) {

            EffectSurplus surplus = boardController.placeOnCompositeHarvestPlace(familyMember,action.getAdditionalServants(),this.match.getPlayers().size());
            applyEffectSurplus(player,surplus);

            //we have to subtract force malus from activation force
            applyHarvestChain(player,familyMember.getForce() + action.getAdditionalServants() - this.match.getBoard().getHarvestArea().getSecondaryPlace().getForceMalus());
        }

        //if boardSectorType is SingleProductionPlace we place the family member in the single production place of the production area
        //once positioned the single production place give to the player an effectSurplus
        //the productionChain (activation of all the permanent effect of building cards that has production type) is also started
        if(action.getActionTarget() == BoardSectorType.SingleProductionPlace) {

            EffectSurplus surplus = boardController.placeOnSingleProductionPlace(familyMember,action.getAdditionalServants(),this.match.getPlayers().size());
            applyEffectSurplus(player,surplus);
            applyProductionChain(player,familyMember.getForce() + action.getAdditionalServants());

        }

        //if boardSectorType is CompositeProductionPlace we place the family member in the composite production place of the production area
        //once positioned the composite production place give to the player an effectSurplus
        //the productionChain (activation of all the permanent effect of building cards that has production type) is also started with a malus on the activation force
        if(action.getActionTarget() == BoardSectorType.CompositeProductionPlace) {

            EffectSurplus surplus = boardController.placeOnCompositeProductionPlace(familyMember,action.getAdditionalServants(),this.match.getPlayers().size());
            applyEffectSurplus(player,surplus);

            //we have to subtract a force malus from activation force
            applyProductionChain(player,familyMember.getForce() + action.getAdditionalServants() - this.match.getBoard().getProductionArea().getSecondaryPlace().getForceMalus());

        }

        //if boardSectorType is TerritoryTower we place the family member in the correct (placementIndex) towerSlot of the tower
        //once positioned the towerSlot give to the player an effectSurplus
        if(action.getActionTarget() == BoardSectorType.TerritoryTower) {

            //control if the player has another family member in the tower
            if(this.match.getBoard().getTerritoryTowerPlayers().contains(player))
                throw new PlayerAlreadyOccupiedTowerException("the player already has a family member in this tower");

            //if the tower is already occupied tha player have to pay 3 coins
            if(this.match.getBoard().getTerritoryTowerPlayers().size()>0){
                player.subtractCoins(3);}

            //try to apply card cost to the player that made the action .. if this method return an exception no family members will be set here
            ApplyDvptCardCost(player,this.match.getBoard().getTerritoryTower().get(action.getPlacementIndex()).getDvptCard(),action.getCostOptionType());

            EffectSurplus surplus = boardController.placeOnTower(familyMember, action.getAdditionalServants(), this.match.getPlayers().size(),DvptCardType.territory, action.getPlacementIndex());
            applyEffectSurplus(player,surplus);

            //add to the personal board of the player the territory card set in the tower slot
            player.getPersonalBoard().addTerritoryCard((TerritoryDvptCard) this.match.getBoard().getTerritoryTower().get(action.getPlacementIndex()).getDvptCard());

            applyImmediateEffect(player,this.match.getBoard().getTerritoryTower().get(action.getPlacementIndex()).getDvptCard());

            //set the dvptCard of the tower to null value because no one can choose or take it now
            this.match.getBoard().getTerritoryTower().get(action.getPlacementIndex()).setDvptCard(null);

        }

        //if boardSectorType is BuildingTower we place the family member in the correct (placementIndex) towerSlot of the tower
        //once positioned the towerSlot give to the player an effectSurplus
        if(action.getActionTarget() == BoardSectorType.BuildingTower) {

            //control if the player has another family member in the tower
            if(this.match.getBoard().getBuildingTowerPlayers().contains(player))
                throw new PlayerAlreadyOccupiedTowerException("the player already has a family member in this tower");

            //if the tower is already occupied tha player have to pay 3 coins
            if(this.match.getBoard().getBuildingTowerPlayers().size()>0)
                player.subtractCoins(3);

            //try to apply card cost to the player that made the action .. if this method return an exception no family members will be set here
            ApplyDvptCardCost(player,this.match.getBoard().getBuildingTower().get(action.getPlacementIndex()).getDvptCard(),action.getCostOptionType());

            EffectSurplus surplus = boardController.placeOnTower(familyMember, action.getAdditionalServants(), this.match.getPlayers().size(),DvptCardType.building, action.getPlacementIndex());
            applyEffectSurplus(player,surplus);

            //add to the personal board of the player the building card set in the tower slot
            player.getPersonalBoard().addBuildingCard((BuildingDvptCard) this.match.getBoard().getBuildingTower().get(action.getPlacementIndex()).getDvptCard());

            applyImmediateEffect(player,this.match.getBoard().getBuildingTower().get(action.getPlacementIndex()).getDvptCard());

            //set the dvptCard of the tower to null value because no one can choose or take it now
            this.match.getBoard().getBuildingTower().get(action.getPlacementIndex()).setDvptCard(null);

        }

        //if boardSectorType is CharacterTower we place the family member in the correct (placementIndex) towerSlot of the tower
        //once positioned the towerSlot give to the player an effectSurplus
        if(action.getActionTarget() == BoardSectorType.CharacterTower) {

            //control if the player has another family member in the tower
            if(this.match.getBoard().getCharacterTowerPlayers().contains(player))
                throw new PlayerAlreadyOccupiedTowerException("the player already has a family member in this tower");

            //if the tower is already occupied tha player have to pay 3 coins
            if(this.match.getBoard().getCharacterTowerPlayers().size()>0)
                player.subtractCoins(3);

            //try to apply card cost to the player that made the action .. if this method return an exception no family members will be set here
            ApplyDvptCardCost(player,this.match.getBoard().getCharacterTower().get(action.getPlacementIndex()).getDvptCard(),action.getCostOptionType());

            EffectSurplus surplus = boardController.placeOnTower(familyMember, action.getAdditionalServants(), this.match.getPlayers().size(),DvptCardType.character, action.getPlacementIndex());
            applyEffectSurplus(player,surplus);

            //add to the personal board of the player the building card set in the tower slot
            player.getPersonalBoard().addCharacterCard((CharacterDvptCard) this.match.getBoard().getCharacterTower().get(action.getPlacementIndex()).getDvptCard());

            applyImmediateEffect(player,this.match.getBoard().getCharacterTower().get(action.getPlacementIndex()).getDvptCard());

            //set the dvptCard of the tower to null value because no one can choose or take it now
            this.match.getBoard().getCharacterTower().get(action.getPlacementIndex()).setDvptCard(null);

        }

        //if boardSectorType is VentureTower we place the family member in the correct (placementIndex) towerSlot of the tower
        //once positioned the towerSlot give to the player an effectSurplus
        if(action.getActionTarget() == BoardSectorType.VentureTower) {

            //control if the player has another family member in the tower
            if(this.match.getBoard().getVentureTowerPlayers().contains(player))
                throw new PlayerAlreadyOccupiedTowerException("the player already has a family member in this tower");

            //if the tower is already occupied tha player have to pay 3 coins
            if(this.match.getBoard().getVentureTowerPlayers().size()>0)
                player.subtractCoins(3);

            //try to apply card cost to the player that made the action .. if this method return an exception no family members will be set here
            ApplyDvptCardCost(player,this.match.getBoard().getVentureTower().get(action.getPlacementIndex()).getDvptCard(),action.getCostOptionType());

            EffectSurplus surplus = boardController.placeOnTower(familyMember, action.getAdditionalServants(), this.match.getPlayers().size(),DvptCardType.venture, action.getPlacementIndex());
            applyEffectSurplus(player,surplus);

            //add to the personal board of the player the building card set in the tower slot
            player.getPersonalBoard().addVentureCard((VentureDvptCard) this.match.getBoard().getVentureTower().get(action.getPlacementIndex()).getDvptCard());

            applyImmediateEffect(player,this.match.getBoard().getVentureTower().get(action.getPlacementIndex()).getDvptCard());

            //set the dvptCard of the tower to null value because no one can choose or take it now
            this.match.getBoard().getVentureTower().get(action.getPlacementIndex()).setDvptCard(null);

        }

        //set the familiar busy
        familyMember.setBusy(true);
    }

    /**
     * this method apply the effectSurplus to a player
     * @param player
     * @param surplus
     */

    public void applyEffectSurplus(Player player,EffectSurplus surplus){

        //effect surplus is composed by resources,points and council privilege
        ArrayList<Resource> resourcesSurplus = surplus.getResources();
        ArrayList<Point> pointsSurplus = surplus.getPoints();
        Integer council = surplus.getCouncil();

        player.addResources(resourcesSurplus);
        player.addPoints(pointsSurplus);

        //the client can choose which council privilege want to have
        if(council >= 1)
            //TODO
            ;
    }

    /**
     * this method starts the harvest chain.
     * this harvest chain consists in the activation of all the territory cards permament effect
     * @param player
     * @param force
     */
    public void applyHarvestChain(Player player, Integer force){

        for (TerritoryDvptCard card:player.getPersonalBoard().getTerritoryCards()) {

            //apply territory card permanent effect only if the player has enough force
            if(card.getPermanentEffect().getMinForce() <= force){

                applyEffectSurplus(player,card.getPermanentEffect().getSurplus());

            }
        }
        //apply effect surplus of the personal bonus tile of the player
        if(player.getPersonalBoard().getBonusTile().getHarvestMinForce() <= force)
            applyEffectSurplus(player,player.getPersonalBoard().getBonusTile().getHarvestSurplus());
    }


    public void activateLeaderCard (LeaderCardActivationActionRequest action, Player player) {

        if(player.hasEnoughLeaderRequirements(action.getLeaderCardIndex())){ //verify the requirements to activate Leader Card
        }

    }

    /** this method applies the Production Chain
     * this character chain consists in the activation of all the building card permanent effect**/

    public void applyProductionChain (Player player, Integer force) throws NotEnoughPointsException,NotEnoughResourcesException {

        for (DvptCard card : player.getPersonalBoard().getBuildingCards()
                ) {

            if (force >= card.getPermanentEffect().getMinForce()) {

                applyBuildingPermanentEffect(card, player, 0);

            }

        }

        //apply effect surplus of the personal bonus tile of the player
        if(player.getPersonalBoard().getBonusTile().getProductionMinForce() <= force)
            applyEffectSurplus(player,player.getPersonalBoard().getBonusTile().getProductionSurplus());
    }

    /** this method applies the PermanentEffect of a Building card, that could be a surplus or a conversion
     *
     * */

    public void applyBuildingPermanentEffect (DvptCard card, Player player, Integer choice) throws NotEnoughResourcesException, NotEnoughPointsException{

        if(card.getPermanentEffect().getSurplus() != null)

            applyEffectSurplus(player, card.getPermanentEffect().getSurplus());

        if(!card.getPermanentEffect().getConversion().isEmpty())

            applyConversion(player, card.getPermanentEffect().getConversion() , choice);

    }

    /** this method applies a conversion permanent effect of a development card to a particular player
     *
     * @param player
     * @param conversionList the list of conversion contained in the card effect
     * @param choice the choice of different conversion which can be made by the player
     * @throws NotEnoughResourcesException
     * @throws NotEnoughPointsException
     */


    public void applyConversion (Player player, ArrayList<EffectConversion> conversionList, Integer choice) throws NotEnoughResourcesException, NotEnoughPointsException {

        if(!conversionList.get(choice).getFrom().getResources().isEmpty()) {

            for(Resource from: conversionList.get(choice).getFrom().getResources())

                player.subtractGenericResource(from.getType(), from.getAmount());

        }

        if(!conversionList.get(choice).getFrom().getPoints().isEmpty()) {

            for(Point from: conversionList.get(choice).getFrom().getPoints())

                player.subtractGenericPoint(from.getType(), from.getAmount());

        }

        if(!conversionList.get(choice).getTo().getResources().isEmpty()) {

            for(Resource to: conversionList.get(choice).getTo().getResources())

                player.addGenericResource(to.getType(), to.getAmount());

        }

        if(!conversionList.get(choice).getTo().getPoints().isEmpty()) {

            for(Point to: conversionList.get(choice).getTo().getPoints())

                player.addGenericPoint(to.getType(), to.getAmount());

        }

    }

    /** this method rolls dices and set them on the board */

    public void rollDices (){

        Random random = new Random();

        for (Dice d : this.match.getBoard().getDices()) {

            d.setValue(random.nextInt(5) + 1);

        }

    }

    /**
     * this method calculate the final score of the players
     */
    public LinkedHashMap<Player,Integer> calculatesFinalScore(){

        LinkedHashMap<Player,Integer> finalScore = new LinkedHashMap<Player, Integer>();

        for (Player player : this.match.getPlayers()) {

            Integer totalScore = 0;

            totalScore += player.getVictoryPoints();

            //each venture card give a victory bonus
            for (VentureDvptCard card: player.getPersonalBoard().getVentureCards()) {
                    totalScore += card.getPermanentEffect().getvPoints();
            }

            //one victory point from every 5 resources of all type
            totalScore += (player.getCoins() + player.getStones() + player.getWood() + player.getServants()) / 5;

            //victory points that depends on building card on the player personal board
            totalScore += BoardConfigParser.getVictoryBonus(DvptCardType.building,player.getPersonalBoard().getBuildingCards().size());

            //victory points that depends on character card on the player personal board
            totalScore += BoardConfigParser.getVictoryBonus(DvptCardType.character,player.getPersonalBoard().getCharacterCards().size());

            //victory points that depends on faith points
            totalScore += BoardConfigParser.getVictoryBonusFromFaith(player.getFaithPoints());

            //victory points that depends on military points
            totalScore += getMilitaryPointsBonus(player);

            finalScore.put(player,totalScore);
        }

        return  finalScore;
    }

    /**
     * this method create the military points ranking and return the victory points for each player
     */
    public Integer getMilitaryPointsBonus(Player player){

        Integer position = 1;

        for (Player player1: this.match.getPlayers()) {

            if(player.getMilitaryPoints() < player1.getMilitaryPoints())
                position++;
        }

        return getVictoryBonusFromRanking(position);
    }

    /**
     * this method apply permanent effect of character cards when a player try to place his family member
     * @param action
     * @param player
     * @return
     */
    public FamilyMemberPlacementActionRequest actionCharacterFilter(FamilyMemberPlacementActionRequest action, Player player){

        for (CharacterDvptCard card: player.getPersonalBoard().getCharacterCards()) {

            EffectPermanentAction permanentEffectAction = card.getPermanentEffect().getAction();

            if(permanentEffectAction.getTarget() == ActionType.harvest) {

                if (action.getActionTarget() == BoardSectorType.CompositeHarvestPlace || action.getActionTarget() == BoardSectorType.SingleHarvestPlace) {
                    action.increaseBonus(permanentEffectAction.getForceBonus());
                }
            }

            if(permanentEffectAction.getTarget() == ActionType.production) {

                if (action.getActionTarget() == BoardSectorType.CompositeProductionPlace || action.getActionTarget() == BoardSectorType.SingleProductionPlace) {
                    action.increaseBonus(permanentEffectAction.getForceBonus());
                }
            }

            if(permanentEffectAction.getTarget() == ActionType.card){
                System.out.println(permanentEffectAction.getType());
                if(permanentEffectAction.getType() == DvptCardType.territory)
                    action.increaseBonus(permanentEffectAction.getForceBonus());

                if(permanentEffectAction.getType() == DvptCardType.building)
                    action.increaseBonus(permanentEffectAction.getForceBonus());

                if(permanentEffectAction.getType() == DvptCardType.character)
                    action.increaseBonus(permanentEffectAction.getForceBonus());

                if(permanentEffectAction.getType() == DvptCardType.venture)
                    action.increaseBonus(permanentEffectAction.getForceBonus());

            }
            //TODO effect discount
        }

        return  action;

    }

}
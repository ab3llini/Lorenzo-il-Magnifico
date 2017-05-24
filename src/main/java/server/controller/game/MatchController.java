package server.controller.game;
import exception.*;
import netobject.*;
import netobject.request.action.*;
import server.model.*;
import server.model.board.*;
import server.model.card.developement.Cost;
import server.model.card.developement.DvptCard;
import server.model.card.developement.DvptCardType;
import server.model.effect.EffectSurplus;
import server.model.valuable.Point;
import server.model.valuable.Resource;

import java.util.ArrayList;

import static java.util.Collections.shuffle;

/**
 * Created by Alberto on 19/05/2017.
 */

/**
 * The controller of the match.
 * Will handle the model instance reacting to game events.
 */
public class MatchController {

    /**
     * The model instance of the match
     */
    private Match match;

    /**
     * The instance of the board controller
     */
    private BoardController boardController;

    /**
     * This is the match controller constructor.
     * It is called only by the lobby itself when the match starts
     * @param players the players in the match.
     */
    public MatchController(ArrayList<Player> players) {

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

    public void onPlayerAction(Player player, ActionRequest action) throws NotStrongEnoughException, FamilyMemberAlreadyInUseException, NotEnoughPlayersException {

        if(action instanceof FamilyMemberPlacementActionRequest){

            placeFamilyMember((FamilyMemberPlacementActionRequest) action,player);

        }

        if(action instanceof LeaderCardActivationActionRequest){

            activateLeaderCard((LeaderCardActivationActionRequest) action, player);

        }

        if(action instanceof RollDiceActionRequest){

            rollDice ((player));

        }

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
    public void ApplyDvptCardCost(Player player, DvptCard card,Integer choosenCost) throws NotEnoughResourcesException, NotEnoughMilitaryPointsException {

        //territory cards doesn't have cost
        if(card.getType() == DvptCardType.territory)
            return;

        int i=0;
        System.out.println(card.getCost().size());
        //some cards could have a double cost
        if(card.getCost().size()>1)
          i = choosenCost;

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
     * this method receive an action and its author and place the familiar in the correct place (if it is strong enough)
     * @param action
     * @param player
     * @throws NotStrongEnoughException
     */
    public void placeFamilyMember(FamilyMemberPlacementActionRequest action, Player player) throws NotStrongEnoughException, FamilyMemberAlreadyInUseException, NotEnoughPlayersException {

        FamilyMember familyMember = player.getFamilyMember(action.getColorType());


        //TODO
        if (action.getActionTarget() == BoardSectorType.CouncilPalace) {
            EffectSurplus surplus = boardController.placeOnCouncilPalace(familyMember, action.getAdditionalServants(),this.match.getPlayers().size());
            applyEffectSurplus(player,surplus);
            }

        if (action.getActionTarget() == BoardSectorType.Market) {
            EffectSurplus surplus =boardController.placeOnMarket(familyMember,action.getPlacementIndex(),action.getAdditionalServants(),this.match.getPlayers().size());
            applyEffectSurplus(player,surplus);
        }

        familyMember.setBusy(true);
    }

    /**
     * this method apply the effectSurplus to a player
     * @param player
     * @param surplus
     */

    public void applyEffectSurplus(Player player,EffectSurplus surplus){

        ArrayList<Resource> resourcesSurplus = surplus.getResources();
        ArrayList<Point> pointsSurplus = surplus.getPoints();
        Integer council = surplus.getCouncil();

        player.addResources(resourcesSurplus);
        player.addPoints(pointsSurplus);

        if(council >= 1)
            //TODO
            ;
    }

    public void activateLeaderCard (LeaderCardActivationActionRequest action, Player player) {

        player.hasEnoughLeaderRequirements(action.getLeaderCardIndex()); //verify the requirements to activate Leader Card

    }

    public void rollDice (Player player){

    }
}
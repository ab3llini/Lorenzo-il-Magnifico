package server.controller.game;

import server.model.GameSingleton;
import server.model.board.Board;
import server.model.card.Deck;
import server.model.card.developement.DvptCard;
import server.utility.DvptCardParser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;


/*
 * @author  ab3llini
 * @since   19/05/17.
 */

public class BoardController {

    /**
     * Each development card has an offset between periods of 8
     */
    private static final int DVPT_CARD_OFFSET = 8;

    /**
     * There are 3 total periods
     */
    private static final int TOTAL_PERIODS = 3;

    /**
     * There are 4 total towers
     */
    private static final int TOTAL_TOWERS = 4;

    /**
     * There are 4 total slots for each tower
     */
    private static final int SLOTS_FOR_TOWER = 4;

    /**
     * Territory, building, character and venture tower are placed in a specific order on board; decks' numeration follows this order
     */
    private static final int TERRITORY_TOWER_INDEX = 0;
    private static final int BUILDING_TOWER_INDEX = 1;
    private static final int CHARACTER_TOWER_INDEX = 2;
    private static final int VENTURE_TOWER_INDEX = 3;

    /**
     * Constructor. The board controller takes care of every update relative to the board
     */
    public BoardController(Board board) {

        //TODO: Load into the board model the proper values! Even throughout a constructor chain!

    }

    /**
     * A method that creates the card decks
     *
     * @return the array list
     */
    public ArrayList<Deck<DvptCard>> createDecks() {

        GameSingleton singleton = GameSingleton.getInstance();

        ArrayList<Deck<DvptCard>> deckArray = new ArrayList<Deck<DvptCard>>();

        for (int deckIndex = 0; deckIndex < TOTAL_PERIODS * TOTAL_TOWERS; deckIndex++) {

            Deck<DvptCard> deck = new Deck<DvptCard>();

            for (int cardIndex = deckIndex * DVPT_CARD_OFFSET; cardIndex < deckIndex * DVPT_CARD_OFFSET + DVPT_CARD_OFFSET; cardIndex++) {

                deck.addCard(singleton.getSpecificDvptCard(cardIndex));

            }

            deckArray.add(deck.shuffle());

        }
        return deckArray;

    }

    public void prepareTowers(Board board, Integer round, Integer period, ArrayList<Deck<DvptCard>> deckArray) {

        ArrayList<DvptCard> temporaryTerritory = new ArrayList<DvptCard>();

        ArrayList<DvptCard> temporaryBuilding = new ArrayList<DvptCard>();

        ArrayList<DvptCard> temporaryCharacter = new ArrayList<DvptCard>();

        ArrayList<DvptCard> temporaryVenture = new ArrayList<DvptCard>();

        /**
         * If it is the first turn of a period, every tower will contain the first half of his specific deck, according to his type and period
         */

        if(round % 2 == 1) {

            for(int i=0; i<4; i++) {
                temporaryTerritory.add(deckArray.get(TERRITORY_TOWER_INDEX * TOTAL_PERIODS + (period-1) ).getCards().get(i));
                System.out.println("Sto caricando la carta" + deckArray.get(TERRITORY_TOWER_INDEX * TOTAL_PERIODS).getCards().get(i).getId());
            }

            for(int i = 0; i<4; i++){
                temporaryBuilding.add(deckArray.get(BUILDING_TOWER_INDEX * TOTAL_PERIODS + (period-1) ).getCards().get(i));
            }


            for(int i = 0; i<4; i++){
                temporaryCharacter.add(deckArray.get(CHARACTER_TOWER_INDEX * TOTAL_PERIODS + (period-1) ).getCards().get(i));
            }


            for(int i = 0; i<4; i++){
                temporaryVenture.add(deckArray.get(VENTURE_TOWER_INDEX * TOTAL_PERIODS + (period-1) ).getCards().get(i));
            }
        }
        /**
         * On the contrary, if it is the second round of that period, every tower will contain the second half of his specific deck, according to his type and period
         */

        else {

            for(int i=4; i<8; i++){

                temporaryTerritory.add(deckArray.get(TERRITORY_TOWER_INDEX * TOTAL_PERIODS + (period-1)).getCards().get(i));

            }

            for(int i=4; i<8; i++){

                temporaryBuilding.add(deckArray.get(BUILDING_TOWER_INDEX * TOTAL_PERIODS + (period-1)).getCards().get(i));

            }


            for(int i=4; i<8; i++){

                temporaryCharacter.add(deckArray.get(CHARACTER_TOWER_INDEX * TOTAL_PERIODS + (period-1)).getCards().get(i));

            }


            for(int i=4; i<8; i++){

                temporaryVenture.add(deckArray.get(VENTURE_TOWER_INDEX * TOTAL_PERIODS + (period-1)).getCards().get(i));

            }

        }

        board.setDvptCardOnTerritoryTower(temporaryTerritory);

        board.setDvptCardOnBuildingTower(temporaryBuilding);

        board.setDvptCardOnCharacterTower(temporaryCharacter);

        board.setDvptCardOnVentureTower(temporaryVenture);

    }
}

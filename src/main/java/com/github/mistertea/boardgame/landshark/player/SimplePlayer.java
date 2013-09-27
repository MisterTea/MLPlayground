package com.github.mistertea.boardgame.landshark.player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.github.mistertea.boardgame.core.ThriftB64Utils;
import com.github.mistertea.boardgame.landshark.Board;
import com.github.mistertea.boardgame.landshark.LandsharkCommand;
import com.github.mistertea.boardgame.landshark.LandsharkCommandType;
import com.github.mistertea.boardgame.landshark.PlayerState;
import com.github.mistertea.boardgame.landshark.Property;
import com.github.mistertea.boardgame.landshark.State;
import com.github.mistertea.boardgame.landshark.StateQuery;

public class SimplePlayer extends AbstractPlayer {
	protected Random rng;

	public SimplePlayer(String name) {
		super(name);
		rng = new Random(name.hashCode());
	}

	@Override
	public String fetchCommand(StateQuery query, State state) {
		Board board = query.getBoard();
		PlayerState myState = query.getPlayerState(state, name);

		switch (state.turnState) {
		case ASK_AUCTION:
			return ThriftB64Utils.ThriftToString(new LandsharkCommand()
					.setPlayer(name)
					.setType(LandsharkCommandType.CHOOSE_AUCTION.getValue())
					.setProperty(
							board.properties.get(board.propertyOrder
									.get(myState.location)).name));
		case ASK_ANY_AUCTION: {
			Set<String> propertySet = query.getPlayerState(state, name).properties;
			int i = rng.nextInt(propertySet.size());
			String property = (String) propertySet.toArray()[i];
			return ThriftB64Utils.ThriftToString(new LandsharkCommand()
					.setPlayer(name)
					.setType(LandsharkCommandType.CHOOSE_AUCTION.getValue())
					.setProperty(property));
		}
		case AUCTION: {
			int bid = query.estimateFutureRent(state,
					board.properties.get(state.auctionState.property), name)
					+ rng.nextInt(50);
			int maximumBidGivenRisk = Math.max(
					0,
					myState.cash
							- query.estimateRentBeforeStart(state, name, 0.95f,
									100));
			bid = Math.min(bid, maximumBidGivenRisk);
			return ThriftB64Utils.ThriftToString(new LandsharkCommand()
					.setPlayer(name)
					.setType(LandsharkCommandType.BID.getValue()).setBid(bid));
		}
		case BUYING_HOUSES: {
			Map<String, Integer> housePurchases = new HashMap<>();
			if (query.canBuildHouses(state)) {
				int maximumRisk = Math.max(
						0,
						myState.cash
								- query.estimateRentBeforeStart(state, name,
										0.95f, 100));
				int totalCost = 0;
				for (String propertyName : myState.properties) {
					Property property = board.properties.get(propertyName);
					// Loop through properties finding ones where you can put
					// houses,
					// try to diversify
					if (query.canPutHouses(state, name, propertyName, 1)) {
						if (board.propertyGroups.get(property.group).housePrice
								+ totalCost <= maximumRisk) {
							housePurchases.put(propertyName, 1);
							totalCost += board.propertyGroups
									.get(property.group).housePrice;
						}
					}
				}
			}
			return ThriftB64Utils.ThriftToString(new LandsharkCommand()
					.setPlayer(name)
					.setType(LandsharkCommandType.BUY_HOUSES.getValue())
					.setHousePurchases(housePurchases));
		}
		case FORCE_AUCTION:
			return ThriftB64Utils.ThriftToString(new LandsharkCommand()
					.setPlayer(name)
					.setType(LandsharkCommandType.CHOOSE_AUCTION.getValue())
					.setProperty(
							query.getPlayerState(state, name).properties
									.iterator().next()));
		default:
			throw new RuntimeException("OOPS");
		}
	}

}

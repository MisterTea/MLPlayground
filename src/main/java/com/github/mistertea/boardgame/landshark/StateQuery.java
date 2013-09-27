package com.github.mistertea.boardgame.landshark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Random;

import org.junit.Assert;

public class StateQuery {
	Board board;

	public StateQuery(Board board) {
		this.board = board;
	}

	public boolean canPutHouses(State currentState, String player,
			String property, int houses) {
		if (board.properties.get(property).type != PropertyType.STREET) {
			return false;
		}
		Integer pastHouses = currentState.houses.get(property);
		if (pastHouses == null) {
			if (houses > 5) {
				return false;
			}
			if (!playerOwnsGroup(currentState, player,
					board.properties.get(property).group)) {
				return false;
			}
			return canAffordHouses(currentState, player, property, houses);
		} else {
			// We already have houses, just make sure this doesn't go beyond
			// hotels.
			return (pastHouses + houses) <= 5
					&& canAffordHouses(currentState, player, property, houses);
		}
	}

	public boolean canAffordHouses(State currentState, String player,
			String property, int houses) {
		Assert.assertEquals(board.properties.get(property).type,
				PropertyType.STREET);
		return getPlayerState(currentState, player).cash >= getHousePrice(
				currentState, property) * houses;
	}

	public int getHousePrice(State currentState, String property) {
		return getGroupForProperty(board.properties.get(property)).housePrice;
	}

	private PropertyGroup getGroupForProperty(Property property) {
		return board.propertyGroups.get(property.group);
	}

	public int getRent(State currentState, Property property) {
		String owner = getOwner(currentState, property);
		Assert.assertNotNull(owner);

		switch (property.type) {
		case STREET:
			if (currentState.houses.containsKey(property.name)) {
				return getRentForStreet(property, true,
						currentState.houses.get(property.name));
			} else {
				return getRentForStreet(property,
						playerOwnsGroup(currentState, owner, property.group), 0);
			}
		case UTILITY: {
			String group = property.group;
			int count = 0;
			for (Property p : board.properties.values()) {
				if (p.group != null && p.group.equals(group)) {
					for (PlayerState playerState : currentState.playerStates) {
						if (playerState.properties.contains(p.name)
								&& playerState.name.equals(owner)) {
							count++;
						}
					}
				}
			}
			int baseRent = 25;
			for (int a = 1; a < count; a++) {
				baseRent *= 2;
			}
			return baseRent;
		}
		default:
			throw new RuntimeException("OOPS");
		}
	}

	public int getRentForStreet(Property property, boolean ownsGroup, int houses) {
		if (houses > 0) {
			return property.rent.get(houses);
		} else {
			if (ownsGroup) {
				return property.rent.get(0) * 2;
			}
			return property.rent.get(0);
		}
	}

	public boolean playerOwnsGroup(State currentState, String player,
			String group) {
		PropertyGroup street = board.propertyGroups.get(group);
		Assert.assertNotNull("Street for group " + group + " is null", street);
		if (getPlayerState(currentState, player).properties
				.containsAll(street.memberNames)) {
			return true;
		}
		return false;
	}

	public String getOwner(State currentState, Property property) {
		for (PlayerState playerState : currentState.playerStates) {
			if (playerState.properties.contains(property.name)) {
				return playerState.name;
			}
		}
		return null;
	}

	public boolean canBuildHouses(State currentState) {
		PlayerState currentPlayerState = getCurrentPlayerState(currentState);
		if (!isActivePlayer(currentPlayerState)) {
			return false;
		}

		for (Entry<String, PropertyGroup> entry : board.propertyGroups
				.entrySet()) {
			if (entry.getValue().type != PropertyType.STREET) {
				continue;
			}
			if (currentPlayerState.properties
					.containsAll(entry.getValue().memberNames)) {
				// Make sure they aren't all hotels
				for (String property : entry.getValue().memberNames) {
					if (!currentState.houses.containsKey(property)
							|| currentState.houses.get(property) < 5) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public int getActivePlayers(State currentState) {
		int count = 0;
		for (PlayerState playerState : currentState.playerStates) {
			if (isActivePlayer(playerState)) {
				count++;
			}
		}
		return count;
	}

	public boolean isActivePlayer(PlayerState playerState) {
		return playerState.cash >= 0 && !playerState.quit;
	}

	public PlayerState getCurrentPlayerState(State currentState) {
		return currentState.playerStates.get(currentState.playerTurn);
	}

	public String getCurrentPlayer(State currentState) {
		return getCurrentPlayerState(currentState).name;
	}

	public PlayerState getPlayerState(State currentState, String name) {
		for (PlayerState playerState : currentState.playerStates) {
			if (playerState.name.equals(name)) {
				return playerState;
			}
		}
		throw new RuntimeException("OOPS");
	}

	/**
	 * Crude estimate of the amount of rent that will have to be paid before
	 * reaching the end of the board.
	 * 
	 * @param currentState
	 * @param player
	 * @return
	 */
	public int estimateRentBeforeStart(State currentState, String playerName,
			float confidence, int moneyToSaveForBidding) {
		PlayerState playerState = getPlayerState(currentState, playerName);

		ArrayList<Integer> sums = new ArrayList<Integer>();
		final int NUM_SIMULATIONS = 100000;
		sums.ensureCapacity(NUM_SIMULATIONS);
		Random rng = new Random(playerName.hashCode());
		for (int a = 0; a < NUM_SIMULATIONS; a++) {
			int sum = 0;
			int curpos = playerState.location;
			while (true) {
				// Roll a 2d6.
				curpos += rng.nextInt(6) + rng.nextInt(6) + 2;
				if (curpos >= board.properties.size()) {
					// Passed start
					break;
				}
				Property property = board.properties.get(board.propertyOrder
						.get(curpos));
				String owner = getOwner(currentState, property);
				if (owner != null && !owner.equals(playerState.name)) {
					// rent is due
					sum += getRent(currentState, property);
				} else if (owner == null) {
					// Fudge factor: Save at least 100 for bidding on this property
					sum += moneyToSaveForBidding;
				}
			}
			sums.add(sum);
		}

		// Put the sums in order from highest to lowest
		Collections.sort(sums);
		Collections.reverse(sums);

		int stopIndex = Math.min(NUM_SIMULATIONS - 1,
				(int) ((1.0f - confidence) * NUM_SIMULATIONS));
		return sums.get(stopIndex);
	}

	public int estimateFutureRent(State currentState, Property newProperty,
			String newOwner) {
		State tmpState = currentState.deepCopy();
		getPlayerState(tmpState, newOwner).properties.add(newProperty.name);

		// TODO: This is the mean of the binomial distribution where the number
		// of chances is
		// equal to the probability of landing (i.e. the number of
		// turns is equal to the number of spaces on the board, and every space
		// has an equal chance of being landed on.). Note that you "gain" rent
		// by landing on your own square, to account for the opportunity cost.
		return getRent(tmpState, newProperty) * getActivePlayers(currentState);
	}

	public int countUnownedProperties(State inputState) {
		int count = 0;
		for (Property property : board.properties.values()) {
			if (getOwner(inputState, property) == null) {
				count++;
			}
		}
		return count;
	}

	public Board getBoard() {
		return board;
	}

	public int countOwnedInGroup(State currentState, String group, String owner) {
		PlayerState ownerState = getPlayerState(currentState, owner);
		int count = 0;
		for (String property : board.propertyGroups.get(group).memberNames) {
			if (ownerState.properties.contains(property)) {
				count++;
			}
		}
		return count;
	}
}

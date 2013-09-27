package com.github.mistertea.boardgame.landshark;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TTransport;
import org.junit.Assert;

import com.github.mistertea.boardgame.core.CoreCommand;
import com.github.mistertea.boardgame.core.CoreCommandType;
import com.github.mistertea.boardgame.core.DieRoll;
import com.github.mistertea.boardgame.core.DieRollEngine;
import com.github.mistertea.boardgame.core.ServerMessage;
import com.github.mistertea.boardgame.core.ServerMessageType;
import com.github.mistertea.boardgame.core.ThriftB64Utils;
import com.github.mistertea.boardgame.landshark.player.AbstractPlayer;
import com.github.mistertea.boardgame.landshark.player.SimplePlayer;

public class Engine extends DieRollEngine {
	private Board board;
	private State currentState;
	private List<State> history = new ArrayList<State>();
	public ConcurrentLinkedQueue<String> inputQueue = new ConcurrentLinkedQueue<String>();
	public Map<String, ConcurrentLinkedQueue<String>> outputQueues = new HashMap<>();
	private StateQuery query;
	private Stats stats;

	public Engine(Random rng, Board board, String gameName, List<String> players) {
		super(rng);
		this.board = board;
		this.query = new StateQuery(board);
		this.stats = new Stats();
		initialize(gameName, players);
	}

	private void initialize(String gameName, List<String> players) {
		// Init stats
		for (String propertyName : board.properties.keySet()) {
			stats.propertyOwnerStats.put(propertyName,
					new ArrayList<PropertyStats>());
		}
		currentState = new State().setId(gameName).setDice(
				rollAndGetDice(new DieRoll(2, 6, 0)));
		for (String playerName : players) {
			PlayerState currentPlayer = new PlayerState().setName(playerName)
					.setLocation(0).setCash(board.startingMoney);
			currentState.playerStates.add(currentPlayer);
		}
		rollTheDice();
	}

	public void update() {
		if (isGameOver()) {
			return;
		}
		// Process any inputs
		while (!inputQueue.isEmpty()) {
			processInput(inputQueue.poll());
		}
	}

	private void processInput(String input) {
		CoreCommand coreCommand;
		try {
			coreCommand = ThriftB64Utils.stringToThrift(input,
					CoreCommand.class);
		} catch (IOException e) {
			// Drop the packet if it cannot be parsed
			e.printStackTrace();
			return;
		}

		try {
			if (coreCommand.type < 1000) {
				CoreCommandType type = CoreCommandType
						.findByValue(coreCommand.type);
				if (type == null) {
					throw new IOException("Unknown command type: "
							+ coreCommand.type);
				}
				switch (type) {
				case CHAT:
					String chatMessage = "<" + coreCommand.player + "> "
							+ coreCommand.chat;
					broadcastMessage(new ServerMessage(ServerMessageType.CHAT,
							System.currentTimeMillis(), chatMessage));
					handlePlayerDeath(coreCommand.player);
					break;
				case QUIT:
					query.getPlayerState(currentState, coreCommand.player).quit = true;
					snapshot();
					break;
				default:
					break;
				}
			} else {
				LandsharkCommand landsharkCommand;
				try {
					landsharkCommand = ThriftB64Utils.stringToThrift(input,
							LandsharkCommand.class);
				} catch (IOException e) {
					// Drop the packet if it cannot be parsed
					e.printStackTrace();
					return;
				}

				LandsharkCommandType type = LandsharkCommandType
						.findByValue(landsharkCommand.type);
				if (type == null) {
					throw new IOException("Unknown command type: "
							+ landsharkCommand.type);
				}
				switch (type) {
				case BID:
					if (currentState.auctionState == null) {
						throw new IOException(
								"Tried to bid when not in an auction: "
										+ landsharkCommand);
					}
					currentState.auctionState.bids.put(landsharkCommand.player,
							landsharkCommand.bid);
					if (currentState.auctionState.bids.size() == query
							.getActivePlayers(currentState)) {
						completeAuction();
					}
					break;
				case BUY_HOUSES:
					Assert.assertEquals(TurnState.BUYING_HOUSES,
							currentState.turnState);
					if (!landsharkCommand.housePurchases.isEmpty()) {
						for (Map.Entry<String, Integer> entry : landsharkCommand.housePurchases
								.entrySet()) {
							String property = entry.getKey();
							int houses = entry.getValue();
							if (!query.canPutHouses(currentState,
									landsharkCommand.player, property, houses)) {
								query.canPutHouses(currentState,
										landsharkCommand.player, property,
										houses);
								throw new IOException(
										"Invalid house allocation: "
												+ landsharkCommand.player + " "
												+ property + " " + houses);
							} else {
								putHouses(property, houses);
							}
						}
					} else {
						finishBuyingHouses();
					}
					break;
				case CHOOSE_AUCTION:
					Assert.assertTrue(currentState.turnState == TurnState.FORCE_AUCTION
							|| currentState.turnState == TurnState.ASK_ANY_AUCTION
							|| currentState.turnState == TurnState.ASK_AUCTION);
					if (landsharkCommand.property == null
							&& (currentState.turnState != TurnState.FORCE_AUCTION)) {
						finishRolling();
					} else {
						Assert.assertEquals("Invalid auction choice: "
								+ landsharkCommand.toString(),
								query.getOwner(currentState, board.properties
										.get(landsharkCommand.property)),
								landsharkCommand.player);
						if (currentState.turnState == TurnState.ASK_AUCTION) {
							Assert.assertEquals(
									board.propertyOrder
											.get(query
													.getCurrentPlayerState(currentState).location),
									landsharkCommand.property);
						}
						currentState.turnState = TurnState.AUCTION;
						currentState.auctionState = new AuctionState()
								.setProperty(landsharkCommand.property)
								.setAuctionOwner(landsharkCommand.player);
						snapshot();
					}
					break;
				default:
					break;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
//			ServerMessage errorMessage = new ServerMessage(
//					ServerMessageType.ERROR, System.currentTimeMillis(),
//					"Error processing input: " + e.getMessage());
//			outputQueues.get(coreCommand.player).add(
//					ThriftB64Utils.ThriftToString(errorMessage));
		}
	}

	private void putHouses(String property, int houses) {
		String owner = query.getOwner(currentState,
				board.properties.get(property));
		Assert.assertTrue(query.canAffordHouses(currentState, owner, property,
				houses));
		Integer pastHouses = currentState.houses.get(property);
		if (pastHouses == null) {
			pastHouses = 0;
		}
		currentState.houses.put(property, pastHouses + houses);
		List<PropertyStats> propertyOwnerStats = stats.propertyOwnerStats
				.get(property);

		// TODO: The fact that you buy houses later is neglected here. Maybe
		// create a new entry?
		int cost = query.getHousePrice(currentState, property) * houses;
		query.getPlayerState(currentState, owner).cash -= cost;
		Assert.assertTrue(query.getPlayerState(currentState, owner).cash >= 0);

		PropertyStats ownerStats = propertyOwnerStats.get(propertyOwnerStats
				.size() - 1);
		ownerStats.investment = ownerStats.investment + cost;
	}

	private void completeAuction() {
		AuctionState auctionState = currentState.auctionState;
		Property auctionProperty = board.properties.get(auctionState.property);

		// Get the highest bidder
		String highestBidder = null;
		boolean tieForTop = false;
		for (Map.Entry<String, Integer> entry : auctionState.bids.entrySet()) {
			if (highestBidder == null) {
				highestBidder = entry.getKey();
			} else {
				int highestBid = auctionState.bids.get(highestBidder);
				if (highestBid == entry.getValue()) {
					tieForTop = true;
				} else if (highestBid < entry.getValue()) {
					tieForTop = false;
					highestBidder = entry.getKey();
				}
			}
		}

		if (false && tieForTop) { // For now, break ties based on user name hash
			// Redo auction
			snapshot();
			currentState.auctionState.bids.clear();
		} else {
			// Someone wins the auction
			PlayerState highestBidderState = query.getPlayerState(currentState,
					highestBidder);
			int winningBid = auctionState.bids.get(highestBidder);
			if (winningBid == 0) {
				// No one bid a positive number, skip the auction
				currentState.message = "No one bid on the auction.  Auction cancelled.";
			} else {
				if (auctionState.auctionOwner == null
						|| highestBidder.equals(auctionState.auctionOwner)) {
					// The winner has to pay the bank
					highestBidderState.cash -= winningBid;
					currentState.message = highestBidder
							+ " won the auction and pays " + winningBid
							+ " to the bank";
				} else {
					// The winner pays someone else
					highestBidderState.cash -= winningBid;
					PlayerState seller = query.getPlayerState(currentState,
							auctionState.auctionOwner);
					seller.cash += winningBid;
					seller.properties.remove(auctionState.property);
					PropertyStats propertyStats = getLatestPropertyStats(auctionState.property);
					propertyStats.revenue += winningBid;
					currentState.message = highestBidder
							+ " won the auction and pays " + winningBid
							+ " to " + seller.name;
				}
				highestBidderState.properties
						.add(currentState.auctionState.property);
				boolean winningCreatesGroup = query.playerOwnsGroup(
						currentState, highestBidder, auctionProperty.group);
				stats.propertyOwnerStats.get(auctionState.property).add(
						new PropertyStats(highestBidder, winningBid, 0, 0,
								winningCreatesGroup, 1));
			}
			currentState.turnState = TurnState.AUCTION_RESULTS;
			snapshot();
			currentState.auctionState = null;
			finishRolling();
		}
	}

	private PropertyStats getLatestPropertyStats(String property) {
		List<PropertyStats> statsList = stats.propertyOwnerStats.get(property);
		Assert.assertFalse(statsList.isEmpty());
		return statsList.get(statsList.size() - 1);
	}

	private void rollTheDice() {
		PlayerState currentPlayerState = query
				.getCurrentPlayerState(currentState);
		currentState.dice = rollAndGetDice(new DieRoll(2, 6, 0));
		if (hasDoubles()) {
			// Rolled doubles
			currentState.numDoubles++;
		} else {
			currentState.numDoubles = 0;
		}
		currentState.turnState = TurnState.ROLLING_DICE;
		snapshot();

		if (currentState.numDoubles == 3) {
			// 3 doubles, forced auction if the player has anything to
			// auction
			if (currentPlayerState.properties.isEmpty()) {
				finishRolling();
			} else {
				currentState.turnState = TurnState.FORCE_AUCTION;
				snapshot();
			}
			return;
		}

		int diceSum = currentState.dice.get(0) + currentState.dice.get(1);
		// Move the player
		currentPlayerState.location += diceSum;
		while (currentPlayerState.location >= board.properties.size()) {
			// Cross the start space, collect 200.
			currentPlayerState.location -= board.properties.size();
			currentPlayerState.cash += 200;
			currentState.message = currentPlayerState.name
					+ " crosses start and collects 200";
			snapshot();
		}

		Property property = board.properties.get(board.propertyOrder
				.get(currentPlayerState.location));

		switch (property.type) {
		case AUCTIONHOUSE:
			if (currentPlayerState.properties.isEmpty()) {
				finishRolling();
			} else {
				currentState.turnState = TurnState.ASK_ANY_AUCTION;
				snapshot();
			}
			break;
		case EMPTY:
		case START:
			finishRolling();
			break;
		case UTILITY:
		case STREET: {
			String owner = query.getOwner(currentState, property);
			if (owner != null
					&& owner.equals(query.getCurrentPlayer(currentState))) {
				currentState.turnState = TurnState.ASK_AUCTION;
				snapshot();
			} else if (owner != null) {
				// Pay owner
				int rent = query.getRent(currentState, property);
				currentPlayerState.cash -= rent;
				query.getPlayerState(currentState, owner).cash += rent;
				getLatestPropertyStats(property.name).revenue += rent;
				currentState.turnState = TurnState.PAYING_RENT;
				currentState.message = currentPlayerState.name + " pays "
						+ rent + " to " + owner + " for rent on "
						+ property.name;
				snapshot();
				finishRolling();
			} else {
				// Start auction

				// TODO: Maybe the player who landed on the square should win in
				// the event of a tie?
				currentState.turnState = TurnState.AUCTION;
				currentState.auctionState = new AuctionState()
						.setProperty(property.name);
				snapshot();
			}
		}
			break;
		case TAXES: {
			int taxes = (int) ((currentPlayerState.cash * (100L - property.tax)) / 100L);
			currentPlayerState.cash = taxes;
			currentState.message = currentPlayerState.name
					+ " has to pay taxes of " + taxes;
			finishRolling();
		}
			break;
		}
	}

	private void handlePlayerDeath(String playerName) {
		PlayerState playerState = query
				.getPlayerState(currentState, playerName);
		if (playerState.quit || playerState.cash < 0) {
			// Lose all properties (if we owned any)
			playerState.properties.clear();
		}
	}

	private boolean hasDoubles() {
		return currentState.dice.get(0) == currentState.dice.get(1);
	}

	private void finishRolling() {
		// Handle player death (if appropriate)
		handlePlayerDeath(query.getCurrentPlayer(currentState));

		// If we haven't tried buying houses, try now.
		if (query.canBuildHouses(currentState)) {
			currentState.turnState = TurnState.BUYING_HOUSES;
			snapshot();
			return;
		} else {
			finishBuyingHouses();
		}
	}

	private void finishBuyingHouses() {
		if (hasDoubles()) {
			if (query.isActivePlayer(query.getCurrentPlayerState(currentState))) {
				rollTheDice();
				return;
			}
		}

		while (true) {
			currentState.playerTurn = (currentState.playerTurn + 1)
					% currentState.playerStates.size();
			if (query.isActivePlayer(query.getCurrentPlayerState(currentState))) {
				rollTheDice();
				return;
			}
		}
	}

	private void snapshot() {
		broadcastMessage(new ServerMessage(ServerMessageType.NEW_STATE,
				System.currentTimeMillis(),
				ThriftB64Utils.ThriftToString(currentState)));
		// Add the state to the history
		history.add(currentState.deepCopy());
		System.out.println("CURRENT STATE: " + currentState.toString());
		currentState.message = null;
	}

	private void broadcastMessage(ServerMessage serverMessage) {
		String serverMessageString = ThriftB64Utils
				.ThriftToString(serverMessage);
		for (ConcurrentLinkedQueue<String> outputQueue : outputQueues.values()) {
			outputQueue.add(serverMessageString);
		}
	}

	private boolean isGameOver() {
		return query.getActivePlayers(currentState) <= 1;
	}

	public static void main(String[] args) throws IOException, TException {
		Board newBoard = new Board();
		{
			TTransport trans = new TIOStreamTransport(new FileInputStream(
					"Board.tft"));
			TProtocol prot = new TJSONProtocol(trans);
			newBoard.read(prot);
			trans.close();
		}

		List<String> playerNames = new ArrayList<String>();
		playerNames.add("a");
		playerNames.add("b");
		playerNames.add("c");
		playerNames.add("d");
		Engine engine = new Engine(new Random(1L), newBoard, "GameName",
				playerNames);

		Map<String, AbstractPlayer> playerControllers = new HashMap<>();
		// playerControllers.put("a", new ConsolePlayer("a"));
		playerControllers.put("a", new SimplePlayer("a"));
		playerControllers.put("b", new SimplePlayer("b"));
		playerControllers.put("c", new SimplePlayer("c"));
		playerControllers.put("d", new SimplePlayer("d"));

		while (!engine.isGameOver()) {
			// Poll for inputs
			for (AbstractPlayer playerController : playerControllers.values()) {
				if (!engine.query.isActivePlayer(engine.query.getPlayerState(
						engine.currentState, playerController.getName()))) {
					// Player is not active
					continue;
				}

				if (engine.currentState.turnState != TurnState.AUCTION
						&& !engine.query.getCurrentPlayer(engine.currentState)
								.equals(playerController.getName())) {
					// It isn't this player's turn.
					continue;
				}

				String command = playerController.fetchCommand(engine.query,
						engine.currentState);
				if (command == null) {
					continue;
				}
				if (command.isEmpty()) {
					System.out.println("ABORTING GAME");
					// Special command to abort
					break;
				}
				engine.inputQueue.add(command);
			}
			engine.update();
		}
		System.out.println("GAME OVER");
	}
}

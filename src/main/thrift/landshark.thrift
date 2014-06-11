namespace java com.github.mistertea.boardgame.landshark
namespace scala com.github.mistertea.boardgame.landshark.scala

include "core.thrift"

enum PropertyType {
	STREET,
	AUCTIONHOUSE,
	UTILITY,
	START,
	TAXES,
	EMPTY,
}

enum TurnState {
	START_GAME,
	ROLLING_DICE,
	ASK_AUCTION,
	ASK_ANY_AUCTION,
	FORCE_AUCTION,
	BUYING_HOUSES,
	AUCTION,
	AUCTION_RESULTS,
	PAYING_RENT,
}

struct PropertyGroup {
  1:set<string> memberNames = [],
  2:i32 housePrice,
  3:PropertyType type,
}

struct PlayerState {
	1:string name,
	2:set<string> properties = [],
	3:i32 location = 0,
	4:i32 cash = 0,
	5:bool quit=false,
}

struct AuctionState {
	1:string property,
	2:string auctionOwner, // null means the bank owns the property
	3:map<string, i32> bids = {},
}

struct State {
	1:string id,
	2:list<PlayerState> playerStates = [],
	3:map<string, i32> houses = {},
	4:i32 playerTurn = 0,
	5:TurnState turnState = TurnState.START_GAME,
	6:AuctionState auctionState,

	100:list<i32> dice = [],
	101:i32 numDoubles = 0,
	102:string message,
}

struct Property {
	1:string name,
	2:PropertyType type,
	3:list<i32> rent = [],
	4:string group,
	5:i32 tax,
}

struct Board {
	1:map<string,Property> properties = {},
	2:list<string> propertyOrder = [],
	3:i32 startingMoney = 1500,
	4:map<string,PropertyGroup> propertyGroups = {},
}

enum LandsharkCommandType {
	BID = 1000,
	PASS,
	BUY_HOUSES,
	CHOOSE_AUCTION,
}

struct LandsharkCommand {
	1:string player,
	2:i64 creationTime,
	3:i32 type,
	4:string property,
	5:map<string, i32> housePurchases,
	6:i32 bid,
}

struct PropertyStats {
	1:string owner,
	2:i32 price,
	3:i32 investment,  // investment includes buying houses
	4:i32 revenue,
	5:bool street,  // True if the owner owns the entire street
	6:i32 duration,
}

struct PlayerStats {
	1:string name,
}

struct Stats {
	1:map<string, list<PropertyStats> > propertyOwnerStats = {},
	2:map<string, PlayerStats> playerStats = {},
}



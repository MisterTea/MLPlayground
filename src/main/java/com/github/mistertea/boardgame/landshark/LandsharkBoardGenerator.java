package com.github.mistertea.boardgame.landshark;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

import junit.framework.Assert;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TTransport;

public class LandsharkBoardGenerator {

	/**
	 * @param args
	 * @throws FileNotFoundException
	 * @throws TException
	 */
	public static void main(String[] args) throws FileNotFoundException,
			TException {
		Board board = new Board();
		addProperty(board, "Home Base", PropertyType.START);
		addStreet(board, "Mediterranian Avenue", 2, 50, "Brown");
		addProperty(board, "Bonhams", PropertyType.AUCTIONHOUSE);
		addStreet(board, "Baltic Avenue", 4, 50, "Brown");
		addProperty(board, "Traffic Jam", PropertyType.EMPTY);
		addUtility(board, "JFK", "Airport");
		addStreet(board, "Oriental Avenue", 6, 50, "LightBlue");
		addProperty(board, "Antiquorum", PropertyType.AUCTIONHOUSE);
		addStreet(board, "Vermont Avenue", 6, 50, "LightBlue");
		addStreet(board, "Connecticut Avenue", 8, 50, "LightBlue");
		addTaxes(board, "Payroll Tax", 10);
		addStreet(board, "St. Charlies Place", 10, 100, "Purple");
		addUtility(board, "Electric Company", "Utilities");
		addStreet(board, "States Avenue", 10, 100, "Purple");
		addStreet(board, "Virginia Avenue", 12, 100, "Purple");
		addUtility(board, "LAX", "Airport");
		addStreet(board, "St. James Place", 14, 100, "Orange");
		addProperty(board, "Dorotheum", PropertyType.AUCTIONHOUSE);
		addStreet(board, "Tennessee Avenue", 14, 100, "Orange");
		addStreet(board, "New York Avenue", 16, 100, "Orange");
		addTaxes(board, "Legal Battles", 20);
		addStreet(board, "Kentucky Avenue", 18, 150, "Red");
		addProperty(board, "TODO_AUCTION_HOUSE_NAME", PropertyType.AUCTIONHOUSE);
		addStreet(board, "Indiana Avenue", 18, 150, "Red");
		addStreet(board, "Illinois Avenue", 20, 150, "Red");
		addUtility(board, "Orlando International", "Airport");
		addStreet(board, "Atlantic Avenue", 22, 150, "Yellow");
		addStreet(board, "Ventnor Avenue", 22, 150, "Yellow");
		addUtility(board, "Water Works", "Utilities");
		addStreet(board, "Marvin Gardens", 24, 150, "Yellow");
		addTaxes(board, "Government Audit", 5);
		addStreet(board, "Pacific Avenue", 24, 200, "Green");
		addStreet(board, "North Carolina Avenue", 24, 200, "Green");
		addProperty(board, "TODO_AUCTION_HOUSE_NAME2",
				PropertyType.AUCTIONHOUSE);
		addStreet(board, "Pennsylvania Avenue", 24, 200, "Green");
		addUtility(board, "Dallas Fort Worth", "Airport");
		addUtility(board, "Waste Services", "Utilities");
		addStreet(board, "Park Place", 35, 200, "DarkBlue");
		addTaxes(board, "Maintinence Fees", 20);
		addStreet(board, "Boardwalk", 40, 200, "DarkBlue");

		TTransport trans = new TIOStreamTransport(new FileOutputStream(
				"Board.tft"));
		TProtocol prot = new TJSONProtocol(trans);
		board.write(prot);
		trans.close();

		/*
		 * TTransport trans2 = new TIOStreamTransport(new FileInputStream(
		 * "NewBoard.tft")); TProtocol prot2 = new TJSONProtocol(trans2); Board
		 * newBoard = new Board(); newBoard.read(prot2); trans2.close();
		 * 
		 * System.out.println(board.toString()); System.out.println("***");
		 * System.out.println(newBoard.toString());
		 * Assert.assertEquals(board.toString(), newBoard.toString());
		 */
	}

	private static void addUtility(Board board, String name, String group) {
		Property property = new Property(name, PropertyType.UTILITY, null,
				group, 0);
		Assert.assertFalse(board.properties.containsKey(name));
		board.properties.put(name, property);
		board.propertyOrder.add(name);
		if (!board.propertyGroups.containsKey(group)) {
			board.propertyGroups.put(group,
					new PropertyGroup().setType(PropertyType.UTILITY));
		}
		board.propertyGroups.get(group).memberNames.add(name);
	}

	private static void addProperty(Board board, String name, PropertyType type) {
		Property property = new Property(name, type, null, null, 0);
		Assert.assertFalse(board.properties.containsKey(name));
		board.properties.put(name, property);
		board.propertyOrder.add(name);
	}

	private static void addTaxes(Board board, String name, int tax) {
		Property property = new Property(name, PropertyType.TAXES, null, null,
				tax);
		Assert.assertFalse(board.properties.containsKey(name));
		board.properties.put(name, property);
		board.propertyOrder.add(name);
	}

	private static void addStreet(Board board, String name, int baseRent,
			int housePrice,
			String group) {
		PropertyType type = PropertyType.STREET;
		ArrayList<Integer> rents = null;
		Integer oneHouseRent = baseRent * 5;
		rents = new ArrayList<Integer>();
		rents.add(baseRent);
		rents.add(oneHouseRent);

		int cumsum = oneHouseRent;
		cumsum *= 3; // second house payoff is 3x first house
		cumsum += 9; // ceiling function
		rents.add((cumsum / 10) * 10);

		// The payoff multiplier from 2 to 3 houses is computed based on the
		// rent.
		double twoToThree = Math.min(3.0,
				4.8 * Math.pow(Math.log(baseRent), -0.6));
		cumsum *= twoToThree;
		cumsum += 19; // ceiling function
		rents.add((cumsum / 20) * 20);

		// The payoff from 3 to 4 houses follows a log-power curve
		double threeToFour = Math.min(2.0,
				2 * Math.pow(Math.log(baseRent), -0.4));
		cumsum *= threeToFour;
		cumsum += 49; // ceiling function
		rents.add((cumsum / 50) * 50);

		double fourToHotel = Math.min(1.5,
				1.5 * Math.pow(Math.log(baseRent), -0.2));
		cumsum *= fourToHotel;
		cumsum += 49; // ceiling function
		rents.add((cumsum / 50) * 50);

		System.out.print("RENTS: ");
		for (int r : rents) {
			System.out.print(r + " ");
		}
		System.out.println();

		Property property = new Property(name, type, rents, group, 0);
		Assert.assertFalse(board.properties.containsKey(name));
		board.properties.put(name, property);
		board.propertyOrder.add(name);
		if (!board.propertyGroups.containsKey(group)) {
			board.propertyGroups.put(group,
					new PropertyGroup().setType(PropertyType.STREET)
							.setHousePrice(housePrice));
		}
		board.propertyGroups.get(group).memberNames.add(name);
	}
}

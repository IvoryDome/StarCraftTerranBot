import java.awt.List;

import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

public class TerranBot extends DefaultBWListener {

	private Mirror mirror = new Mirror();

	private Game game;

	private Player self;

	public void run() {
		mirror.getModule().setEventListener(this);
		mirror.startGame();
	}

	public boolean hasUnitType(UnitType unitType) {
		int i = 0;
		for (Unit myUnit : self.getUnits()) {
			if (myUnit.getType() == unitType) {
				i++;
			}
		}
		if (i > 0) {
			return true;
		} else {
			return false;
		}
	}

	public boolean hasNumberOfUnit(int num, UnitType unitType) {
		int i = 0;
		for (Unit myUnit : self.getUnits()) {
			if (myUnit.getType() == unitType) {
				i++;
			}
		}
		if (i == num) {
			return true;
		} else {
			return false;
		}
	}

//	List allWorkers(List units) {
//		List workerList = new List();
//		for (Unit myUnit : self.getUnits() ) {
//			if (myUnit.getType() == UnitType.Terran_SCV) {
//			workerList.add(Unit myUnit);
//		}

	// Returns a suitable TilePosition to build a given building type near
	// specified TilePosition aroundTile, or null if not found. (builder parameter
	// is our worker)
	public TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile) {
		TilePosition ret = null;
		int maxDist = 3;
		int stopDist = 40;

		// Refinery, Assimilator, Extractor
		if (buildingType.isRefinery()) {
			for (Unit n : game.neutral().getUnits()) {
				if ((n.getType() == UnitType.Resource_Vespene_Geyser)
						&& (Math.abs(n.getTilePosition().getX() - aroundTile.getX()) < stopDist)
						&& (Math.abs(n.getTilePosition().getY() - aroundTile.getY()) < stopDist))
					return n.getTilePosition();
			}
		}

		while ((maxDist < stopDist) && (ret == null)) {
			for (int i = aroundTile.getX() - maxDist; i <= aroundTile.getX() + maxDist; i++) {
				for (int j = aroundTile.getY() - maxDist; j <= aroundTile.getY() + maxDist; j++) {
					if (game.canBuildHere(new TilePosition(i, j), buildingType, builder, false)) {
						// units that are blocking the tile
						boolean unitsInWay = false;
						for (Unit u : game.getAllUnits()) {
							if (u.getID() == builder.getID())
								continue;
							if ((Math.abs(u.getTilePosition().getX() - i) < 4)
									&& (Math.abs(u.getTilePosition().getY() - j) < 4))
								unitsInWay = true;
						}
						if (!unitsInWay) {
							return new TilePosition(i, j);
						}
						// creep for Zerg
						if (buildingType.requiresCreep()) {
							boolean creepMissing = false;
							for (int k = i; k <= i + buildingType.tileWidth(); k++) {
								for (int l = j; l <= j + buildingType.tileHeight(); l++) {
									if (!game.hasCreep(k, l))
										creepMissing = true;
									break;
								}
							}
							if (creepMissing)
								continue;
						}
					}
				}
			}
			maxDist += 2;
		}

		if (ret == null)
			game.printf("Unable to find suitable build position for " + buildingType.toString());
		return ret;
	}

	@Override
	public void onUnitCreate(Unit unit) {
		System.out.println("New unit discovered " + unit.getType());

	}

	@Override
	public void onStart() {
		game = mirror.getGame();
		self = game.self();

		// Use BWTA to analyze map
		// This may take a few minutes if the map is processed first time!
		System.out.println("Analyzing map...");
		BWTA.readMap();
		BWTA.analyze();
		System.out.println("Map data ready");

		int i = 0;
		for (BaseLocation baseLocation : BWTA.getBaseLocations()) {
			System.out.println("Base location #" + (++i) + ". Printing location's region polygon:");
			for (Position position : baseLocation.getRegion().getPolygon().getPoints()) {
				System.out.print(position + ", ");
			}
			System.out.println();
		}

//		game.setLocalSpeed(7);

	}

	@Override
	public void onFrame() {
		// game.setTextSize(10);
		game.drawTextScreen(10, 10, "Playing as " + self.getName() + " - " + self.getRace());

		StringBuilder units = new StringBuilder("My units:\n");

		Unit scv1 = self.getUnits().get(0);
		Unit scv2 = self.getUnits().get(1);
		Unit scv3 = self.getUnits().get(2);
		Unit scv4 = self.getUnits().get(3);
//		Unit scv5 = self.getUnits().get(5);
//		Unit scv6 = self.getUnits().get(6);
//		Unit scv7 = self.getUnits().get(7);
//		Unit refinery1 = null;

//		for (Unit myUnit : self.getUnits()) {
//			if (myUnit.getType() == UnitType.Terran_Refinery && !myUnit.isBeingConstructed()) {
//				refinery1 = myUnit;
//			}
//		}

		// iterate through my units
		for (Unit myUnit : self.getUnits()) {
			units.append(myUnit.getType()).append(" ").append(myUnit.getTilePosition()).append("\n");

			// if there's enough minerals, train an SCV
			if (myUnit.getType() == UnitType.Terran_Command_Center && self.minerals() >= 50 && myUnit.isIdle()) {
				myUnit.train(UnitType.Terran_SCV);
			}

			// train 7 marines
			if (myUnit.getType() == UnitType.Terran_Barracks && self.minerals() >= 50 && myUnit.isIdle()
					&& self.supplyUsed() < 70 && !hasNumberOfUnit(7, UnitType.Terran_Marine)) {
				myUnit.train(UnitType.Terran_Marine);
			}

			// if it's a worker and it's idle, send it to the closest mineral patch
			if (myUnit.getType().isWorker() && myUnit.isIdle()) {
				Unit closestMineral = null;
//				Unit secondClosestMineral = null;
//				Unit thirdClosestMineral = null;
//				Unit fourthClosestMineral = null;

				
				
				// find the closest mineral
				for (Unit neutralUnit : game.neutral().getUnits()) {
					if (neutralUnit.getType().isMineralField() && !neutralUnit.isBeingGathered()) {
						if (closestMineral == null								
								|| myUnit.getDistance(neutralUnit) < myUnit.getDistance(closestMineral)) {
							closestMineral = neutralUnit;
						}
					}
				}

				// if a mineral patch was found, send the worker to gather it
				if (closestMineral != null) {
					myUnit.gather(closestMineral, false);
				}
			}
//		}


			// print TilePosition and Position of my SCVs
			if (myUnit.getType() == UnitType.Terran_SCV) {
				game.drawTextMap(myUnit.getPosition().getX(), myUnit.getPosition().getY(),
						"TilePos: " + myUnit.getTilePosition().toString() + " Pos: " + myUnit.getPosition().toString());
			}

		}

		if ((self.supplyTotal() - self.supplyUsed() < 5) && (self.minerals() >= 100) && (self.supplyUsed() > 20)) {
			// iterate over units to find a worker
			for (Unit myUnit : self.getUnits()) {
				if ((myUnit.getType() == UnitType.Terran_SCV) && (myUnit != scv4) && !(myUnit.isGatheringGas())) {
					// get a nice place to build a supply depot
					TilePosition buildTile = getBuildTile(myUnit, UnitType.Terran_Supply_Depot,
							self.getStartLocation());
					// and, if found, send the worker to build it (and leave others alone - break;)
					if (buildTile != null) {
						myUnit.build(UnitType.Terran_Supply_Depot, buildTile);
						break;
					}
				}
			}
		}

		if ((self.supplyTotal() - self.supplyUsed() < 5) && (self.minerals() >= 100) && (self.supplyUsed() < 22)) {
			// iterate over units to find a worker
//			for (Unit myUnit : self.getUnits()) {
//				if ((myUnit.getType() == UnitType.Terran_SCV)) {
				// get a nice place to build a supply depot
				TilePosition buildTile = getBuildTile(scv4, UnitType.Terran_Supply_Depot, self.getStartLocation());
				// and, if found, send the worker to build it (and leave others alone - break;)
				if (buildTile != null) {
					scv4.build(UnitType.Terran_Supply_Depot, buildTile);
//					break;
				}
//				}
//			}
		}

		if (hasNumberOfUnit(7, UnitType.Terran_Marine)) {
			for (Unit myUnit : self.getUnits()) {
				if (myUnit.getType() == UnitType.Terran_Barracks) {
					myUnit.lift();

				}
			}
		}

		if ((self.supplyUsed() > 20) && (self.minerals() >= 150) && !(hasUnitType(UnitType.Terran_Barracks))) {
			// iterate over units to find a worker
//			for (Unit myUnit : self.getUnits()) {
//					if ((myUnit.getType() == UnitType.Terran_SCV)) { // && (myUnit.isGatheringMinerals())) {
			// get a nice place to build a barracks
			TilePosition buildTile = getBuildTile(scv4, UnitType.Terran_Barracks, self.getStartLocation());
			// and, if found, send the worker to build it (and leave others alone - break;)
			if (buildTile != null) {
				scv4.build(UnitType.Terran_Barracks, buildTile);
//							break;
			}
		}
//				}
//		}

		if ((self.supplyUsed() > 23) && (self.minerals() >= 100) && !(hasUnitType(UnitType.Terran_Refinery))
				&& (hasUnitType(UnitType.Terran_Barracks))) {
			// iterate over units to find a worker
//			for (Unit myUnit : self.getUnits()) {
//					if (myUnit.getType() == UnitType.Terran_SCV) {
			// get a nice place to build a refinery
			TilePosition buildTile = getBuildTile(scv2, UnitType.Terran_Refinery, self.getStartLocation());
			// and, if found, send the worker to build it (and leave others alone - break;)
			if (buildTile != null) {
				scv2.build(UnitType.Terran_Refinery, buildTile);
//							break;
			}
		}

		if (self.minerals() >= 200 && self.gas() >= 100) {
//					if (myUnit.getType() == UnitType.Terran_SCV) {
			TilePosition buildTile = getBuildTile(scv1, UnitType.Terran_Factory, self.getStartLocation());
			// and, if found, send the worker to build it (and leave others alone - break;)
			if (buildTile != null) {
				scv4.build(UnitType.Terran_Factory, buildTile);
//							break;
			}
		}
			
			if ((self.minerals() >= 250) && (self.gas() >= 200)) {
				for (Unit myUnit: self.getUnits() ) {
					if (myUnit.getType() == UnitType.Terran_Factory) {
						myUnit.train(UnitType.Terran_Siege_Tank_Siege_Mode);
					}
					if (myUnit.getType() == UnitType.Terran_Machine_Shop) {
						myUnit.research(TechType.Spider_Mines);
						break;
					}
					
			}
		}
//				}

//		}
//        }

		// draw my units on screen
		game.drawTextScreen(10, 25, units.toString());

	}

//	}
//	}

	@Override
	public void onUnitComplete(Unit unit) {

		Unit scv1 = self.getUnits().get(0);
		Unit scv2 = self.getUnits().get(1);
		Unit scv3 = self.getUnits().get(2);

//		get scvs to mine refinery when it's complete

		if (unit.getType() == UnitType.Terran_Refinery) {
			scv1.gather(unit);
			scv2.gather(unit);
			scv3.gather(unit);
		}

		else if (unit.getType() == UnitType.Terran_Factory) {
			unit.buildAddon(UnitType.Terran_Machine_Shop);
		}
		

	}

	public static void main(String[] args) {
		new TerranBot().run();
	}
}
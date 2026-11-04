/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */


package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.Warehouse;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndEgoExtract;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Reflection;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.CheersForTheBeginning;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.Doubt;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.FruitOfUnderstanding;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.OrdealMob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.PerfectFood;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.AngelaSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CheersForTheBeginningSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.DoubtSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.FruitOfUnderstandingSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.PerfectFoodSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndQuest;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Angela, the AI secretary of L Corp. She waits by the stairs of floors 6, 11 and 16, can't be harmed,
//and on floor 6 offers the hero one of three Dawn ordeals, chosen at random when she appears.
//In exchange for enkephalin boxes from the warehouse she also extracts or refines E.G.O, once each per game.
public class Angela extends NPC {

	{
		spriteClass = AngelaSprite.class;

		properties.add(Property.IMMOVABLE);
	}

	//only the Angela of floor 6 (or one summoned by the Tester) offers the Dawn ordeal,
	//the ones on floors 11 and 16 only trade E.G.O for enkephalin
	private boolean offersOrdeal = true;

	@Override
	public int defenseSkill( Char enemy ) {
		return INFINITE_EVASION;
	}

	@Override
	public void damage( int dmg, Object src ) {
		//do nothing
	}

	@Override
	public boolean add( Buff buff ) {
		return false;
	}

	@Override
	public boolean reset() {
		return true;
	}

	@Override
	protected Char chooseEnemy() {
		return null;
	}

	@Override
	public boolean interact( Char c ) {
		sprite.turnTo( pos, c.pos );

		if (c != Dungeon.hero){
			return super.interact(c);
		}

		//an Angela summoned by the Tester picks her ordeal on the spot
		if (offersOrdeal && Quest.type == 0){
			Quest.spawned = true;
			Quest.type = Random.Int(4) + 1;
		}

		//the very first talk with Angela on this profile: her welcome, the badge and the gift
		if (!Warehouse.welcomed()){
			boolean before = Badges.isUnlocked(Badges.Badge.WELCOME_TO_LOBOTOMY);
			//a profile that already has the badge has met her before, so it gets no second gift
			boolean gift = !before && Warehouse.claimWelcome(WELCOME_REWARD);
			if (before) Warehouse.claimWelcome(0);
			Badges.validateWelcomeToLobotomy();
			if (gift){
				GLog.p(Messages.get(Angela.class, "welcome_reward", WELCOME_REWARD, Warehouse.enkephalin()));
				Game.runOnRenderThread(() -> showWelcome(1));
				return true;
			}
		}

		Game.runOnRenderThread(this::showMenu);
		return true;
	}

	//enkephalin boxes given once ever, with the welcome badge
	public static final int WELCOME_REWARD = 100;
	private static final int WELCOME_PAGES = 3;

	private void showWelcome( int page ){
		GameScene.show(new WndQuest(this, Messages.get(Angela.class, "welcome_" + page, WELCOME_REWARD)){
			@Override
			public void hide() {
				super.hide();
				if (page < WELCOME_PAGES){
					showWelcome(page + 1);
				} else {
					showMenu();
				}
			}
		});
	}

	private static final int MENU_ORDEAL  = 0;
	private static final int MENU_EXTRACT = 1;
	private static final int MENU_REFINE  = 2;
	private static final int MENU_LEAVE   = 3;
	private static final int MENU_ASIDE   = 4;

	//Angela's services: the ordeal (floor 6 only), E.G.O extraction and E.G.O refining, paid for with enkephalin boxes
	private void showMenu(){
		ArrayList<Integer> entries = new ArrayList<>();
		ArrayList<String> labels = new ArrayList<>();
		if (offersOrdeal){
			entries.add(MENU_ORDEAL);
			labels.add(Messages.get(Angela.class, "menu_ordeal", Quest.ordealName()));
		}
		entries.add(MENU_EXTRACT);
		labels.add(Messages.get(Angela.class, Quest.extracted ? "menu_extract_used" : "menu_extract"));
		entries.add(MENU_REFINE);
		labels.add(Messages.get(Angela.class, Quest.refined ? "menu_refine_used" : "menu_refine"));
		entries.add(MENU_ASIDE);
		labels.add(Messages.get(Angela.class, "menu_aside"));
		entries.add(MENU_LEAVE);
		labels.add(Messages.get(Angela.class, "menu_leave"));

		GameScene.show(new WndOptions(
				sprite(),
				Messages.titleCase(name()),
				Messages.get(Angela.class, "menu", Warehouse.enkephalin()),
				labels.toArray(new String[0])){
			@Override
			protected boolean enabled( int index ) {
				switch (entries.get(index)){
					case MENU_ORDEAL:  return !Quest.completed;
					case MENU_EXTRACT: return !Quest.extracted;
					case MENU_REFINE:  return !Quest.refined;
					default: return true;
				}
			}

			@Override
			protected void onSelect( int index ) {
				switch (entries.get(index)){
					case MENU_ORDEAL:  ordeal(); break;
					case MENU_EXTRACT: WndEgoExtract.open(Angela.this); break;
					case MENU_REFINE:  refine(); break;
					case MENU_ASIDE:   stepAside(); break;
				}
			}
		});
	}

	//Angela never walks around, so she swaps places with the hero when asked, in case she stands in the way
	private void stepAside(){
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive() || Dungeon.level.distance(pos, hero.pos) != 1) return;
		//she is immovable so nothing knocks her around, which also rules out the usual ally swap: done by hand,
		// the same way Char.interact swaps two characters
		if (hero.rooted || hero.paralysed > 0) return;
		int oldPos = pos;
		int newPos = hero.pos;

		hero.pos = oldPos;
		moveSprite(oldPos, newPos);
		move(newPos);

		hero.pos = newPos;
		hero.sprite.move(newPos, oldPos);
		hero.move(oldPos);

		yell(Messages.get(Angela.class, "aside"));
		//the swap happens from her menu, outside the hero's own action, so the turn goes on from here
		hero.spendAndNext(1 / hero.speed());
	}

	private static final String OFFERS_ORDEAL = "offers_ordeal";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(OFFERS_ORDEAL, offersOrdeal);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		offersOrdeal = !bundle.contains(OFFERS_ORDEAL) || bundle.getBoolean(OFFERS_ORDEAL);
	}

	private void ordeal(){
		if (Quest.given){
			yell(Messages.get(Angela.class, "in_progress", Quest.ordealName()));
			return;
		}
		GameScene.show(new WndOptions(
				sprite(),
				Messages.titleCase(name()),
				Messages.get(Angela.class, "offer", Quest.ordealName(), Quest.ORDEAL_REWARD),
				Messages.get(Angela.class, "accept"),
				Messages.get(Angela.class, "decline")){
			@Override
			protected void onSelect( int index ) {
				if (index == 0){
					Quest.start();
				} else {
					yell(Messages.get(Angela.class, "declined"));
				}
			}
		});
	}

	//called by WndEgoExtract once the hero has confirmed the extraction
	public void extract( Class<? extends Item> cls ){
		if (Quest.extracted) return;
		Item item = Reflection.newInstance(cls);
		if (item == null) return;
		RiskLevel risk = Warehouse.risk(item);
		int cost = Warehouse.extractCost(risk);
		if (!Warehouse.spendEnkephalin(cost)){
			yell(Messages.get(Angela.class, "not_enough"));
			return;
		}
		Quest.extracted = true;
		item.identify();
		if (!item.doPickUp(Dungeon.hero, Dungeon.hero.pos)){
			Dungeon.level.drop(item, Dungeon.hero.pos).sprite.drop();
		}
		Sample.INSTANCE.play(Assets.Sounds.EVOKE);
		GLog.p(Messages.get(Angela.class, "extracted", item.name(), cost, Warehouse.enkephalin()));
		yell(Messages.get(Angela.class, "extract_done"));
	}

	public static boolean refinable( Item item ){
		return Warehouse.risk(item) != null && item.level() < 2;
	}

	private void refine(){
		GameScene.selectItem(new WndBag.ItemSelector() {
			@Override
			public String textPrompt() {
				return Messages.get(Angela.class, "refine_prompt");
			}

			@Override
			public boolean itemSelectable( Item item ) {
				return refinable(item);
			}

			@Override
			public void onSelect( Item item ) {
				if (item == null || !refinable(item)) return;
				int cost = Warehouse.refineCost(Warehouse.costRisk(item));
				boolean enough = Warehouse.enkephalin() >= cost;
				String text = Messages.get(Angela.class, "refine_confirm", item.name(), cost, Warehouse.enkephalin());
				if (!enough) text += "\n\n" + Messages.get(Angela.class, "not_enough");
				GameScene.show(new WndOptions(
						new ItemSprite(item),
						Messages.titleCase(item.name()),
						text,
						Messages.get(Angela.class, "refine_yes", cost),
						Messages.get(Angela.class, "cancel")){
					@Override
					protected boolean enabled( int index ) {
						return index != 0 || enough;
					}

					@Override
					protected void onSelect( int index ) {
						if (index != 0 || Quest.refined || !refinable(item)) return;
						if (!Warehouse.spendEnkephalin(cost)){
							yell(Messages.get(Angela.class, "not_enough"));
							return;
						}
						Quest.refined = true;
						item.upgrade();
						Item.updateQuickslot();
						Dungeon.hero.sprite.emitter().start(Speck.factory(Speck.UP), 0.2f, 3);
						Sample.INSTANCE.play(Assets.Sounds.EVOKE);
						GLog.p(Messages.get(Angela.class, "refined", item.name(), cost, Warehouse.enkephalin()));
						yell(Messages.get(Angela.class, "refine_done"));
					}
				});
			}
		});
	}

	public static class Quest {

		public static final int GREEN   = 1; //Doubt
		public static final int VIOLET  = 2; //Fruit of Understanding
		public static final int AMBER   = 3; //Perfect Food x4, in the corridors
		public static final int CRIMSON = 4; //Cheers for the Beginning, anywhere on the floor

		public static final int AMBER_COUNT = 4;

		//enkephalin boxes given for overcoming a Dawn ordeal
		public static final int ORDEAL_REWARD = 10;

		private static boolean spawned;
		private static int type;
		private static boolean given;
		private static boolean completed;
		//each of Angela's services can be used once per game
		private static boolean extracted;
		private static boolean refined;

		public static void reset(){
			spawned = false;
			type = 0;
			given = false;
			completed = false;
			extracted = false;
			refined = false;
		}

		private static final String NODE      = "angela";
		private static final String SPAWNED   = "spawned";
		private static final String TYPE      = "type";
		private static final String GIVEN     = "given";
		private static final String COMPLETED = "completed";
		private static final String EXTRACTED = "extracted";
		private static final String REFINED   = "refined";

		public static void storeInBundle( Bundle bundle ){
			Bundle node = new Bundle();
			node.put(SPAWNED, spawned);
			node.put(TYPE, type);
			node.put(GIVEN, given);
			node.put(COMPLETED, completed);
			node.put(EXTRACTED, extracted);
			node.put(REFINED, refined);
			bundle.put(NODE, node);
		}

		public static void restoreFromBundle( Bundle bundle ){
			Bundle node = bundle.getBundle(NODE);
			if (node.isNull()){
				reset();
				return;
			}
			spawned = node.getBoolean(SPAWNED);
			type = node.getInt(TYPE);
			given = node.getBoolean(GIVEN);
			completed = node.getBoolean(COMPLETED);
			extracted = node.getBoolean(EXTRACTED);
			refined = node.getBoolean(REFINED);
		}

		public static boolean extracted(){
			return extracted;
		}

		public static boolean refined(){
			return refined;
		}

		public static String ordealName(){
			return Messages.get(Angela.class, "ordeal_" + type);
		}

		//places Angela near the entrance stairs of floor 6 while it is being built, with her ordeal
		public static void spawn( Level level, Room entranceRoom ){
			if (spawned) return;
			if (placeNearEntrance(level, entranceRoom, true) == null) return;

			spawned = true;
			type = Random.Int(4) + 1;
			given = false;
			completed = false;
		}

		//places an Angela without an ordeal near the entrance stairs of floors 11 and 16
		public static void spawnTrader( Level level, Room entranceRoom ){
			placeNearEntrance(level, entranceRoom, false);
		}

		private static Angela placeNearEntrance( Level level, Room entranceRoom, boolean offersOrdeal ){
			if (entranceRoom == null) return null;

			int entrance = level.entrance();
			int reachable = reachableCells(level, entrance, -1);
			ArrayList<Integer> near = new ArrayList<>();
			ArrayList<Integer> inTheWay = new ArrayList<>();
			ArrayList<Integer> inRoom = new ArrayList<>();
			for (Point p : entranceRoom.getPoints()){
				int cell = level.pointToCell(p);
				if (!entranceRoom.inside(p) || cell == entrance || !level.passable[cell] || level.solid[cell]
						|| level.traps.get(cell) != null || level.map[cell] == Terrain.EMPTY_SP
						|| level.findMob(cell) != null){
					continue;
				}
				boolean byDoor = false;
				for (int i : PathFinder.NEIGHBOURS4){
					if (level.map[cell + i] == Terrain.DOOR) byDoor = true;
				}
				if (byDoor) continue;
				//cells where she would stand in the way are only a last resort, she can still be asked to step aside
				if (cutsOff(level, cell, reachable)){
					inTheWay.add(cell);
					continue;
				}
				inRoom.add(cell);
				if (level.distance(cell, entrance) <= 2) near.add(cell);
			}
			if (near.isEmpty() && inRoom.isEmpty()) inRoom = inTheWay;
			if (near.isEmpty() && inRoom.isEmpty()) return null;

			Angela angela = new Angela();
			angela.offersOrdeal = offersOrdeal;
			angela.pos = Random.element(near.isEmpty() ? inRoom : near);
			level.mobs.add(angela);
			return angela;
		}

		//how many cells can be walked to from the entrance, with one cell taken by someone who never moves
		private static int reachableCells( Level level, int from, int blocked ){
			boolean[] passable = level.passable.clone();
			if (blocked >= 0) passable[blocked] = false;
			PathFinder.buildDistanceMap(from, passable);
			int count = 0;
			for (int d : PathFinder.distance){
				if (d < Integer.MAX_VALUE) count++;
			}
			return count;
		}

		//standing here would leave some part of the floor that could be reached before out of reach
		private static boolean cutsOff( Level level, int cell, int reachable ){
			return reachableCells(level, level.entrance(), cell) < reachable - 1;
		}

		public static void start(){
			given = true;

			Level level = Dungeon.level;
			switch (type){
				case GREEN: default:
					Doubt doubt = new Doubt();
					place(doubt, level.randomRespawnCell(doubt));
					break;
				case VIOLET:
					FruitOfUnderstanding fruit = new FruitOfUnderstanding();
					place(fruit, level.randomRespawnCell(fruit));
					break;
				case AMBER:
					ArrayList<Integer> cells = corridorCells(level, true);
					if (cells.size() < AMBER_COUNT) cells = corridorCells(level, false);
					for (int i = 0; i < AMBER_COUNT; i++){
						PerfectFood food = new PerfectFood();
						int cell = cells.isEmpty() ? level.randomRespawnCell(food) : cells.remove(Random.Int(cells.size()));
						place(food, cell);
					}
					break;
				case CRIMSON:
					CheersForTheBeginning cheers = new CheersForTheBeginning();
					place(cheers, level.randomRespawnCell(cheers));
					break;
			}

			GLog.n(ordealName());
			showOrdealText("start_" + type);
		}

		//corridor cells free for a larva, optionally only those out of the hero's sight
		private static ArrayList<Integer> corridorCells( Level level, boolean hidden ){
			ArrayList<Integer> result = new ArrayList<>();
			boolean[] corridors = PerfectFood.corridors(level);
			if (corridors == null) return result;
			for (int i = 0; i < corridors.length; i++){
				if (corridors[i] && !level.solid[i] && Actor.findChar(i) == null
						&& level.distance(i, Dungeon.hero.pos) > 2
						&& (!hidden || !level.heroFOV[i])){
					result.add(i);
				}
			}
			return result;
		}

		private static void place( Mob mob, int cell ){
			if (cell == -1 || Actor.findChar(cell) != null){
				Level level = Dungeon.level;
				int tries = 0;
				do {
					cell = Random.Int(level.length());
					tries++;
				} while ((!level.passable[cell] || level.solid[cell] || Actor.findChar(cell) != null
						|| level.distance(cell, Dungeon.hero.pos) <= 2) && tries < 500);
				if (tries >= 500) return;
			}
			mob.pos = cell;
			GameScene.add(mob);
		}

		//called whenever an ordeal monster dies, the ordeal ends once none are left on the floor
		public static void onOrdealMobSlain(){
			if (!given || completed) return;
			for (Mob m : Dungeon.level.mobs){
				if (m instanceof OrdealMob && m.isAlive()){
					return;
				}
			}
			completed = true;
			Warehouse.addEnkephalin(ORDEAL_REWARD);
			GLog.p(Messages.get(Angela.class, "reward", ORDEAL_REWARD, Warehouse.enkephalin()));
			showOrdealText("end_" + type);
		}

		private static void showOrdealText( String key ){
			String title = ordealName();
			String text = Messages.get(Angela.class, key);
			int t = type;
			Game.runOnRenderThread(() -> {
				CharSprite icon;
				switch (t){
					case GREEN: default: icon = new DoubtSprite(); break;
					case VIOLET: icon = new FruitOfUnderstandingSprite(); break;
					case AMBER: icon = new PerfectFoodSprite(); break;
					case CRIMSON: icon = new CheersForTheBeginningSprite(); break;
				}
				GameScene.show(new WndTitledMessage(icon, title, text));
			});
		}
	}
}

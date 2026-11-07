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
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.BeaconOfReturning;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
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
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.boss.BlueStar;
import com.shatteredpixel.shatteredpixeldungeon.sprites.BlueStarSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.CheersForTheBeginning;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.ClimaxThroes;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.FoodChain;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.WhereWeMustReach;
import com.shatteredpixel.shatteredpixeldungeon.levels.LCorpHQLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ClimaxThroesSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.FoodChainSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.WhereWeMustReachSprite;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.Doubt;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.FruitOfUnderstanding;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.GrantUsLove;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.HarmonyOfSkin;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.OrdealMob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.PerfectFood;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.ProcessOfUnderstanding;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.Sweeper;
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
import com.shatteredpixel.shatteredpixeldungeon.sprites.GrantUsLoveSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HarmonyOfSkinSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.PerfectFoodSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ProcessOfUnderstandingSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SweeperSprite;
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

//Angela, the AI secretary of L Corp. She waits by the stairs of floors 6, 11, 16 and 21, can't be harmed,
//and offers the hero one of four Dawn ordeals on floor 6, once that is overcome one of four Noon ordeals on floor 11,
//and once that is overcome too one of three Dusk ordeals on floor 16.
//In exchange for enkephalin boxes from the warehouse she also extracts or refines E.G.O, once each per game,
//and for a beacon of returning her elevator takes the hero to her on another floor already reached.
//On floor 26 another Angela stands at the top of the amulet's room, who does nothing but send the hero to a boss fight.
public class Angela extends NPC {

	{
		spriteClass = AngelaSprite.class;

		properties.add(Property.IMMOVABLE);
	}

	//the ordeal she offers: the Dawn on floor 6, the Noon on floor 11, the Dusk on floor 16, none on floor 21.
	//One summoned with the testing tools offers whichever comes next.
	public static final int NONE = 0;
	public static final int DAWN = 1;
	public static final int NOON = 2;
	public static final int DUSK = 3;
	//the Angela of floor 26, who only offers the boss fights
	public static final int BOSS_GATE = 4;
	private static final int NEXT = -1;

	private int ordealTier = NEXT;

	private int tier(){
		if (ordealTier == NEXT) return Quest.noonCompleted ? DUSK : Quest.completed ? NOON : DAWN;
		return ordealTier;
	}

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

		if (ordealTier == BOSS_GATE){
			Game.runOnRenderThread(this::showBossMenu);
			return true;
		}

		//an Angela summoned with the testing tools (or from an older save) picks her ordeal on the spot
		if (tier() == DAWN && Quest.type == 0){
			Quest.spawned = true;
			Quest.type = Random.Int(4) + 1;
		}
		if (tier() == NOON && Quest.noonType == 0){
			Quest.noonType = Random.Int(4) + 1;
		}
		if (tier() == DUSK && Quest.duskType == 0){
			Quest.duskType = Random.Int(3) + 1;
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
	private static final int MENU_ELEVATOR = 5;

	//Angela's services: the ordeal (floor 6 only), E.G.O extraction and E.G.O refining, paid for with enkephalin boxes
	private void showMenu(){
		ArrayList<Integer> entries = new ArrayList<>();
		ArrayList<String> labels = new ArrayList<>();
		int tier = tier();
		if (tier != NONE){
			entries.add(MENU_ORDEAL);
			labels.add(Messages.get(Angela.class, Quest.unlocked(tier) ? "menu_ordeal"
					: tier == DUSK ? "menu_ordeal_locked_dusk" : "menu_ordeal_locked", Quest.ordealName(tier)));
		}
		entries.add(MENU_EXTRACT);
		labels.add(Messages.get(Angela.class, Quest.extracted ? "menu_extract_used" : "menu_extract"));
		entries.add(MENU_REFINE);
		labels.add(Messages.get(Angela.class, Quest.refined ? "menu_refine_used" : "menu_refine"));
		entries.add(MENU_ELEVATOR);
		labels.add(Messages.get(Angela.class, "menu_elevator"));
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
					case MENU_ORDEAL:  return Quest.unlocked(tier) && !Quest.completed(tier);
					case MENU_EXTRACT: return !Quest.extracted;
					case MENU_REFINE:  return !Quest.refined;
					default: return true;
				}
			}

			@Override
			protected void onSelect( int index ) {
				switch (entries.get(index)){
					case MENU_ORDEAL:  ordeal(tier); break;
					case MENU_EXTRACT: WndEgoExtract.open(Angela.this); break;
					case MENU_REFINE:  refine(); break;
					case MENU_ASIDE:   stepAside(); break;
					case MENU_ELEVATOR: elevator(); break;
				}
			}
		});
	}

	//the boss fights the Angela of floor 26 can send the hero to, by message key.
	// Each one is fought at L Corp headquarters, which stays sealed until the boss falls
	public static final String BLUE_STAR = "blue_star";
	private static final String[] BOSS_FIGHTS = {BLUE_STAR};

	private void showBossMenu(){
		ArrayList<String> labels = new ArrayList<>();
		for (String boss : BOSS_FIGHTS){
			labels.add(Messages.get(Angela.class, "boss_" + boss));
		}
		boolean none = labels.isEmpty();
		if (none) labels.add(Messages.get(Angela.class, "boss_preparing"));
		labels.add(Messages.get(Angela.class, "menu_leave"));

		GameScene.show(new WndOptions(
				sprite(),
				Messages.titleCase(name()),
				Messages.get(Angela.class, "boss_menu"),
				labels.toArray(new String[0])){
			@Override
			protected boolean enabled( int index ) {
				return !none || index != 0;
			}

			@Override
			protected void onSelect( int index ) {
				if (!none && index < BOSS_FIGHTS.length) enterBossFight(BOSS_FIGHTS[index]);
			}
		});
	}

	private void enterBossFight( String boss ){
		GameScene.show(new WndOptions(
				sprite(),
				Messages.titleCase(name()),
				Messages.get(Angela.class, "boss_confirm", Messages.get(Angela.class, "boss_" + boss)),
				Messages.get(Angela.class, "boss_accept"),
				Messages.get(Angela.class, "cancel")){
			@Override
			protected void onSelect( int index ) {
				if (index == 0) Quest.startBossFight(boss);
			}
		});
	}

	//Angela of floor 26, as summoned with the testing tools
	public static class BossGate extends Angela {
		{
			((Angela) this).ordealTier = BOSS_GATE;
		}
	}

	//the floors where Angela waits by the stairs
	public static final int[] FLOORS = {6, 11, 16, 21};
	public static final int ELEVATOR_COST = 1;

	private static boolean canRideTo( int depth ){
		return depth != Dungeon.depth && Dungeon.levelHasBeenGenerated(depth, 0);
	}

	//her elevator: for a beacon of returning she sends the hero to her on another floor already reached
	private void elevator(){
		//closed for the whole ascent once the amulet has been taken, even if it is put down again
		if (Statistics.amuletObtained){
			yell(Messages.get(Angela.class, "elevator_ascent"));
			return;
		}
		BeaconOfReturning beacon = Dungeon.hero.belongings.getItem(BeaconOfReturning.class);
		int beacons = beacon == null ? 0 : beacon.quantity();
		String text = Messages.get(Angela.class, "elevator", ELEVATOR_COST, beacons);
		if (beacons < ELEVATOR_COST) text += "\n\n" + Messages.get(Angela.class, "elevator_no_beacon");

		ArrayList<Integer> floors = new ArrayList<>();
		ArrayList<String> labels = new ArrayList<>();
		for (int d : FLOORS){
			if (d == Dungeon.depth) continue;
			floors.add(d);
			labels.add(Messages.get(Angela.class, canRideTo(d) ? "elevator_floor" : "elevator_floor_locked", d));
		}
		labels.add(Messages.get(Angela.class, "cancel"));

		GameScene.show(new WndOptions(
				sprite(),
				Messages.titleCase(name()),
				text,
				labels.toArray(new String[0])){
			@Override
			protected boolean enabled( int index ) {
				return index >= floors.size() || (beacons >= ELEVATOR_COST && canRideTo(floors.get(index)));
			}

			@Override
			protected void onSelect( int index ) {
				if (index < floors.size()) ride(floors.get(index));
			}
		});
	}

	private void ride( int depth ){
		Hero hero = Dungeon.hero;
		BeaconOfReturning beacon = hero.belongings.getItem(BeaconOfReturning.class);
		if (beacon == null || beacon.quantity() < ELEVATOR_COST || !canRideTo(depth) || !hero.isAlive()) return;
		if (Statistics.amuletObtained || !Dungeon.interfloorTeleportAllowed()){
			yell(Messages.get(Angela.class, "elevator_blocked"));
			return;
		}

		//the last beacon goes with its return point, the same as when it is used up by itself
		if (beacon.quantity() == 1){
			BeaconOfReturning.BeaconTracker tracker = hero.buff(BeaconOfReturning.BeaconTracker.class);
			if (tracker != null){
				Notes.remove(Notes.Landmark.BEACON_LOCATION, tracker.returnDepth);
				tracker.detach();
			}
		}
		beacon.detach(hero.belongings.backpack);
		Item.updateQuickslot();
		GLog.i(Messages.get(Angela.class, "elevator_ride", depth));
		Sample.INSTANCE.play(Assets.Sounds.BEACON);

		Level.beforeTransition();
		Invisibility.dispel();
		InterlevelScene.mode = InterlevelScene.Mode.RETURN;
		InterlevelScene.returnDepth = depth;
		InterlevelScene.returnBranch = 0;
		InterlevelScene.returnPos = Quest.BESIDE_ANGELA;
		Game.switchScene(InterlevelScene.class);
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

	//older saves only stored whether she offered an ordeal at all
	private static final String OFFERS_ORDEAL = "offers_ordeal";
	private static final String ORDEAL_TIER   = "ordeal_tier";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(ORDEAL_TIER, ordealTier);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		if (bundle.contains(ORDEAL_TIER)){
			ordealTier = bundle.getInt(ORDEAL_TIER);
			//the trader of floor 16 from before the Dusk ordeals now offers them
			if (ordealTier == NONE && Dungeon.depth == 16 && Dungeon.branch == 0) ordealTier = DUSK;
		} else if (!bundle.contains(OFFERS_ORDEAL) || bundle.getBoolean(OFFERS_ORDEAL)){
			ordealTier = DAWN;
		} else {
			//the trader of floor 11 from before the Noon ordeals now offers them
			ordealTier = Dungeon.depth == 11 ? NOON : NONE;
		}
	}

	private void ordeal( int tier ){
		if (Quest.given(tier)){
			yell(Messages.get(Angela.class, "in_progress", Quest.ordealName(tier)));
			return;
		}
		//the Amber Dusk is fought at L Corp headquarters
		String offer = Messages.get(Angela.class, tier == DUSK && Quest.duskType == Quest.DUSK_AMBER ? "offer_hq" : "offer",
				Quest.ordealName(tier), Quest.reward(tier));
		GameScene.show(new WndOptions(
				sprite(),
				Messages.titleCase(name()),
				offer,
				Messages.get(Angela.class, "accept"),
				Messages.get(Angela.class, "decline")){
			@Override
			protected void onSelect( int index ) {
				if (index == 0){
					Quest.start(tier);
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

		//the Noon ordeals of floor 11
		public static final int NOON_GREEN   = 1; //Process of Understanding
		public static final int NOON_VIOLET  = 2; //Grant Us Love, falling into the hero's room
		public static final int NOON_INDIGO  = 3; //Sweepers x3, as a pack
		public static final int NOON_CRIMSON = 4; //Harmony of Skin, splitting into two Cheers for the Beginning

		//the Dusk ordeals of floor 16
		public static final int DUSK_GREEN   = 1; //Where We Must Reach, calling up Doubts and Processes of Understanding
		public static final int DUSK_CRIMSON = 2; //Throes of Climax, splitting into two Harmonies of Skin
		public static final int DUSK_AMBER   = 3; //Food Chains x2, in the corridors of L Corp headquarters

		public static final int FOOD_CHAINS = 2;

		//enkephalin boxes given for overcoming a Dawn, a Noon or a Dusk ordeal
		public static final int ORDEAL_REWARD = 10;
		public static final int NOON_REWARD   = 20;
		public static final int DUSK_REWARD   = 30;

		private static boolean spawned;
		private static int type;
		private static boolean given;
		private static boolean completed;
		private static int noonType;
		private static boolean noonGiven;
		private static boolean noonCompleted;
		private static int duskType;
		private static boolean duskGiven;
		private static boolean duskCompleted;
		//the Amber Dusk takes the hero to L Corp headquarters, the Food Chains come out once they arrive
		private static boolean duskPending;
		//the boss fight the hero is on the way to, it comes out once they arrive at L Corp headquarters
		private static String bossPending = null;
		//each of Angela's services can be used once per game
		private static boolean extracted;
		private static boolean refined;

		public static void reset(){
			spawned = false;
			type = 0;
			given = false;
			completed = false;
			noonType = 0;
			noonGiven = false;
			noonCompleted = false;
			duskType = 0;
			duskGiven = false;
			duskCompleted = false;
			duskPending = false;
			bossPending = null;
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
		private static final String NOON_TYPE      = "noon_type";
		private static final String NOON_GIVEN     = "noon_given";
		private static final String NOON_COMPLETED = "noon_completed";
		private static final String DUSK_TYPE      = "dusk_type";
		private static final String DUSK_GIVEN     = "dusk_given";
		private static final String DUSK_COMPLETED = "dusk_completed";
		private static final String DUSK_PENDING   = "dusk_pending";
		private static final String BOSS_PENDING   = "boss_pending";

		public static void storeInBundle( Bundle bundle ){
			Bundle node = new Bundle();
			node.put(SPAWNED, spawned);
			node.put(TYPE, type);
			node.put(GIVEN, given);
			node.put(COMPLETED, completed);
			node.put(EXTRACTED, extracted);
			node.put(REFINED, refined);
			node.put(NOON_TYPE, noonType);
			node.put(NOON_GIVEN, noonGiven);
			node.put(NOON_COMPLETED, noonCompleted);
			node.put(DUSK_TYPE, duskType);
			node.put(DUSK_GIVEN, duskGiven);
			node.put(DUSK_COMPLETED, duskCompleted);
			node.put(DUSK_PENDING, duskPending);
			if (bossPending != null) node.put(BOSS_PENDING, bossPending);
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
			noonType = node.getInt(NOON_TYPE);
			noonGiven = node.getBoolean(NOON_GIVEN);
			noonCompleted = node.getBoolean(NOON_COMPLETED);
			duskType = node.getInt(DUSK_TYPE);
			duskGiven = node.getBoolean(DUSK_GIVEN);
			duskCompleted = node.getBoolean(DUSK_COMPLETED);
			duskPending = node.getBoolean(DUSK_PENDING);
			bossPending = node.contains(BOSS_PENDING) ? node.getString(BOSS_PENDING) : null;
		}

		public static boolean extracted(){
			return extracted;
		}

		public static boolean refined(){
			return refined;
		}

		public static String ordealName(){
			return ordealName(DAWN);
		}

		public static String ordealName( int tier ){
			if (tier == DUSK) return Messages.get(Angela.class, "dusk_" + duskType);
			if (tier == NOON) return Messages.get(Angela.class, "noon_" + noonType);
			return Messages.get(Angela.class, "ordeal_" + type);
		}

		//the Noon ordeal waits until the Dawn one has been overcome, the Dusk one until the Noon one has
		public static boolean unlocked( int tier ){
			if (tier == DUSK) return noonCompleted;
			return tier != NOON || completed;
		}

		public static boolean given( int tier ){
			if (tier == DUSK) return duskGiven;
			return tier == NOON ? noonGiven : given;
		}

		public static boolean completed( int tier ){
			if (tier == DUSK) return duskCompleted;
			return tier == NOON ? noonCompleted : completed;
		}

		public static int reward( int tier ){
			if (tier == DUSK) return DUSK_REWARD;
			return tier == NOON ? NOON_REWARD : ORDEAL_REWARD;
		}

		//places Angela near the entrance stairs of floor 6 while it is being built, with her ordeal
		public static void spawn( Level level, Room entranceRoom ){
			if (spawned) return;
			if (placeNearEntrance(level, entranceRoom, DAWN) == null) return;

			spawned = true;
			type = Random.Int(4) + 1;
			given = false;
			completed = false;
		}

		//places an Angela with the Noon ordeal near the entrance stairs of floor 11
		public static void spawnNoon( Level level, Room entranceRoom ){
			if (placeNearEntrance(level, entranceRoom, NOON) == null) return;
			noonType = Random.Int(4) + 1;
			noonGiven = false;
			noonCompleted = false;
		}

		//places an Angela with the Dusk ordeal near the entrance stairs of floor 16
		public static void spawnDusk( Level level, Room entranceRoom ){
			if (placeNearEntrance(level, entranceRoom, DUSK) == null) return;
			duskType = Random.Int(3) + 1;
			duskGiven = false;
			duskCompleted = false;
			duskPending = false;
		}

		//places an Angela without an ordeal near the entrance stairs of floor 21
		public static void spawnTrader( Level level, Room entranceRoom ){
			placeNearEntrance(level, entranceRoom, NONE);
		}

		//places the Angela of floor 26 on the given cell, or next to it if someone already stands there
		public static void spawnBossGate( Level level, int cell ){
			for (Mob m : level.mobs){
				if (m instanceof Angela) return;
			}
			int pos = cell;
			if (level.findMob(pos) != null || (Dungeon.hero != null && Dungeon.hero.pos == pos)){
				pos = -1;
				for (int n : PathFinder.NEIGHBOURS8){
					int c = cell + n;
					if (level.passable[c] && !level.solid[c] && level.findMob(c) == null
							&& (Dungeon.hero == null || Dungeon.hero.pos != c)){
						pos = c;
						break;
					}
				}
				if (pos == -1) return;
			}
			Angela angela = new Angela();
			angela.ordealTier = BOSS_GATE;
			angela.pos = pos;
			level.mobs.add(angela);
		}

		private static Angela placeNearEntrance( Level level, Room entranceRoom, int ordealTier ){
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
			angela.ordealTier = ordealTier;
			angela.pos = Random.element(near.isEmpty() ? inRoom : near);
			level.mobs.add(angela);
			return angela;
		}

		//the hero's position when arriving by her elevator, Dungeon.switchLevel turns it into a cell beside her
		public static final int BESIDE_ANGELA = -3;

		//a free cell next to the Angela on this level, or -1 (the entrance) if she isn't here
		public static int besideAngela( Level level ){
			for (Mob m : level.mobs){
				if (!(m instanceof Angela)) continue;
				ArrayList<Integer> cells = new ArrayList<>();
				for (int n : PathFinder.NEIGHBOURS8){
					int c = m.pos + n;
					if (c >= 0 && c < level.length() && level.passable[c] && !level.solid[c]
							&& level.findMob(c) == null && !level.invalidHeroPos(c)){
						cells.add(c);
					}
				}
				if (!cells.isEmpty()) return Random.element(cells);
			}
			return -1;
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

		public static void start( int tier ){
			if (tier == DUSK){
				startDusk();
				return;
			}
			if (tier == NOON){
				startNoon();
				return;
			}
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
			showOrdealText(DAWN, "start_" + type);
		}

		private static void startNoon(){
			noonGiven = true;
			GLog.n(ordealName(NOON));

			Level level = Dungeon.level;
			switch (noonType){
				case NOON_GREEN: default:
					ProcessOfUnderstanding process = new ProcessOfUnderstanding();
					place(process, level.randomRespawnCell(process));
					break;
				case NOON_VIOLET:
					//it falls into the room the hero is standing in
					GrantUsLove.warn(level, Dungeon.hero.pos);
					break;
				case NOON_INDIGO:
					//the pack comes out together, somewhere away from the hero
					Sweeper first = new Sweeper();
					place(first, level.randomRespawnCell(first));
					for (int i = 1; i < Sweeper.COUNT; i++){
						Sweeper next = new Sweeper();
						int cell = -1;
						if (first.pos >= 0){
							ArrayList<Integer> near = new ArrayList<>();
							for (int n : PathFinder.NEIGHBOURS8){
								int c = first.pos + n;
								if (level.passable[c] && !level.solid[c] && Actor.findChar(c) == null) near.add(c);
							}
							if (!near.isEmpty()) cell = Random.element(near);
						}
						place(next, cell == -1 ? level.randomRespawnCell(next) : cell);
					}
					break;
				case NOON_CRIMSON:
					HarmonyOfSkin harmony = new HarmonyOfSkin();
					place(harmony, level.randomRespawnCell(harmony));
					break;
			}

			showOrdealText(NOON, "noon_start_" + noonType);
		}

		private static void startDusk(){
			duskGiven = true;

			Level level = Dungeon.level;
			switch (duskType){
				case DUSK_GREEN: default:
					WhereWeMustReach tower = new WhereWeMustReach();
					place(tower, farCell(level, tower));
					break;
				case DUSK_CRIMSON:
					ClimaxThroes throes = new ClimaxThroes();
					place(throes, level.randomRespawnCell(throes));
					break;
				case DUSK_AMBER:
					//off to L Corp headquarters, where the Food Chains wait in the corridors
					duskPending = true;
					Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
					Level.beforeTransition();
					Invisibility.dispel();
					InterlevelScene.curTransition = new LevelTransition(level, Dungeon.hero.pos,
							LevelTransition.Type.BRANCH_EXIT, Dungeon.depth, 2, LevelTransition.Type.BRANCH_ENTRANCE);
					InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
					Game.switchScene(InterlevelScene.class);
					return;
			}

			GLog.n(ordealName(DUSK));
			showOrdealText(DUSK, "dusk_start_" + duskType);
		}

		//a free cell some way from the hero, for an ordeal that is meant to be found
		private static int farCell( Level level, Mob mob ){
			for (int i = 0; i < 50; i++){
				int cell = level.randomRespawnCell(mob);
				if (cell != -1 && level.distance(cell, Dungeon.hero.pos) >= 8) return cell;
			}
			return level.randomRespawnCell(mob);
		}

		//the Food Chains come out once the hero has arrived at L Corp headquarters, which stays sealed until they fall.
		// Called while the floor is being switched to, before its actors start
		public static void beforeArrival( Level level ){
			if (bossPending != null && level instanceof LCorpHQLevel){
				if (bossPending.equals(BLUE_STAR)) BlueStar.spawn((LCorpHQLevel) level);
				bossArrival = bossPending;
				bossPending = null;
				return;
			}
			if (!duskPending || !(level instanceof LCorpHQLevel)) return;
			duskPending = false;
			ArrayList<Integer> corridors = new ArrayList<>();
			for (int i = 0; i < LCorpHQLevel.corridors().size(); i++) corridors.add(i);
			Random.shuffle(corridors);
			int placed = 0;
			for (int c : corridors){
				if (placed >= FOOD_CHAINS) break;
				if (FoodChain.spawn((LCorpHQLevel) level, c, Dungeon.hero.pos)) placed++;
			}
			level.locked = true;
			arrivalText = true;
		}

		private static boolean arrivalText = false;
		private static String bossArrival = null;

		//off to L Corp headquarters for one of the boss fights of floor 26
		public static void startBossFight( String boss ){
			Level level = Dungeon.level;
			if (level.locked || !Dungeon.hero.isAlive()) return;
			bossPending = boss;
			Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
			Level.beforeTransition();
			Invisibility.dispel();
			InterlevelScene.curTransition = new LevelTransition(level, Dungeon.hero.pos,
					LevelTransition.Type.BRANCH_EXIT, Dungeon.depth, 2, LevelTransition.Type.BRANCH_ENTRANCE);
			InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
			Game.switchScene(InterlevelScene.class);
		}

		//once the floor's actors are running, the Amber Dusk's words (or the boss fight's) are shown
		public static void afterArrival(){
			if (bossArrival != null){
				String boss = bossArrival;
				bossArrival = null;
				Actor.add(new Actor(){
					{
						actPriority = VFX_PRIO;
					}

					@Override
					protected boolean act() {
						Actor.remove(this);
						for (Mob m : Dungeon.level.mobs){
							if (m instanceof BlueStar) BossHealthBar.assignBoss(m);
						}
						Game.runOnRenderThread(() -> GameScene.show(new WndTitledMessage(new BlueStarSprite(),
								Messages.get(Angela.class, "boss_" + boss), Messages.get(Angela.class, "boss_start_" + boss))));
						return true;
					}
				});
				return;
			}
			if (!arrivalText) return;
			arrivalText = false;
			Actor.add(new Actor(){
				{
					actPriority = VFX_PRIO;
				}

				@Override
				protected boolean act() {
					Actor.remove(this);
					GLog.n(ordealName(DUSK));
					showOrdealText(DUSK, "dusk_start_" + duskType);
					return true;
				}
			});
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

		public static void place( Mob mob, int cell ){
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

		//called whenever an ordeal monster dies, the ordeal ends once none of its own are left on the floor
		public static void onOrdealMobSlain( int tier ){
			if (!given(tier) || completed(tier)) return;
			for (Mob m : Dungeon.level.mobs){
				if (m instanceof OrdealMob && ((OrdealMob) m).tier() == tier && m.isAlive()){
					return;
				}
			}
			int reward = reward(tier);
			if (tier == DUSK) duskCompleted = true;
			else if (tier == NOON) noonCompleted = true;
			else completed = true;
			//L Corp headquarters opens again once the Food Chains have fallen
			if (tier == DUSK && Dungeon.level instanceof LCorpHQLevel) Dungeon.level.locked = false;
			Warehouse.addEnkephalin(reward);
			GLog.p(Messages.get(Angela.class, "reward", reward, Warehouse.enkephalin()));
			showOrdealText(tier, tier == DUSK ? "dusk_end_" + duskType : tier == NOON ? "noon_end_" + noonType : "end_" + type);
		}

		private static void showOrdealText( int tier, String key ){
			String title = ordealName(tier);
			String text = Messages.get(Angela.class, key);
			int t = tier == DUSK ? duskType : tier == NOON ? noonType : type;
			Game.runOnRenderThread(() -> {
				CharSprite icon;
				if (tier == DUSK){
					switch (t){
						case DUSK_GREEN: default: icon = new WhereWeMustReachSprite(); break;
						case DUSK_CRIMSON: icon = new ClimaxThroesSprite(); break;
						case DUSK_AMBER: icon = new FoodChainSprite(); break;
					}
				} else if (tier == NOON){
					switch (t){
						case NOON_GREEN: default: icon = new ProcessOfUnderstandingSprite(); break;
						case NOON_VIOLET: icon = new GrantUsLoveSprite(); break;
						case NOON_INDIGO: icon = new SweeperSprite(); break;
						case NOON_CRIMSON: icon = new HarmonyOfSkinSprite(); break;
					}
				} else {
					switch (t){
						case GREEN: default: icon = new DoubtSprite(); break;
						case VIOLET: icon = new FruitOfUnderstandingSprite(); break;
						case AMBER: icon = new PerfectFoodSprite(); break;
						case CRIMSON: icon = new CheersForTheBeginningSprite(); break;
					}
				}
				GameScene.show(new WndTitledMessage(icon, title, text));
			});
		}
	}
}

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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
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
import com.shatteredpixel.shatteredpixeldungeon.sprites.DoubtSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.FruitOfUnderstandingSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.PerfectFoodSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Angela, the AI secretary of L Corp. She waits by the stairs of floor 6, can't be harmed,
//and offers the hero one of three Dawn ordeals, chosen at random when she appears.
public class Angela extends NPC {

	{
		spriteClass = AngelaSprite.class;

		properties.add(Property.IMMOVABLE);
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

		//an Angela summoned by the Tester picks her ordeal on the spot
		if (Quest.type == 0){
			Quest.spawned = true;
			Quest.type = Random.Int(3) + 1;
		}

		if (!Quest.given){
			Game.runOnRenderThread(() -> GameScene.show(new WndOptions(
					sprite(),
					Messages.titleCase(name()),
					Messages.get(Angela.class, "offer", Quest.ordealName()),
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
			}));
		} else if (!Quest.completed){
			yell(Messages.get(Angela.class, "in_progress", Quest.ordealName()));
		} else {
			yell(Messages.get(Angela.class, "done"));
		}
		return true;
	}

	public static class Quest {

		public static final int GREEN   = 1; //Doubt
		public static final int VIOLET  = 2; //Fruit of Understanding
		public static final int AMBER   = 3; //Perfect Food x4, in the corridors

		public static final int AMBER_COUNT = 4;

		private static boolean spawned;
		private static int type;
		private static boolean given;
		private static boolean completed;

		public static void reset(){
			spawned = false;
			type = 0;
			given = false;
			completed = false;
		}

		private static final String NODE      = "angela";
		private static final String SPAWNED   = "spawned";
		private static final String TYPE      = "type";
		private static final String GIVEN     = "given";
		private static final String COMPLETED = "completed";

		public static void storeInBundle( Bundle bundle ){
			Bundle node = new Bundle();
			node.put(SPAWNED, spawned);
			node.put(TYPE, type);
			node.put(GIVEN, given);
			node.put(COMPLETED, completed);
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
		}

		public static String ordealName(){
			return Messages.get(Angela.class, "ordeal_" + type);
		}

		//places Angela near the entrance stairs of floor 6 while it is being built
		public static void spawn( Level level, Room entranceRoom ){
			if (spawned || entranceRoom == null) return;

			Angela angela = new Angela();
			int entrance = level.entrance();

			ArrayList<Integer> near = new ArrayList<>();
			ArrayList<Integer> inRoom = new ArrayList<>();
			for (Point p : entranceRoom.getPoints()){
				int cell = level.pointToCell(p);
				if (!entranceRoom.inside(p) || cell == entrance || !level.passable[cell] || level.solid[cell]
						|| level.traps.get(cell) != null || level.map[cell] == Terrain.EMPTY_SP){
					continue;
				}
				boolean byDoor = false;
				for (int i : PathFinder.NEIGHBOURS4){
					if (level.map[cell + i] == Terrain.DOOR) byDoor = true;
				}
				if (byDoor) continue;
				inRoom.add(cell);
				if (level.distance(cell, entrance) <= 2) near.add(cell);
			}
			if (near.isEmpty() && inRoom.isEmpty()) return;

			angela.pos = Random.element(near.isEmpty() ? inRoom : near);
			level.mobs.add(angela);

			spawned = true;
			type = Random.Int(3) + 1;
			given = false;
			completed = false;
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
				}
				GameScene.show(new WndTitledMessage(icon, title, text));
			});
		}
	}
}

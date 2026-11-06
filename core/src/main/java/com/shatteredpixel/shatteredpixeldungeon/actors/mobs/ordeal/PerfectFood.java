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


package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.LCorpHQLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.connection.ConnectionRoom;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.sprites.PerfectFoodSprite;
import com.watabou.utils.BArray;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Dawn of Amber: larvae that crawl through the corridors and never leave them
public class PerfectFood extends OrdealMob {

	{
		spriteClass = PerfectFoodSprite.class;

		HP = HT = 35;
		defenseSkill = 15;
		EXP = 3;

		attackType = DamageType.RED;
		damageMultipliers = new float[]{2.0f, 1.0f, 1.0f, 2.0f};

		WANDERING = new Wandering();
		state = WANDERING;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(2, 8);
	}

	@Override
	public int attackSkill( Char target ) {
		return 12;
	}

	//the passable cells of the level's corridors (connection rooms, doors included).
	//A level without corridors returns null, and the larvae can then go anywhere.
	public static boolean[] corridors( Level level ){
		//L Corp headquarters has its corridors between the rooms of the sephirot
		if (level instanceof LCorpHQLevel){
			boolean[] result = new boolean[level.length()];
			for (int[] c : LCorpHQLevel.corridors()){
				for (int x = c[0]; x < c[0] + c[2]; x++){
					for (int y = c[1]; y < c[1] + c[3]; y++){
						result[x + y*level.width()] = true;
					}
				}
			}
			return result;
		}
		if (!(level instanceof RegularLevel)) return null;
		boolean[] result = new boolean[level.length()];
		boolean any = false;
		for (Room r : ((RegularLevel) level).rooms()){
			if (!(r instanceof ConnectionRoom)) continue;
			for (int x = r.left; x <= r.right; x++){
				for (int y = r.top; y <= r.bottom; y++){
					int cell = x + y*level.width();
					if (cell >= 0 && cell < level.length() && level.passable[cell]){
						result[cell] = true;
						any = true;
					}
				}
			}
		}
		return any ? result : null;
	}

	private boolean[] corridors;

	private boolean[] corridors(){
		if (corridors == null || corridors.length != Dungeon.level.length()){
			corridors = corridors(Dungeon.level);
			if (corridors == null){
				corridors = Dungeon.level.passable.clone();
			}
		}
		return corridors;
	}

	@Override
	protected boolean getCloser( int target ) {
		if (rooted) return false;
		int step = Dungeon.findStep( this, target, BArray.and(corridors(), Dungeon.level.passable, null), fieldOfView, true );
		if (step != -1){
			move( step );
			return true;
		}
		return false;
	}

	@Override
	protected boolean getFurther( int target ) {
		if (rooted) return false;
		int step = Dungeon.flee( this, target, BArray.and(corridors(), Dungeon.level.passable, null), fieldOfView, true );
		if (step != -1){
			move( step );
			return true;
		}
		return false;
	}

	private class Wandering extends Mob.Wandering {
		@Override
		protected int randomDestination() {
			boolean[] mask = corridors();
			ArrayList<Integer> cells = new ArrayList<>();
			for (int i = 0; i < mask.length; i++){
				if (mask[i]) cells.add(i);
			}
			return cells.isEmpty() ? super.randomDestination() : Random.element(cells);
		}
	}
}

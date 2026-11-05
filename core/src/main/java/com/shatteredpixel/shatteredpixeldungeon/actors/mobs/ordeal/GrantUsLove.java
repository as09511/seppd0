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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Angela;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GrantUsLoveSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Noon of Violet: a monolith of glowing runes that falls from above. The whole room is marked for 5 turns,
//then it lands, striking everything left in the room. It never moves, and its tentacles reach 2 tiles.
public class GrantUsLove extends OrdealMob {

	public static final int FALL_TURNS  = 5;
	public static final int FALL_DAMAGE = 100;
	public static final int REACH       = 2;

	{
		spriteClass = GrantUsLoveSprite.class;

		HP = HT = 350;
		defenseSkill = 0;
		EXP = 22;

		tier = Angela.NOON;
		maxLvl = NOON_MAX_LVL;
		risk = RiskLevel.HE;

		attackType = DamageType.BLACK;
		damageMultipliers = new float[]{0.8f, 2.0f, 0.8f, 1.0f};

		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(5, 15);
	}

	@Override
	public int attackSkill( Char target ) {
		return 25;
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return Dungeon.level.distance(pos, enemy.pos) <= REACH
				&& new Ballistica(pos, enemy.pos, Ballistica.PROJECTILE).collisionPos == enemy.pos;
	}

	@Override
	protected boolean getCloser( int target ) {
		return false;
	}

	@Override
	protected boolean getFurther( int target ) {
		return false;
	}

	//marks the room around the given cell, where the monolith will land in a few turns
	public static void warn( Level level, int around ){
		ArrayList<Integer> cells = fallArea(level, around);
		Fall fall = null;
		for (int cell : cells){
			fall = Blob.seed(cell, FALL_TURNS, Fall.class, level);
			GameScene.targetedCell(cell, FALL_TURNS);
		}
		if (fall != null){
			fall.center = center(level, around, cells);
			GameScene.add(fall);
		}
		GLog.n(Messages.get(GrantUsLove.class, "warn"));
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 0.6f);
	}

	//the room the cell is in, or the tiles around it outside of rooms
	private static ArrayList<Integer> fallArea( Level level, int around ){
		ArrayList<Integer> cells = new ArrayList<>();
		Room room = level instanceof RegularLevel ? ((RegularLevel) level).room(around) : null;
		if (room != null){
			for (Point p : room.getPoints()){
				int cell = level.pointToCell(p);
				if (room.inside(p) && !level.solid[cell]) cells.add(cell);
			}
		}
		if (cells.isEmpty()){
			for (int cell = 0; cell < level.length(); cell++){
				if (!level.solid[cell] && level.distance(cell, around) <= 3) cells.add(cell);
			}
		}
		return cells;
	}

	private static int center( Level level, int around, ArrayList<Integer> cells ){
		Room room = level instanceof RegularLevel ? ((RegularLevel) level).room(around) : null;
		if (room != null){
			int c = level.pointToCell(room.center());
			if (cells.contains(c)) return c;
		}
		return around;
	}

	//the warning: every marked cell counts down, and the monolith lands when they run out
	public static class Fall extends Blob {

		private int center = -1;

		@Override
		protected void evolve() {
			boolean land = false;
			int cell;
			for (int i = area.top - 1; i <= area.bottom; i++){
				for (int j = area.left - 1; j <= area.right; j++){
					cell = j + i*Dungeon.level.width();
					if (!Dungeon.level.insideMap(cell)) continue;
					if (cur[cell] > 0){
						off[cell] = cur[cell] - 1;
						volume += off[cell];
						if (off[cell] == 0) land = true;
					} else {
						off[cell] = 0;
					}
				}
			}
			if (land) land();
		}

		private void land(){
			ArrayList<Integer> area = new ArrayList<>();
			for (int i = 0; i < cur.length; i++){
				if (cur[i] > 0) area.add(i);
			}

			GrantUsLove love = new GrantUsLove();
			int spot = landingCell(area);

			//everyone still in the room is crushed
			for (Char ch : Actor.chars().toArray(new Char[0])){
				if (!ch.isAlive() || !area.contains(ch.pos)) continue;
				int dmg = DamageType.apply(ch, DamageType.RED, FALL_DAMAGE);
				ch.damage(dmg, love);
				if (ch == Dungeon.hero && !ch.isAlive()){
					Dungeon.fail(love);
					GLog.n(Messages.get(GrantUsLove.class, "crushed"));
				}
			}

			for (int cell : area){
				if (Dungeon.level.heroFOV[cell] && Random.Int(3) == 0){
					CellEmitter.get(cell).burst(Speck.factory(Speck.ROCK), 2);
				}
			}
			PixelScene.shake(4, 0.4f);
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.7f);
			Sample.INSTANCE.play(Assets.Sounds.ROCKS);

			if (spot != -1){
				love.pos = spot;
				GameScene.add(love);
				love.sprite.parent.add(new com.watabou.noosa.tweeners.AlphaTweener(love.sprite, 1f, 0.2f));
				love.sprite.alpha(0);
				GLog.w(Messages.get(GrantUsLove.class, "landed"));
			}
			center = -1;
		}

		private int landingCell( ArrayList<Integer> area ){
			ArrayList<Integer> free = new ArrayList<>();
			for (int cell : area){
				if (Actor.findChar(cell) == null && Dungeon.level.passable[cell]) free.add(cell);
			}
			if (free.isEmpty()){
				//the room is packed, it lands wherever there is space nearby
				for (int cell : area){
					for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8){
						int c = cell + n;
						if (Dungeon.level.insideMap(c) && Actor.findChar(c) == null && Dungeon.level.passable[c]) return c;
					}
				}
				return -1;
			}
			if (free.contains(center)) return center;
			int best = free.get(0);
			for (int cell : free){
				if (center != -1 && Dungeon.level.trueDistance(cell, center) < Dungeon.level.trueDistance(best, center)) best = cell;
			}
			return best;
		}

		@Override
		public String tileDesc() {
			return Messages.get(GrantUsLove.class, "fall_desc");
		}

		private static final String CENTER = "center";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(CENTER, center);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			center = bundle.contains(CENTER) ? bundle.getInt(CENTER) : -1;
		}
	}
}

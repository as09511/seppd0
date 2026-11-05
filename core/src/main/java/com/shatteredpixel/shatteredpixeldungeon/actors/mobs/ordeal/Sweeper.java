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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Angela;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SweeperSprite;
import com.watabou.utils.Random;

//Noon of Indigo: sweepers of the backstreets that come out at night. They roam as a pack,
//and whenever someone dies near one of them it feeds on the remains, healing 30% of its health.
public class Sweeper extends OrdealMob {

	public static final int COUNT = 3;
	public static final int FEED_RANGE = 3;
	public static final float FEED_HEAL = 0.3f;

	{
		spriteClass = SweeperSprite.class;

		HP = HT = 100;
		defenseSkill = 15;
		EXP = 8;

		tier = Angela.NOON;
		maxLvl = NOON_MAX_LVL;
		risk = RiskLevel.HE;

		attackType = DamageType.BLACK;
		damageMultipliers = new float[]{1.0f, 1.2f, 0.5f, 0.8f};

		WANDERING = new Pack();
		state = WANDERING;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(10, 20);
	}

	@Override
	public int attackSkill( Char target ) {
		return 20;
	}

	//called whenever a character dies, every other sweeper close by feeds on it
	public static void onDeath( Char dead ){
		if (Dungeon.level == null) return;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (!(m instanceof Sweeper) || m == dead || !m.isAlive()) continue;
			if (Dungeon.level.distance(m.pos, dead.pos) > FEED_RANGE) continue;
			int heal = Math.min(Math.round(m.HT * FEED_HEAL), m.HT - m.HP);
			if (heal <= 0) continue;
			m.HP += heal;
			if (m.sprite != null && m.sprite.visible){
				m.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
				m.sprite.emitter().burst(Speck.factory(Speck.HEALING), 3);
			}
		}
	}

	//the first of the pack still standing leads it, the rest keep close to it
	private Sweeper leader(){
		Sweeper leader = null;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof Sweeper && m.isAlive() && (leader == null || m.id() < leader.id())){
				leader = (Sweeper) m;
			}
		}
		return leader;
	}

	//one sweeper that spots its prey calls the whole pack to it
	private void alertPack( Char enemy ){
		if (enemy == null) return;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m instanceof Sweeper && m != this && m.isAlive() && m.state != m.HUNTING){
				m.aggro(enemy);
				((Sweeper) m).target = enemy.pos;
			}
		}
	}

	@Override
	public void aggro( Char ch ) {
		boolean was = state == HUNTING;
		super.aggro(ch);
		if (!was && state == HUNTING) alertPack(ch);
	}

	private class Pack extends Wandering {

		@Override
		protected boolean noticeEnemy() {
			boolean result = super.noticeEnemy();
			alertPack(enemy);
			return result;
		}

		@Override
		protected boolean continueWandering() {
			Sweeper leader = leader();
			if (leader != null && leader != Sweeper.this){
				if (Dungeon.level.distance(pos, leader.pos) > 2){
					target = leader.pos;
				} else if (leader.target != -1 && leader.target != pos){
					target = leader.target;
				}
			}
			return super.continueWandering();
		}
	}
}

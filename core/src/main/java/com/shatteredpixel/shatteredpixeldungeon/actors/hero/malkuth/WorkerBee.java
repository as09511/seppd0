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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.malkuth;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bee;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.watabou.utils.Bundle;

//an allied bee raised by the queen bee talent from an enemy that bled to death after a hornet dash
//borrows the regular bee's sprite for now, and only lasts a limited time
public class WorkerBee extends Bee {

	{
		alignment = Alignment.ALLY;
		state = HUNTING;
	}

	public static final float LIFESPAN = 20f;

	public static WorkerBee spawnAt( int pos ){
		if (Actor.findChar(pos) != null || !Dungeon.level.passable[pos]) return null;
		WorkerBee bee = new WorkerBee();
		bee.spawn(Dungeon.scalingDepth());
		bee.HP = bee.HT;
		bee.setPotInfo(-1, null);
		bee.pos = pos;
		GameScene.add(bee);
		Buff.affect(bee, Lifespan.class, LIFESPAN);
		Dungeon.level.occupyCell(bee);
		return bee;
	}

	//L Corp's employees and worker bees deal RED damage; notebook bonus damage stays untyped
	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = DamageType.apply(enemy, DamageType.RED, damage);
		return super.attackProc(enemy, damage);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		alignment = Alignment.ALLY;
	}

	public static class Lifespan extends FlavourBuff {

		{
			type = buffType.NEUTRAL;
		}

		@Override
		public boolean act() {
			if (target.isAlive()) target.die(null);
			return super.act();
		}
	}
}

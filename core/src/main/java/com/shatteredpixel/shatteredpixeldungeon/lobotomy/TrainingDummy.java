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


package com.shatteredpixel.shatteredpixeldungeon.lobotomy;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SheepSprite;

//testing target with fixed damage multipliers, it never acts
public abstract class TrainingDummy extends Mob {

	{
		spriteClass = SheepSprite.class;

		HP = HT = 100;
		defenseSkill = 0;
		EXP = 0;
		maxLvl = -2;

		state = PASSIVE;
	}

	@Override
	protected boolean act() {
		enemySeen = true;
		spend( TICK );
		return true;
	}

	@Override
	public int damageRoll() {
		return 0;
	}

	@Override
	public int attackSkill( Char target ) {
		return 0;
	}

	//0.5 to every damage type
	public static class Resistant extends TrainingDummy {
		{
			damageMultipliers = new float[]{0.5f, 0.5f, 0.5f, 0.5f};
		}
	}

	//1.5 to every damage type
	public static class Weak extends TrainingDummy {
		{
			damageMultipliers = new float[]{1.5f, 1.5f, 1.5f, 1.5f};
		}
	}

	//2.0 to PALE only
	public static class PaleWeak extends TrainingDummy {
		{
			damageMultipliers = new float[]{1f, 1f, 1f, 2f};
		}
	}

}

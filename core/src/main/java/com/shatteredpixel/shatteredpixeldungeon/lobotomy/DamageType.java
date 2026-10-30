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
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

//Lobotomy Corporation's four damage types. Each Char takes a multiplier of the final damage
//(after armor) for each type, e.g. 1.5x RED / 1.0x WHITE / 0.5x BLACK / 2.0x PALE.
//Damage without a type, such as from regular non-E.G.O weapons, is unaffected.
public enum DamageType {
	RED, WHITE, BLACK, PALE;

	public static float[] neutralMultipliers(){
		return new float[]{1f, 1f, 1f, 1f};
	}

	public static int apply(Char defender, DamageType type, int damage){
		if (type == null) return damage;
		return Math.max(0, Math.round(damage * defender.damageMultiplier(type)));
	}

	public static boolean isNeutral(float[] multipliers){
		for (float m : multipliers){
			if (m != 1f) return false;
		}
		return true;
	}

	//e.g. "RED x1.5 / WHITE x1 / BLACK x0.5 / PALE x2"
	public static String multipliersDesc(float[] multipliers){
		StringBuilder sb = new StringBuilder();
		for (DamageType t : values()){
			if (sb.length() > 0) sb.append(" / ");
			sb.append(t.name()).append(" x").append(Messages.decimalFormat("#.##", multipliers[t.ordinal()]));
		}
		return Messages.get(DamageType.class, "multipliers", sb.toString());
	}
}

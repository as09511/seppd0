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
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

//Lobotomy Corporation's four damage types. Each Char takes a multiplier of the final damage
//(after armor) for each type, e.g. 1.5x RED / 1.0x WHITE / 0.5x BLACK / 2.0x PALE.
//Damage without a type, such as from regular non-E.G.O weapons, is unaffected.
public enum DamageType {
	RED     (FloatingText.RED_DMG,   0xCA2844),
	WHITE   (FloatingText.WHITE_DMG, 0xEEECC0),
	BLACK   (FloatingText.BLACK_DMG, 0xA46EAC),
	PALE    (FloatingText.PALE_DMG,  0x40CDBE);

	private final int icon;
	private final int color;

	DamageType(int icon, int color){
		this.icon = icon;
		this.color = color;
	}

	//icon and number color of the damage popup
	public int icon(){
		return icon;
	}

	public int color(){
		return color;
	}

	public static float[] neutralMultipliers(){
		return new float[]{1f, 1f, 1f, 1f};
	}

	public static int apply(Char defender, DamageType type, int damage){
		if (type == null) return damage;
		defender.incomingDamageType = type;
		return Math.max(0, Math.round(damage * defender.damageMultiplier(type)));
	}

	public static boolean isNeutral(float[] multipliers){
		for (float m : multipliers){
			if (m != 1f) return false;
		}
		return true;
	}

	//e.g. "내성: RED 1.5 / WHITE 1.0 / BLACK 0.5 / PALE 2.0"
	public static String multipliersDesc(float[] multipliers){
		StringBuilder sb = new StringBuilder();
		for (DamageType t : values()){
			if (sb.length() > 0) sb.append(" / ");
			sb.append(t.name()).append(" ").append(Messages.decimalFormat("0.0#", multipliers[t.ordinal()]));
		}
		return Messages.get(DamageType.class, "resistance", sb.toString());
	}
}

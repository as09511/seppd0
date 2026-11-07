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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.EgoWeapon;

//the damage type of the hero's own melee attacks, which copies of the hero (mirror images, prismatic images,
//Tiphereth B...) deal as well: an E.G.O weapon's own type, RED bare-handed, none with any other weapon
public class HeroDamageType {

	public static DamageType of( Hero hero ){
		if (hero == null) return null;
		KindOfWeapon wep = hero.belongings.weapon();
		if (wep instanceof EgoWeapon) return ((EgoWeapon) wep).damageType();
		if (wep == null || RingOfForce.fightingUnarmed(hero)) return DamageType.RED;
		return null;
	}
}

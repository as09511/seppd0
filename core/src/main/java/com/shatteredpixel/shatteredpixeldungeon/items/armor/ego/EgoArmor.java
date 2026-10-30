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


package com.shatteredpixel.shatteredpixeldungeon.items.armor.ego;

import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;

//E.G.O armor, based on the suits extracted from abnormalities in Lobotomy Corporation
//The wearer takes damage of each type scaled by the armor's multiplier for that type
public abstract class EgoArmor extends Armor {

	//null for standard-issue equipment
	protected RiskLevel risk = null;
	//RED, WHITE, BLACK, PALE
	protected float[] damageMultipliers = DamageType.neutralMultipliers();

	public EgoArmor( int tier ) {
		super( tier );
	}

	public RiskLevel risk(){
		return risk;
	}

	public float damageMultiplier( DamageType type ){
		return damageMultipliers[type.ordinal()];
	}

	@Override
	public String info() {
		return super.info() + "\n\n" + RiskLevel.desc(risk) + "\n" + DamageType.multipliersDesc(damageMultipliers);
	}
}

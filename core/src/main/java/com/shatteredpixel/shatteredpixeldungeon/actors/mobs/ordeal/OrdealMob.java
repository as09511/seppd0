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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Angela;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;

//a monster summoned by one of Angela's ordeals. Its attacks deal a damage type,
//and the ordeal ends once the last of them on the floor is slain.
public abstract class OrdealMob extends Mob {

	{
		maxLvl = 14;
		state = WANDERING;
	}

	protected DamageType attackType = DamageType.RED;
	protected RiskLevel risk = RiskLevel.TETH;

	//which of Angela's ordeals this monster belongs to: Angela.DAWN, Angela.NOON or Angela.DUSK
	protected int tier = Angela.DAWN;

	public int tier(){
		return tier;
	}

	//a monster called up by one of a later ordeal (the Harmony of Skin's Cheers, the Doubts of Where We Must Reach...)
	// counts toward that ordeal instead
	public OrdealMob tier( int tier ){
		this.tier = tier;
		maxLvl = maxLvl(tier, maxLvl);
		return this;
	}

	public OrdealMob noon(){
		return tier(Angela.NOON);
	}

	//Noon and Dusk monsters still give experience to a hero a few levels further down
	public static final int NOON_MAX_LVL = 20;
	public static final int DUSK_MAX_LVL = 26;

	private static int maxLvl( int tier, int fallback ){
		if (tier == Angela.DUSK) return DUSK_MAX_LVL;
		if (tier == Angela.NOON) return NOON_MAX_LVL;
		return fallback;
	}

	@Override
	public String info() {
		return Messages.get(OrdealMob.class, "risk", risk.coloredTitle()) + "\n\n" + super.info();
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = DamageType.apply(enemy, attackType, damage);
		return super.attackProc(enemy, damage);
	}

	@Override
	public void die( Object cause ) {
		super.die(cause);
		Angela.Quest.onOrdealMobSlain(tier);
	}

	private static final String TIER = "ordeal_tier";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(TIER, tier);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		if (bundle.contains(TIER)) tier = bundle.getInt(TIER);
		maxLvl = maxLvl(tier, maxLvl);
	}
}

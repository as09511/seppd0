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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.hod.ManagerCall;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//what remains of the king's crown once it is worn: holds the armor ability's charge,
// so the hero's armor stays as it is. Works from the backpack, like Malkuth's notebook.
public class AbilityCrown extends Item {

	private static final String AC_ABILITY = "ABILITY";
	//Hod's emergency relocation: swap places with the manager
	private static final String AC_RELOCATE = "RELOCATE";

	{
		image = ItemSpriteSheet.CROWN;

		defaultAction = AC_ABILITY;

		unique = true;
		bones = false;
	}

	public float charge = 0;

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_ABILITY );
		if (ManagerCall.canRelocate(hero)){
			actions.add( AC_RELOCATE );
		}
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (hero.armorAbility != null && action.equals(AC_ABILITY)){
			return Messages.upperCase(hero.armorAbility.name());
		} else if (action.equals(AC_RELOCATE)){
			return Messages.get(this, "ac_relocate", Math.round(ManagerCall.relocateCost(hero)));
		} else {
			return super.actionName(action, hero);
		}
	}

	@Override
	public void execute( Hero hero, String action ) {

		super.execute( hero, action );

		if (action.equals(AC_ABILITY)){
			if (hero.armorAbility == null){
				usesTargeting = false;
				GLog.w( Messages.get(this, "no_ability") );
			} else if (charge < hero.armorAbility.chargeUse(hero)) {
				usesTargeting = false;
				GLog.w( Messages.get(this, "low_charge") );
			} else {
				usesTargeting = hero.armorAbility.useTargeting();
				hero.armorAbility.use(this, hero);
			}
		} else if (action.equals(AC_RELOCATE)){
			usesTargeting = false;
			if (ManagerCall.canRelocate(hero)){
				ManagerCall.relocate(this, hero);
			}
		}
	}

	@Override
	public int targetingPos(Hero user, int dst) {
		return user.armorAbility.targetedPos(user, dst);
	}

	@Override
	public String status() {
		return Messages.format( "%.0f%%", Math.floor(charge) );
	}

	@Override
	public String desc() {
		String desc = super.desc();

		if (Dungeon.hero != null && Dungeon.hero.belongings.contains(this)) {
			ArmorAbility ability = Dungeon.hero.armorAbility;
			if (ability != null) {
				desc += "\n\n" + ability.shortDesc();
				float chargeUse = ability.chargeUse(Dungeon.hero);
				//trinity has variable charge cost
				if (!(ability instanceof Trinity)) {
					desc += " " + Messages.get(this, "charge_use", Messages.decimalFormat("#.##", chargeUse));
				}
			} else {
				desc += "\n\n" + "_" + Messages.get(this, "no_ability") + "_";
			}
		}

		return desc;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 0;
	}

	private static final String CHARGE = "charge";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( CHARGE, charge );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		charge = bundle.getFloat( CHARGE );
	}

	//charges the carried crown, 500 turns to go from empty to full
	public static class Charger extends Buff {

		{
			type = buffType.NEUTRAL;
			revivePersists = true;
		}

		@Override
		public boolean act() {
			if (target instanceof Hero && Regeneration.regenOn()) {
				AbilityCrown crown = ((Hero) target).belongings.getItem(AbilityCrown.class);
				if (crown != null && crown.charge < 100) {
					float chargeGain = 100 / 500f;
					chargeGain *= RingOfEnergy.armorChargeMultiplier(target);
					crown.charge = Math.min(100, crown.charge + chargeGain);
					updateQuickslot();
				}
			}
			spend( TICK );
			return true;
		}
	}
}

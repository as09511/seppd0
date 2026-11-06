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
import com.shatteredpixel.shatteredpixeldungeon.QuickSlot;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.hod.ManagerCall;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.ArrayList;

//Hod's emergency relocation talent hands her this radio, so the swap with the manager can sit in a quickslot.
//It does the same as the forced weight's relocation button and spends the forced weight's charge.
public class RelocationRadio extends Item {

	private static final String AC_RELOCATE = "RELOCATE";

	{
		image = ItemSpriteSheet.RELOCATION_RADIO;

		defaultAction = AC_RELOCATE;

		unique = true;
		bones = false;
		keptThoughLostInvent = true;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.remove( AC_DROP );
		actions.remove( AC_THROW );
		actions.add( AC_RELOCATE );
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_RELOCATE)){
			return Messages.get(this, "ac_relocate", Math.round(ManagerCall.relocateCost(hero)));
		}
		return super.actionName(action, hero);
	}

	@Override
	public void execute( Hero hero, String action ) {

		super.execute( hero, action );

		if (action.equals(AC_RELOCATE)){
			AbilityCrown crown = hero.belongings.getItem(AbilityCrown.class);
			if (crown == null || !hero.hasTalent(Talent.EMERGENCY_RELOCATION)){
				GLog.w( Messages.get(this, "no_talent") );
			} else if (!ManagerCall.canRelocate(hero)){
				GLog.w( Messages.get(this, "no_manager") );
			} else {
				ManagerCall.relocate(crown, hero);
			}
		}
	}

	@Override
	public String status() {
		if (Dungeon.hero == null) return null;
		return Integer.toString(Math.round(ManagerCall.relocateCost(Dungeon.hero)));
	}

	@Override
	public String desc() {
		String desc = super.desc();
		if (Dungeon.hero != null && Dungeon.hero.hasTalent(Talent.EMERGENCY_RELOCATION)){
			desc += "\n\n" + Messages.get(this, "cost", Math.round(ManagerCall.relocateCost(Dungeon.hero)));
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

	//the radio is carried exactly while the talent is taken
	public static void sync( Hero hero, boolean toQuickslot ){
		RelocationRadio radio = hero.belongings.getItem(RelocationRadio.class);
		if (hero.hasTalent(Talent.EMERGENCY_RELOCATION)){
			if (radio == null){
				radio = new RelocationRadio();
				if (!radio.collect(hero.belongings.backpack)){
					hero.belongings.backpack.items.add(radio);
				}
				if (toQuickslot && !Dungeon.quickslot.contains(radio)){
					for (int i = 0; i < QuickSlot.SIZE; i++){
						if (Dungeon.quickslot.getItem(i) == null){
							Dungeon.quickslot.setSlot(i, radio);
							break;
						}
					}
				}
			}
		} else if (radio != null){
			Dungeon.quickslot.clearItem(radio);
			radio.detachAll(hero.belongings.backpack);
		}
		updateQuickslot();
	}
}

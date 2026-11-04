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


package com.shatteredpixel.shatteredpixeldungeon.actors.hero.yesod;


import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.CommissionTint;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Random;

//commission talent: one enemy on each floor is marked, killing it gives info and a guaranteed item
public class Commission extends Buff {

	//chance for the reward to be equipment instead of a consumable
	public static final float EQUIPMENT_CHANCE = 0.3f;

	{
		type = buffType.NEGATIVE;
		announced = true;
		//a dwarf ghoul that collapses beside another loses its buffs until it gets back up, the mark has to stay
		revivePersists = true;
	}

	//the target is picked while the level loads, before the game log exists, so it's announced on its first turn
	private boolean announcedTarget = false;

	@Override
	public boolean act() {
		if (!announcedTarget){
			announcedTarget = true;
			com.shatteredpixel.shatteredpixeldungeon.utils.GLog.i(Messages.get(Commission.class, "assigned", target.name()));
		}
		diactivate();
		return true;
	}

	private static final String ANNOUNCED = "announced";

	@Override
	public void storeInBundle(com.watabou.utils.Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(ANNOUNCED, announcedTarget);
	}

	@Override
	public void restoreFromBundle(com.watabou.utils.Bundle bundle) {
		super.restoreFromBundle(bundle);
		announcedTarget = bundle.getBoolean(ANNOUNCED);
	}

	//the target is tinted purple so it stands out, like corrupted enemies are darkened
	private transient CommissionTint tint;

	@Override
	public void fx(boolean on) {
		if (on) {
			if (tint != null) tint.remove();
			tint = CommissionTint.apply(target.sprite);
		} else if (tint != null) {
			tint.remove();
			tint = null;
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.MARK;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.75f, 0.5f, 1f);
	}

	public static void onTargetDied( Char target ){
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive() || !hero.hasTalent(Talent.COMMISSION)) return;
		int pts = hero.pointsInTalent(Talent.COMMISSION);

		InfoPower.gain(pts);

		Item reward;
		if (Random.Float() < EQUIPMENT_CHANCE){
			reward = RingOfWealth.genEquipmentDrop(0);
			//+0/+1/+2 guaranteed
			if (reward.isUpgradable() && reward.level() < pts-1){
				reward.level(pts-1);
			}
			reward.cursed = false;
			reward.cursedKnown = true;
		} else {
			switch (pts){
				case 1: default: reward = RingOfWealth.genLowValueConsumable(); break;
				case 2: reward = RingOfWealth.genMidValueConsumable(); break;
				case 3: reward = RingOfWealth.genHighValueConsumable(); break;
			}
		}
		Dungeon.level.drop(reward, target.pos).sprite.drop();
		com.shatteredpixel.shatteredpixeldungeon.utils.GLog.p(Messages.get(Commission.class, "complete"));
	}
}

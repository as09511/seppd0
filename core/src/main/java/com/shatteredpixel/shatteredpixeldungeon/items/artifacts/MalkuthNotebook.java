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

package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.malkuth.EmployeeCall;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.malkuth.LCorpEmployee;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//Malkuth's starting item: grants a short burst of haste and a longer blessing
public class MalkuthNotebook extends Artifact {

	public static final String AC_USE = "USE";

	public static final float HASTE_DURATION = 3f;
	public static final float BLESS_DURATION = 15f;
	public static final int BASE_CHARGES = 2;
	public static final int RECHARGE_TURNS = 50;

	{
		image = ItemSpriteSheet.MALKUTH_NOTEBOOK;

		levelCap = 0;

		charge = BASE_CHARGES;
		partialCharge = 0;
		chargeCap = BASE_CHARGES;

		defaultAction = AC_USE;

		unique = true;
		bones = false;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		if (isEquipped( hero ) && !cursed && hero.buff(MagicImmune.class) == null) {
			actions.add(AC_USE);
		}
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {

		super.execute(hero, action);

		if (hero.buff(MagicImmune.class) != null) return;

		if (action.equals(AC_USE)) {

			if (!isEquipped(hero))      GLog.i( Messages.get(Artifact.class, "need_to_equip") );
			else if (cursed)            GLog.i( Messages.get(this, "cursed") );
			else if (charge <= 0)       GLog.i( Messages.get(this, "no_charge") );
			else {
				charge--;
				updateQuickslot();

				applyEffect(hero);
				Sample.INSTANCE.play(Assets.Sounds.READ);
				hero.sprite.operate(hero.pos);
				Talent.onArtifactUsed(hero);

				//TT2 protocol: no time spent, and +2/+3 also clears slowness, cripple and roots
				int tt2 = hero.pointsInTalent(Talent.TT2_PROTOCOL);
				if (tt2 >= 2){
					Buff.detach(hero, Slow.class);
					Buff.detach(hero, Cripple.class);
				}
				if (tt2 >= 3){
					Buff.detach(hero, Roots.class);
				}
				if (tt2 > 0){
					hero.next();
				} else {
					hero.spendAndNext(1f);
				}
			}

		}
	}

	public static float hasteDuration(Hero hero){
		//+1/+2/+3 turns from the extra approval talent
		return HASTE_DURATION + hero.pointsInTalent(Talent.EXTRA_APPROVAL);
	}

	//3 charges at +3 extra approval
	public static int maxCharges(Hero hero){
		return hero != null && hero.pointsInTalent(Talent.EXTRA_APPROVAL) >= 3 ? BASE_CHARGES + 1 : BASE_CHARGES;
	}

	//45/40/35 turns per charge with the efficient work talent
	public static int rechargeTurns(Hero hero){
		return RECHARGE_TURNS - (hero == null ? 0 : 5*hero.pointsInTalent(Talent.EFFICIENT_WORK));
	}

	public static float blessDuration(Hero hero){
		//+3/+5 turns from the overtime talent
		float duration = BLESS_DURATION;
		if (hero.hasTalent(Talent.OVERTIME)){
			duration += 1 + 2*hero.pointsInTalent(Talent.OVERTIME);
		}
		return duration;
	}

	//the notebook's effect, also triggered by the emergency manual talent
	public static void applyEffect(Hero hero){
		float haste = hasteDuration(hero);
		float bless = blessDuration(hero);
		empower(hero, haste, bless);

		//the control team's sephirah always shares the effect with her employee
		EmployeeCall call = hero.buff(EmployeeCall.class);
		LCorpEmployee employee = call == null ? null : call.employee();
		if (employee != null){
			empower(employee, haste, bless);
		}

		//joint command: allies within 2/3 tiles get the same effect
		if (hero.hasTalent(Talent.JOINT_COMMAND)){
			int range = 1 + hero.pointsInTalent(Talent.JOINT_COMMAND);
			for (Char ch : Actor.chars()){
				if (ch != hero && ch != employee && ch.alignment == Char.Alignment.ALLY
						&& Dungeon.level.distance(hero.pos, ch.pos) <= range){
					empower(ch, haste, bless);
				}
			}
		}
	}

	private static void empower(Char ch, float haste, float bless){
		Buff.prolong(ch, Haste.class, haste);
		Buff.prolong(ch, Bless.class, bless);
		if (ch.sprite != null){
			ch.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 6);
		}
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new NotebookRecharge();
	}

	//partial charges from talents like the worker bee
	public void directCharge(float amount){
		if (charge < chargeCap) {
			partialCharge += amount;
			while (partialCharge >= 1f) {
				charge++;
				partialCharge--;
			}
			if (charge >= chargeCap){
				partialCharge = 0;
				charge = chargeCap;
			}
			updateQuickslot();
		}
	}

	@Override
	public void charge(Hero target, float amount) {
		if (cursed || target.buff(MagicImmune.class) != null) return;

		if (charge < chargeCap) {
			partialCharge += 0.25f*amount;
			while (partialCharge >= 1f) {
				charge++;
				partialCharge--;
			}
			if (charge >= chargeCap){
				partialCharge = 0;
				charge = chargeCap;
			}
			updateQuickslot();
		}
	}

	@Override
	public String desc() {
		String desc = super.desc();
		if (Dungeon.hero != null){
			desc += "\n\n" + Messages.get(this, "effect", (int)hasteDuration(Dungeon.hero), (int)blessDuration(Dungeon.hero), maxCharges(Dungeon.hero), rechargeTurns(Dungeon.hero));
		}
		return desc;
	}

	public class NotebookRecharge extends ArtifactBuff {

		@Override
		public boolean act() {
			chargeCap = maxCharges(target instanceof Hero ? (Hero) target : null);
			if (charge < chargeCap && !cursed && target.buff(MagicImmune.class) == null && Regeneration.regenOn()) {
				partialCharge += RingOfEnergy.artifactChargeMultiplier(target) / rechargeTurns(target instanceof Hero ? (Hero) target : null);
				while (partialCharge >= 1f) {
					charge++;
					partialCharge--;
				}
				if (charge >= chargeCap){
					partialCharge = 0;
					charge = chargeCap;
				}
				updateQuickslot();
			}
			spend( TICK );
			return true;
		}
	}
}

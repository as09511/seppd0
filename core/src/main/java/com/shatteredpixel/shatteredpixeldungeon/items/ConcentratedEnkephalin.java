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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;

//Netzach's concentrated enkephalin: drinking it restores all health, which is then paid back as damage
//spread over the following turns, like viscosity. It holds 1 charge and recharges on each floor visited for the first time.
public class ConcentratedEnkephalin extends Item {

	public static final String AC_DRINK = "DRINK";

	{
		image = ItemSpriteSheet.CONCENTRATED_ENKEPHALIN;
		defaultAction = AC_DRINK;

		unique = true;
		bones = false;
	}

	private int charges = 1;
	//floors (depth and branch) this item has already recharged on
	private HashSet<Integer> floors = new HashSet<>();

	public static int maxCharges( Hero hero ){
		return hero != null && hero.hasTalent(Talent.EXTRA_RATION) ? 2 : 1;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_DRINK);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute(hero, action);

		if (action.equals(AC_DRINK)){
			if (charges <= 0){
				GLog.w(Messages.get(this, "no_charge"));
				return;
			}
			int heal = hero.HT - hero.HP;
			if (heal <= 0){
				GLog.w(Messages.get(this, "full"));
				return;
			}
			charges--;
			updateQuickslot();

			hero.HP = hero.HT;
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			Buff.affect(hero, EnkephalinDebt.class).extend(heal);

			if (hero.hasTalent(Talent.OVERDOSE)){
				Buff.prolong(hero, Overdose.class, Overdose.DURATION);
			}

			Sample.INSTANCE.play(Assets.Sounds.DRINK);
			GLog.i(Messages.get(this, "drink", heal));
			hero.sprite.operate(hero.pos);
			hero.spendAndNext(1f);
		}
	}

	//a new floor recharges every concentrated enkephalin the hero carries, see Dungeon.switchLevel
	public static void onLevelArrived( Hero hero ){
		int key = Dungeon.depth * 100 + Dungeon.branch;
		for (ConcentratedEnkephalin e : hero.belongings.getAllItems(ConcentratedEnkephalin.class)){
			e.arrive(hero, key);
		}
	}

	private void arrive( Hero hero, int key ){
		if (floors.contains(key)) return;
		floors.add(key);
		int max = maxCharges(hero);
		if (charges >= max) return;
		charges++;
		//extra ration +2: a 50% chance of one more charge
		if (charges < max && hero.pointsInTalent(Talent.EXTRA_RATION) >= 2 && Random.Int(2) == 0){
			charges++;
		}
		updateQuickslot();
		GLog.p(Messages.get(this, "recharged", charges, max));
	}

	//Netzach's healing outside of regeneration, with regenerator formula fix turning the overflow into a shield
	public static void heal( Hero hero, int amount ){
		if (amount <= 0 || !hero.isAlive()) return;
		int healed = Math.min(amount, hero.HT - hero.HP);
		if (healed > 0){
			hero.HP += healed;
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);
		}
		onOverheal(hero, amount - healed);
	}

	//healing beyond max health, 20/35% of which becomes a shield with regenerator formula fix
	public static void onOverheal( Hero hero, int overflow ){
		if (overflow <= 0 || !hero.hasTalent(Talent.REGENERATOR_FIX)) return;
		int shield = Math.round(overflow * (0.05f + 0.15f*hero.pointsInTalent(Talent.REGENERATOR_FIX)));
		if (shield > 0){
			Buff.affect(hero, Barrier.class).incShield(shield);
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
		}
	}

	@Override
	public String status() {
		return charges + "/" + maxCharges(Dungeon.hero);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc") + "\n\n" + Messages.get(this, "charges", charges, maxCharges(Dungeon.hero));
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	private static final String CHARGES = "charges";
	private static final String FLOORS  = "floors";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(CHARGES, charges);
		int[] f = new int[floors.size()];
		int i = 0;
		for (int k : floors) f[i++] = k;
		bundle.put(FLOORS, f);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		charges = bundle.getInt(CHARGES);
		floors.clear();
		if (bundle.contains(FLOORS)){
			for (int k : bundle.getIntArray(FLOORS)) floors.add(k);
		}
	}

	//the health borrowed by the enkephalin, paid back 10% at a time each turn like viscosity
	public static class EnkephalinDebt extends Viscosity.DeferedDamage {

		@Override
		public String name() {
			return Messages.get(this, "name");
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", damage);
		}
	}

	//overdose: 15/25% more damage for 10 turns after drinking
	public static class Overdose extends FlavourBuff {

		public static final float DURATION = 10f;

		{
			type = buffType.POSITIVE;
		}

		public static float damageFactor( Hero hero ){
			if (hero.buff(Overdose.class) == null) return 1f;
			return 1f + 0.05f + 0.1f*hero.pointsInTalent(Talent.OVERDOSE);
		}

		@Override
		public int icon() {
			return BuffIndicator.UPGRADE;
		}

		@Override
		public void tintIcon( com.watabou.noosa.Image icon ) {
			icon.hardlight(0x46C850);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (DURATION - visualcooldown()) / DURATION);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}
}

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


package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//ALEPH, a small orb that fires warm rays: WHITE damage, 3 shots,
//refilled one at a time every 3 turns while equipped instead of being reloaded by hand
public class SoundOfAStar extends EgoGun {

	private static final int MAX_AMMO = 3;
	public static final int RECHARGE_TURNS = 3;

	{
		image = ItemSpriteSheet.SOUND_OF_A_STAR;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.2f;

		risk = RiskLevel.ALEPH;
		damageType = DamageType.WHITE;
		tier = 5;
	}

	@Override
	public int min(int lvl) {
		return  5 +             //5 base
				lvl;            //+1 per level
	}

	@Override
	public int max(int lvl) {
		return  20 +            //20 base, down from 30
				6*lvl;          //+6 per level
	}

	@Override
	public int maxAmmo() {
		return MAX_AMMO;
	}

	//never reloaded by hand
	@Override
	public float reloadTime() {
		return 0;
	}

	@Override
	protected boolean canReload() {
		return false;
	}

	@Override
	public int shotMin(int lvl) {
		return  6 +             //6 base
				lvl;            //+1 per level
	}

	@Override
	public int shotMax(int lvl) {
		return  24 +            //24 base
				6*lvl;          //+6 per level
	}

	@Override
	protected int boltType() {
		return MagicMissile.MAGIC_MISSILE;
	}

	@Override
	protected void fireSound() {
		Sample.INSTANCE.play(Assets.Sounds.ZAP, 1f, 1.2f);
	}

	@Override
	public void activate(Char ch) {
		super.activate(ch);
		if (ch instanceof Hero){
			Buff.affect(ch, StarRecharge.class);
		}
	}

	//adds one shot, returns true if the magazine was not full
	boolean addShot(){
		if (ammo >= MAX_AMMO) return false;
		ammo++;
		updateQuickslot();
		return true;
	}

	//adds a shot every 3 turns while the orb is equipped, and goes away once it no longer is
	public static class StarRecharge extends Buff {

		{
			type = buffType.NEUTRAL;
		}

		private int turns = 0;

		private SoundOfAStar orb(){
			if (!(target instanceof Hero)) return null;
			Hero hero = (Hero) target;
			if (hero.belongings.weapon() instanceof SoundOfAStar) return (SoundOfAStar) hero.belongings.weapon();
			if (hero.belongings.secondWep() instanceof SoundOfAStar) return (SoundOfAStar) hero.belongings.secondWep();
			return null;
		}

		@Override
		public boolean act() {
			SoundOfAStar orb = orb();
			if (orb == null){
				detach();
				return true;
			}
			if (orb.ammo() < orb.maxAmmo()){
				turns++;
				if (turns >= RECHARGE_TURNS){
					turns = 0;
					orb.addShot();
				}
			} else {
				turns = 0;
			}
			spend(TICK);
			return true;
		}

		private static final String TURNS = "turns";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(TURNS, turns);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			turns = bundle.getInt(TURNS);
		}
	}
}

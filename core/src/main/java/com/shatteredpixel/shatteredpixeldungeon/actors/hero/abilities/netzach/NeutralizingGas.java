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


package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.netzach;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.AbilityCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

//Netzach's first armor ability: sprays mental corruption neutralizing gas around him.
//Allies standing in it can't be given debuffs.
public class NeutralizingGas extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	//5x5, wide spray: 7x7 / 9x9 / 11x11
	public static int radius( Hero hero ){
		return 2 + hero.pointsInTalent(Talent.WIDE_SPRAY);
	}

	//10 turns, lingering gas: +5 per point
	public static int duration( Hero hero ){
		return 10 + 5*hero.pointsInTalent(Talent.LINGERING_GAS);
	}

	@Override
	protected void activate( AbilityCrown armor, Hero hero, Integer target ) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		int r = radius(hero);
		int dur = duration(hero);
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (!Dungeon.level.solid[i] && Dungeon.level.distance(hero.pos, i) <= r){
				GameScene.add(Blob.seed(i, dur, Gas.class));
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.GAS);
		hero.sprite.operate(hero.pos);
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	//true if the gas stops this buff from being added to this char, see Char.add
	public static boolean blocks( Char ch, Buff buff ){
		if (buff.type != Buff.buffType.NEGATIVE || ch.alignment != Char.Alignment.ALLY) return false;
		//the gas does not stop damage that is owed, nor the special ally and inventory states
		if (buff instanceof Viscosity.DeferedDamage || buff instanceof AllyBuff || buff instanceof LostInventory) return false;
		if (Dungeon.level == null || ch.pos < 0 || ch.pos >= Dungeon.level.length()) return false;
		Blob gas = Dungeon.level.blobs.get(Gas.class);
		return gas != null && gas.volume > 0 && gas.cur[ch.pos] > 0;
	}

	@Override
	public int icon() {
		return HeroIcon.NEUTRALIZING_GAS;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.WIDE_SPRAY, Talent.PURIFYING_MIST, Talent.LINGERING_GAS, Talent.HEROIC_ENERGY};
	}

	//the gas: it does not spread, each cell just fades after a while.
	//purifying mist: allies inside heal 1/2/3 each turn
	public static class Gas extends Blob {

		@Override
		protected void evolve() {
			int heal = Dungeon.hero == null ? 0 : Dungeon.hero.pointsInTalent(Talent.PURIFYING_MIST);
			int cell;
			for (int i = area.left; i < area.right; i++){
				for (int j = area.top; j < area.bottom; j++){
					cell = i + j*Dungeon.level.width();
					off[cell] = cur[cell] > 0 ? cur[cell] - 1 : 0;
					if (off[cell] > 0){
						volume += off[cell];
						if (heal > 0){
							Char ch = Actor.findChar(cell);
							if (ch != null && ch.alignment == Char.Alignment.ALLY && ch.isAlive() && ch.HP < ch.HT){
								int h = Math.min(heal, ch.HT - ch.HP);
								ch.HP += h;
								if (ch.sprite != null && Dungeon.level.heroFOV[cell]){
									ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(h), FloatingText.HEALING);
								}
							}
						}
					}
				}
			}
		}

		@Override
		public void use( BlobEmitter emitter ) {
			super.use(emitter);
			emitter.pour(Speck.factory(Speck.STEAM), 0.35f);
		}

		@Override
		public String tileDesc() {
			return Messages.get(this, "desc");
		}
	}
}

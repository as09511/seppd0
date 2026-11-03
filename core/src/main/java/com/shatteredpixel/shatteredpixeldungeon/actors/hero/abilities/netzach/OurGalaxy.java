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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.AbilityCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Netzach's second armor ability: our galaxy. Every visible enemy nearby takes 3-6 damage that ignores armor,
//and Netzach heals for all the damage dealt.
public class OurGalaxy extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	//7x7, starlight: 9x9 / 11x11 / 13x13
	public static int radius( Hero hero ){
		return 3 + hero.pointsInTalent(Talent.STARLIGHT);
	}

	//starfall: +1 to both ends per point
	public static int minDamage( Hero hero ){
		return 3 + hero.pointsInTalent(Talent.STARFALL);
	}

	public static int maxDamage( Hero hero ){
		return 6 + hero.pointsInTalent(Talent.STARFALL);
	}

	@Override
	protected void activate( AbilityCrown armor, Hero hero, Integer target ) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		int r = radius(hero);
		ArrayList<Char> targets = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.alignment == Char.Alignment.ENEMY && m.isAlive() && Dungeon.level.heroFOV[m.pos]
					&& Dungeon.level.distance(hero.pos, m.pos) <= r){
				targets.add(m);
			}
		}

		int dealt = 0;
		for (Char ch : targets){
			int before = ch.HP + ch.shielding();
			ch.damage(Random.NormalIntRange(minDamage(hero), maxDamage(hero)), hero);
			dealt += Math.max(0, before - (ch.HP + ch.shielding()));
			if (ch.sprite != null){
				ch.sprite.emitter().burst(Speck.factory(Speck.STAR), 6);
				MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.BEACON, ch.sprite, hero.pos, () -> {});
			}
		}

		//the stolen starlight heals Netzach, galaxy embrace turns 33/66/100% of the excess into a shield
		if (dealt > 0){
			int heal = Math.min(dealt, hero.HT - hero.HP);
			if (heal > 0){
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
			int excess = dealt - heal;
			if (excess > 0 && hero.hasTalent(Talent.GALAXY_EMBRACE)){
				int shield = Math.round(excess * hero.pointsInTalent(Talent.GALAXY_EMBRACE) / 3f);
				if (shield > 0){
					Buff.affect(hero, Barrier.class).incShield(shield);
					hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
				}
			}
		}

		hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 10);
		Sample.INSTANCE.play(Assets.Sounds.CHARMS);
		hero.sprite.operate(hero.pos);
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.OUR_GALAXY;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.STARLIGHT, Talent.STARFALL, Talent.GALAXY_EMBRACE, Talent.HEROIC_ENERGY};
	}
}

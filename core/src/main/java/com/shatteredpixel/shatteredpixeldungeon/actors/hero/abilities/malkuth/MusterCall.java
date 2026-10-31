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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.malkuth;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.AbilityCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Malkuth's first armor ability: hastes and blesses the hero and every ally in sight
public class MusterCall extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	public static float hasteDuration( Hero hero ){
		return 5f + hero.pointsInTalent(Talent.EMERGENCY_MUSTER);
	}

	public static float blessDuration( Hero hero ){
		return 10f + 3f*hero.pointsInTalent(Talent.EMERGENCY_MUSTER);
	}

	@Override
	protected void activate(AbilityCrown armor, Hero hero, Integer target) {

		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		//all hands: allies out of sight but within 4/8/12/any tiles answer the call as well
		int callRange = 0;
		switch (hero.pointsInTalent(Talent.ALL_HANDS_MOVE)){
			case 1: callRange = 4; break;
			case 2: callRange = 8; break;
			case 3: callRange = 12; break;
			case 4: callRange = Integer.MAX_VALUE; break;
		}

		ArrayList<Char> called = new ArrayList<>();
		called.add(hero);
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.alignment != Char.Alignment.ALLY || !m.isAlive()) continue;
			if (Dungeon.level.heroFOV[m.pos]
					|| (callRange > 0 && Dungeon.level.distance(hero.pos, m.pos) <= callRange)){
				called.add(m);
			}
		}

		for (Char ch : called){
			//all hands: called allies gather next to the hero
			if (ch != hero && hero.hasTalent(Talent.ALL_HANDS_MOVE)
					&& Dungeon.level.distance(hero.pos, ch.pos) > 1 && !ch.rooted){
				int dest = freeNeighbour(hero.pos);
				if (dest != -1){
					ScrollOfTeleportation.appear(ch, dest);
					Dungeon.level.occupyCell(ch);
				}
			}

			Buff.prolong(ch, Haste.class, hasteDuration(hero));
			Buff.prolong(ch, Bless.class, blessDuration(hero));

			//morale boost: heals 7.5%/15%/22.5%/30% of max health
			if (hero.hasTalent(Talent.MORALE_BOOST)){
				int heal = Math.round(ch.HT * 0.075f * hero.pointsInTalent(Talent.MORALE_BOOST));
				heal = Math.min(heal, ch.HT - ch.HP);
				if (heal > 0){
					ch.HP += heal;
					ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
				}
			}

			if (ch.sprite != null) ch.sprite.emitter().burst(Speck.factory(Speck.UP), 4);
		}

		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
		hero.sprite.operate(hero.pos);
		Dungeon.observe();
		hero.spendAndNext(Actor.TICK);
	}

	private static int freeNeighbour( int center ){
		ArrayList<Integer> options = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8){
			int c = center + i;
			if (Dungeon.level.passable[c] && !Dungeon.level.avoid[c] && Actor.findChar(c) == null){
				options.add(c);
			}
		}
		if (options.isEmpty()) return -1;
		return options.get(Random.Int(options.size()));
	}

	@Override
	public int icon() {
		return HeroIcon.POWER_OF_MANY;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.EMERGENCY_MUSTER, Talent.MORALE_BOOST, Talent.ALL_HANDS_MOVE, Talent.HEROIC_ENERGY};
	}
}

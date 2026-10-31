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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.AbilityCrown;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;

//Malkuth's second armor ability: dash in a line, striking and bleeding every enemy along the way
public class Hornet extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	public static int maxDistance( Hero hero ){
		return 4 + hero.pointsInTalent(Talent.WINGBEAT);
	}

	@Override
	public int targetedPos(Char user, int dst) {
		Ballistica route = new Ballistica(user.pos, dst, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET);
		return landingCell(route, user instanceof Hero ? maxDistance((Hero)user) : 4, user.pos);
	}

	//furthest free cell along the route, enemies in the way are passed through
	private static int landingCell( Ballistica route, int maxDist, int start ){
		int dist = Math.min(route.dist, maxDist);
		for (int i = dist; i > 0; i--){
			int cell = route.path.get(i);
			if (Actor.findChar(cell) == null && Dungeon.level.passable[cell]){
				return cell;
			}
		}
		return start;
	}

	@Override
	protected void activate(AbilityCrown armor, Hero hero, Integer target) {
		if (target == null){
			return;
		}
		if (target == hero.pos){
			GLog.w(Messages.get(this, "self_target"));
			return;
		}
		if (hero.rooted){
			PixelScene.shake( 1, 1f );
			return;
		}

		Ballistica route = new Ballistica(hero.pos, target, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET);
		int maxDist = maxDistance(hero);
		final int dest = landingCell(route, maxDist, hero.pos);
		if (dest == hero.pos){
			GLog.w(Messages.get(this, "blocked"));
			return;
		}

		//every enemy on the route up to the landing cell, plus whoever we stop in front of
		final ArrayList<Char> victims = new ArrayList<>();
		int dist = Math.min(route.dist, maxDist);
		for (int i = 1; i <= dist; i++){
			Char ch = Actor.findChar(route.path.get(i));
			if (ch != null && ch != hero && ch.alignment != hero.alignment){
				victims.add(ch);
			}
			if (route.path.get(i) == dest){
				int next = i+1 <= dist ? route.path.get(i+1) : -1;
				Char blocker = next == -1 ? null : Actor.findChar(next);
				if (blocker != null && blocker.alignment != hero.alignment) victims.add(blocker);
				break;
			}
		}

		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		hero.busy();
		Sample.INSTANCE.play(Assets.Sounds.MISS, 1f, 1.5f);
		float duration = 0.05f * Math.max(1, Dungeon.level.distance(hero.pos, dest));
		hero.sprite.jump(hero.pos, dest, 0, duration, new Callback() {
			@Override
			public void call() {
				hero.move(dest);
				Dungeon.level.occupyCell(hero);
				Dungeon.observe();
				GameScene.updateFog();

				for (Char ch : victims){
					if (!ch.isAlive()) continue;
					hero.attack(ch, 1f, 0, Char.INFINITE_ACCURACY);
					if (ch.isAlive()){
						Buff.affect(ch, Bleeding.class).set(Math.max(1, hero.damageRoll()*0.33f), Hornet.class);
						Buff.prolong(ch, HornetSting.class, HornetSting.DURATION);
						if (hero.hasTalent(Talent.VENOM_STING)){
							Buff.prolong(ch, Vulnerable.class, 2f*hero.pointsInTalent(Talent.VENOM_STING));
						}
					}
				}

				PixelScene.shake(1, 0.3f);
				Invisibility.dispel();
				hero.spendAndNext(Actor.TICK);
			}
		});
	}

	//marks enemies stung by the hornet, so the queen bee talent can raise them as worker bees
	public static class HornetSting extends FlavourBuff {
		public static final float DURATION = 30f;
	}

	@Override
	public int icon() {
		return HeroIcon.HEROIC_LEAP;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.VENOM_STING, Talent.WINGBEAT, Talent.QUEEN_BEE, Talent.HEROIC_ENERGY};
	}
}

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
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.gebura;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.gebura.RabbitMember;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.AbilityCrown;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Gebura's first armor ability: calls in four Rabbit Team members, one of each damage type, who act on their
// own and withdraw after 20 turns. Calling again sends the current team back first.
//Reinforcement: +1 member per point (RED, WHITE, BLACK, PALE in that order). Extended operation: +5 turns
// per point. Elite squad: +15% health and damage per point.
public class RabbitCall extends ArmorAbility {

	public static final int BASE_MEMBERS = 4;
	public static final int BASE_DURATION = 20;

	{
		baseChargeUse = 50f;
	}

	private ArrayList<Integer> memberIDs = new ArrayList<>();

	public static int duration( Hero hero ){
		return BASE_DURATION + 5*hero.pointsInTalent(Talent.EXTENDED_OPERATION);
	}

	public static int members( Hero hero ){
		return BASE_MEMBERS + hero.pointsInTalent(Talent.REINFORCEMENT);
	}

	@Override
	protected void activate( AbilityCrown armor, Hero hero, Integer target ) {

		//the team already out pulls back
		for (int id : memberIDs){
			Actor a = Actor.findById(id);
			if (a instanceof RabbitMember && ((RabbitMember) a).isAlive()){
				((RabbitMember) a).withdraw();
			}
		}
		memberIDs.clear();

		//free cells around Gebura, the closest first
		ArrayList<Integer> cells = new ArrayList<>();
		PathFinder.buildDistanceMap(hero.pos, com.watabou.utils.BArray.not(Dungeon.level.solid, null), 3);
		for (int dist = 1; dist <= 3; dist++){
			ArrayList<Integer> ring = new ArrayList<>();
			for (int i = 0; i < Dungeon.level.length(); i++){
				if (PathFinder.distance[i] == dist && Dungeon.level.passable[i] && !Dungeon.level.avoid[i]
						&& Actor.findChar(i) == null){
					ring.add(i);
				}
			}
			Random.shuffle(ring);
			cells.addAll(ring);
		}
		if (cells.isEmpty()){
			GLog.w(Messages.get(this, "no_space"));
			return;
		}

		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		int count = Math.min(members(hero), cells.size());
		for (int i = 0; i < count; i++){
			RabbitMember m = new RabbitMember(hero, i % 4, duration(hero));
			m.pos = cells.get(i);
			GameScene.add(m);
			m.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 6);
			memberIDs.add(m.id());
		}
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT);

		hero.sprite.operate(hero.pos);
		hero.spendAndNext(Actor.TICK);
	}

	@Override
	public int icon() {
		return HeroIcon.RABBIT_CALL;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.REINFORCEMENT, Talent.EXTENDED_OPERATION, Talent.ELITE_SQUAD, Talent.HEROIC_ENERGY};
	}

	private static final String MEMBERS = "members";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		int[] ids = new int[memberIDs.size()];
		for (int i = 0; i < ids.length; i++) ids[i] = memberIDs.get(i);
		bundle.put(MEMBERS, ids);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		memberIDs.clear();
		if (bundle.contains(MEMBERS)){
			for (int id : bundle.getIntArray(MEMBERS)) memberIDs.add(id);
		}
	}
}

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


package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.hod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.hod.LCorpManager;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.AbilityCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Hod's first armor ability: calls in an L Corp manager. Using it again replaces the current one.
public class ManagerCall extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	private int managerID = -1;

	public LCorpManager manager(){
		if (managerID == -1) return null;
		Actor a = Actor.findById(managerID);
		if (a instanceof LCorpManager && ((LCorpManager) a).isAlive()){
			return (LCorpManager) a;
		}
		return null;
	}

	//the hero's current manager, if she has this ability and one is alive
	public static LCorpManager currentManager( Hero hero ){
		if (hero == null || !(hero.armorAbility instanceof ManagerCall)) return null;
		return ((ManagerCall) hero.armorAbility).manager();
	}

	//field guidance: Hod and her manager take 5/10/15/20% less damage while next to each other
	public static float guidanceFactor( Hero hero, Char ch ){
		if (hero == null || !hero.hasTalent(Talent.FIELD_GUIDANCE)) return 1f;
		LCorpManager m = currentManager(hero);
		if (m == null || (ch != hero && ch != m) || !Dungeon.level.adjacent(hero.pos, m.pos)) return 1f;
		return 1f - 0.05f*hero.pointsInTalent(Talent.FIELD_GUIDANCE);
	}

	//emergency relocation: 20/17/14/10 charge
	private static final int[] RELOCATE_COST = {20, 20, 17, 14, 10};
	public static float relocateCost( Hero hero ){
		return RELOCATE_COST[Math.min(4, hero.pointsInTalent(Talent.EMERGENCY_RELOCATION))];
	}

	public static boolean canRelocate( Hero hero ){
		return hero.hasTalent(Talent.EMERGENCY_RELOCATION) && currentManager(hero) != null;
	}

	//swaps places with the manager without spending a turn
	public static void relocate( AbilityCrown crown, Hero hero ){
		LCorpManager m = currentManager(hero);
		if (m == null) return;
		if (crown.charge < relocateCost(hero)){
			GLog.w(Messages.get(AbilityCrown.class, "low_charge"));
			return;
		}
		if (hero.rooted || m.rooted){
			GLog.w(Messages.get(ManagerCall.class, "rooted"));
			return;
		}
		crown.charge -= relocateCost(hero);
		Item.updateQuickslot();

		int heroPos = hero.pos;
		int managerPos = m.pos;
		hero.pos = managerPos;
		m.pos = heroPos;
		hero.sprite.place(managerPos);
		m.sprite.place(heroPos);
		CellEmitter.get(heroPos).burst(Speck.factory(Speck.LIGHT), 4);
		CellEmitter.get(managerPos).burst(Speck.factory(Speck.LIGHT), 4);
		Dungeon.level.occupyCell(hero);
		Dungeon.level.occupyCell(m);
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
		Dungeon.observe();
		GameScene.updateFog();
	}

	@Override
	protected void activate(AbilityCrown armor, Hero hero, Integer target) {

		//the current manager steps out to make room for the new one
		LCorpManager old = manager();
		if (old != null){
			CellEmitter.get(old.pos).burst(Speck.factory(Speck.LIGHT), 6);
			old.destroy();
			if (old.sprite != null) old.sprite.killAndErase();
			managerID = -1;
		}

		ArrayList<Integer> cells = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			int cell = hero.pos + n;
			if (Dungeon.level.passable[cell] && !Dungeon.level.avoid[cell] && Actor.findChar(cell) == null){
				cells.add(cell);
			}
		}
		if (cells.isEmpty()){
			GLog.w(Messages.get(this, "no_space"));
			return;
		}

		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		LCorpManager manager = new LCorpManager(hero);
		manager.pos = Random.element(cells);
		GameScene.add(manager);
		manager.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 6);
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
		managerID = manager.id();

		hero.sprite.operate(hero.pos);
		hero.spendAndNext(Actor.TICK);
	}

	@Override
	public int icon() {
		return HeroIcon.SHADOW_CLONE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.HIRING_PROCEDURE, Talent.FIELD_GUIDANCE, Talent.EMERGENCY_RELOCATION, Talent.HEROIC_ENERGY};
	}

	private static final String MANAGER = "manager";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(MANAGER, managerID);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		managerID = bundle.contains(MANAGER) ? bundle.getInt(MANAGER) : -1;
	}
}

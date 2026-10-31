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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.malkuth;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Control Team Sephirah: calls in an L Corp employee from the action button, 50 turns after it falls
public class EmployeeCall extends Buff implements ActionIndicator.Action {

	public static final int COOLDOWN = 50;

	{
		type = buffType.NEUTRAL;
		revivePersists = true;
	}

	private int employeeID = -1;
	private int cooldown = 0;

	public LCorpEmployee employee(){
		if (employeeID == -1) return null;
		Actor a = Actor.findById(employeeID);
		if (a instanceof LCorpEmployee && ((LCorpEmployee) a).isAlive()){
			return (LCorpEmployee) a;
		}
		return null;
	}

	@Override
	public boolean act() {
		//the employee is gone without dying normally, treat it as fallen
		if (employeeID != -1 && employee() == null){
			onEmployeeDied();
		}
		if (cooldown > 0 && employee() == null){
			cooldown--;
		}
		refreshAction();
		spend(TICK);
		return true;
	}

	public void refreshAction(){
		if (cooldown <= 0 && employee() == null && employeeID == -1){
			if (ActionIndicator.action == null) ActionIndicator.setAction(this);
		} else {
			ActionIndicator.clearAction(this);
		}
	}

	public void onEmployeeDied(){
		employeeID = -1;
		cooldown = COOLDOWN;
		refreshAction();
		BuffIndicator.refreshHero();
	}

	@Override
	public void fx(boolean on) {
		if (on) refreshAction();
	}

	@Override
	public void detach() {
		super.detach();
		ActionIndicator.clearAction(this);
	}

	@Override
	public int icon() {
		return cooldown > 0 ? BuffIndicator.TIME : BuffIndicator.NONE;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.3f, 0.8f, 0.3f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (COOLDOWN - cooldown) / (float)COOLDOWN);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(cooldown);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", cooldown);
	}

	@Override
	public String actionName() {
		return Messages.get(this, "action_name");
	}

	@Override
	public int actionIcon() {
		return HeroIcon.SHADOW_CLONE;
	}

	@Override
	public int indicatorColor() {
		return 0x2E7D32;
	}

	@Override
	public void doAction() {
		Hero hero = (Hero) target;

		ArrayList<Integer> cells = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			int cell = hero.pos + n;
			if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null){
				cells.add(cell);
			}
		}
		if (cells.isEmpty()){
			GLog.w(Messages.get(this, "no_space"));
			return;
		}

		LCorpEmployee employee = new LCorpEmployee(hero);
		employee.pos = Random.element(cells);
		GameScene.add(employee);
		employee.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 6);
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
		employeeID = employee.id();

		ActionIndicator.clearAction(this);
		hero.sprite.operate(hero.pos);
		hero.spendAndNext(1f);
	}

	private static final String EMPLOYEE = "employee";
	private static final String COOLDOWN_KEY = "cooldown";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(EMPLOYEE, employeeID);
		bundle.put(COOLDOWN_KEY, cooldown);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		employeeID = bundle.getInt(EMPLOYEE);
		cooldown = bundle.getInt(COOLDOWN_KEY);
	}
}

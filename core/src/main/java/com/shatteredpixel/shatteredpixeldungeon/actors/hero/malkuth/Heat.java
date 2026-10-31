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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Adrenaline;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//History Floor Librarian: gathers heat from burning characters in sight, then turns it into adrenaline
public class Heat extends Buff implements ActionIndicator.Action {

	public static final int MAX_HEAT = 10;
	//heat is lost every this many turns with no fire in sight
	public static final int DECAY_TURNS = 3;
	//heat needed for each turn of adrenaline
	public static final int HEAT_PER_TURN = 2;

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	private int heat = 0;
	private int coolTurns = 0;

	@Override
	public boolean act() {
		int burning = 0;
		for (Char ch : Actor.chars()){
			if (ch.isAlive() && ch.buff(Burning.class) != null
					&& (ch == target || Dungeon.level.heroFOV[ch.pos])){
				burning++;
			}
		}

		if (burning > 0){
			heat = Math.min(MAX_HEAT, heat + burning);
			coolTurns = 0;
		} else if (heat > 0){
			coolTurns++;
			if (coolTurns >= DECAY_TURNS){
				heat--;
				coolTurns = 0;
			}
		}

		refreshAction();
		BuffIndicator.refreshHero();
		spend(TICK);
		return true;
	}

	public void refreshAction(){
		if (heat >= HEAT_PER_TURN){
			if (ActionIndicator.action == null || ActionIndicator.action == this) {
				ActionIndicator.setAction(this);
			}
		} else {
			ActionIndicator.clearAction(this);
		}
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
		return heat > 0 ? BuffIndicator.FIRE : BuffIndicator.NONE;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(1f, 0.55f, 0.1f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (MAX_HEAT - heat) / (float)MAX_HEAT);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(heat);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", heat, MAX_HEAT, heat/HEAT_PER_TURN);
	}

	@Override
	public String actionName() {
		return Messages.get(this, "action_name");
	}

	@Override
	public int actionIcon() {
		return HeroIcon.ELEMENTAL_STRIKE;
	}

	@Override
	public Visual secondaryVisual() {
		BitmapText txt = new BitmapText(PixelScene.pixelFont);
		txt.text( Integer.toString(heat) );
		txt.hardlight(CharSprite.POSITIVE);
		txt.measure();
		return txt;
	}

	@Override
	public int indicatorColor() {
		return 0xC8541E;
	}

	@Override
	public void doAction() {
		if (heat < HEAT_PER_TURN) return;
		Hero hero = (Hero) target;
		Buff.prolong(hero, Adrenaline.class, heat / HEAT_PER_TURN);
		heat = 0;
		coolTurns = 0;
		hero.sprite.emitter().burst(FlameParticle.FACTORY, 10);
		Sample.INSTANCE.play(Assets.Sounds.BURNING);
		ActionIndicator.clearAction(this);
		BuffIndicator.refreshHero();
	}

	private static final String HEAT = "heat";
	private static final String COOL = "cool_turns";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(HEAT, heat);
		bundle.put(COOL, coolTurns);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		heat = bundle.getInt(HEAT);
		coolTurns = bundle.getInt(COOL);
	}
}

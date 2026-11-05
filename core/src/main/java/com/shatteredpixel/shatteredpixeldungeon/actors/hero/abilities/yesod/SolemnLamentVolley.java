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


package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.yesod;

import com.shatteredpixel.shatteredpixeldungeon.lobotomy.HeroDamageBonus;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.AbilityCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.EgoGun;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.SolemnLament;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

//Yesod's third armor ability: fires solemn lament's bullets one by one, each aimed separately.
// No time passes while firing, the volley takes 1 turn once it ends
public class SolemnLamentVolley extends ArmorAbility {

	public static final int BASE_SHOTS = 7;

	{
		baseChargeUse = 50f;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt", shots(Dungeon.hero));
	}

	//coffin: +1/2/3/4 shots
	public static int shots( Hero hero ){
		return BASE_SHOTS + (hero == null ? 0 : hero.pointsInTalent(Talent.COFFIN));
	}

	@Override
	protected void activate(AbilityCrown armor, Hero hero, Integer target) {
		if (target == null || target == hero.pos) return;

		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		//the bullets use the level of the equipped weapon
		SolemnLament gun = new SolemnLament();
		KindOfWeapon wep = hero.belongings.weapon();
		gun.level(wep == null ? 0 : wep.buffedLvl());

		fire(hero, gun, target, shots(hero));
	}

	private void fire( final Hero hero, final SolemnLament gun, int target, final int remaining ){
		hero.busy();
		hero.sprite.zap(target);
		Invisibility.dispel();
		Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH, 1f, 0.8f);

		final int cell = new Ballistica(hero.pos, target, Ballistica.PROJECTILE).collisionPos;
		MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.SHAMAN_WHITE_PURPLE, hero.sprite, cell, new Callback() {
			@Override
			public void call() {
				Char ch = Actor.findChar(cell);
				if (ch != null && ch != hero){
					boolean hit = hero.shoot(ch, new EgoGun.Shot(gun));
					if (hit && ch.isAlive()){
						//like the E.G.O itself, a separate BLACK hit follows the WHITE one
						int dmg = HeroDamageBonus.direct(hero, ch, new EgoGun.Shot(gun).damageRoll(hero), ch.drRoll());
						dmg = DamageType.apply(ch, DamageType.BLACK, dmg);
						ch.damage(dmg, hero);
					}
					if (hit){
						//dead butterfly: 3/4/5/6 fixed damage
						if (hero.hasTalent(Talent.DEAD_BUTTERFLY) && ch.isAlive()){
							ch.damage(2 + hero.pointsInTalent(Talent.DEAD_BUTTERFLY), hero);
						}
						//living butterfly: 1/2/3/4 shielding
						if (hero.hasTalent(Talent.LIVING_BUTTERFLY)){
							Buff.affect(hero, Barrier.class).incShield(hero.pointsInTalent(Talent.LIVING_BUTTERFLY));
						}
					}
				}

				int left = remaining - 1;
				if (left > 0 && hero.isAlive()){
					hero.ready = true;
					GameScene.selectCell(new CellSelector.Listener() {
						//the scene resets to its default selector right after a selection, which sends this
						// listener a null afterwards; only the first answer counts
						private boolean answered = false;

						@Override
						public void onSelect(Integer next) {
							if (answered) return;
							answered = true;
							if (next == null || next == hero.pos){
								finish(hero);
							} else {
								fire(hero, gun, next, left);
							}
						}

						@Override
						public String prompt() {
							return Messages.get(SolemnLamentVolley.class, "prompt", left);
						}
					});
				} else {
					finish(hero);
				}
			}
		});
	}

	private void finish( Hero hero ){
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.SOLEMN_LAMENT;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.COFFIN, Talent.DEAD_BUTTERFLY, Talent.LIVING_BUTTERFLY, Talent.HEROIC_ENERGY};
	}
}

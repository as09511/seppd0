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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Mace;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.CombinedEgo;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//combined E.G.O (TETH, balanced as tier 4): Red Eyes and Penitence swung together.
//Each hit deals its damage as both RED and WHITE, bleeds for 10% of the RED part and shields for 10% of the WHITE part.
//Hitting a bleeding enemy or being hit while shielded charges it; at 20 charge it can smash a head for real.
public class RedEyesPenitence extends EgoWeapon implements CombinedEgo {

	public static final String AC_SMASH = "SMASH";

	public static final int MAX_CHARGE = 20;

	private static final float RATIO       = 0.1f;
	private static final float SMASH_RATIO = 0.2f;
	private static final float SMASH_DAMAGE = 2f;

	{
		image = ItemSpriteSheet.RED_EYES_PENITENCE;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 0.9f;

		risk = RiskLevel.TETH;
		damageType = DamageType.RED;
		tier = 4;
	}

	private int charge = 0;
	private boolean smashing = false;

	@Override
	public int min(int lvl) {
		return  2 +             //2 base
				lvl;            //+1 per level
	}

	@Override
	public int max(int lvl) {
		return  12 +            //12 base
				3*lvl;          //+3 per level
	}

	//refined like a WAW E.G.O, as it is balanced as tier 4
	@Override
	public RiskLevel costRisk() {
		return RiskLevel.WAW;
	}

	@Override
	protected String damageTypeName() {
		return DamageType.RED.coloredName() + "·" + DamageType.WHITE.coloredName();
	}

	public int charge(){
		return charge;
	}

	public void gainCharge( Hero hero ){
		if (charge >= MAX_CHARGE) return;
		charge++;
		updateQuickslot();
		if (charge == MAX_CHARGE){
			GLog.p(Messages.get(this, "charged"));
		}
	}

	//called when the hero takes a hit, see Hero.damage
	public static void onHeroHit( Hero hero, Object src ){
		if (src instanceof Char && hero.shielding() > 0 && hero.belongings.weapon() instanceof RedEyesPenitence){
			((RedEyesPenitence) hero.belongings.weapon()).gainCharge(hero);
		}
	}

	@Override
	public int proc( Char attacker, Char defender, int damage ) {
		boolean wasBleeding = defender.buff(Bleeding.class) != null;
		float ratio = smashing ? SMASH_RATIO : RATIO;

		//the Penitence half: the same damage dealt again as WHITE, part of it kept as a shield
		int white = DamageType.apply(defender, DamageType.WHITE, damage);
		if (white > 0 && defender.isAlive()){
			defender.damage(white, attacker);
			if (attacker instanceof Hero && attacker.isAlive()){
				Buff.affect(attacker, Barrier.class).incShield((int)Math.ceil(white * ratio));
			}
		}

		//the Red Eyes half, which is the hit itself
		int red = super.proc(attacker, defender, damage);
		if (red > 0 && defender.isAlive()){
			Buff.affect(defender, Bleeding.class).set((float)Math.ceil(red * ratio));
		}

		if (wasBleeding && attacker instanceof Hero){
			gainCharge((Hero) attacker);
		}
		return red;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero)) actions.add(AC_SMASH);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute(hero, action);

		if (action.equals(AC_SMASH)){
			if (hero.belongings.weapon() != this){
				GLog.w(Messages.get(this, "need_equip"));
			} else if (charge < MAX_CHARGE){
				GLog.w(Messages.get(this, "not_charged", charge, MAX_CHARGE));
			} else {
				usesTargeting = true;
				GameScene.selectCell(smasher);
			}
		}
	}

	private final CellSelector.Listener smasher = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer cell ) {
			if (cell == null) return;
			Hero hero = Dungeon.hero;
			Char enemy = Actor.findChar(cell);
			if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[cell]){
				GLog.w(Messages.get(RedEyesPenitence.this, "no_target"));
				return;
			}
			if (hero.belongings.weapon() != RedEyesPenitence.this || !hero.canAttack(enemy)){
				GLog.w(Messages.get(RedEyesPenitence.this, "target_range"));
				return;
			}
			hero.busy();
			hero.sprite.attack(enemy.pos, () -> {
				AttackIndicator.target(enemy);
				smashing = true;
				boolean hit = hero.attack(enemy, SMASH_DAMAGE, 0, 1f);
				smashing = false;
				charge = 0;
				updateQuickslot();
				if (hit){
					Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
					PixelScene.shake(2, 0.3f);
				}
				hero.sprite.showStatus(0xFF4040, Messages.get(RedEyesPenitence.this, "smash_name"));
				Invisibility.dispel();
				hero.spendAndNext(hero.attackDelay());
			});
		}

		@Override
		public String prompt() {
			return Messages.get(RedEyesPenitence.this, "smash_prompt");
		}
	};

	@Override
	public String status() {
		return charge + "/" + MAX_CHARGE;
	}

	@Override
	public String statsInfo() {
		return Messages.get(this, "stats_desc", charge, MAX_CHARGE);
	}

	@Override
	protected int abilityBoost( int lvl ) {
		//same as Red Eyes and Penitence
		return 4 + Math.round(1.5f*lvl);
	}

	@Override
	protected void egoAbility( Hero hero, Integer target, int dmgBoost ) {
		Mace.heavyBlowAbility(hero, target, 1, dmgBoost, this);
	}

	private static final String CHARGE = "charge";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(CHARGE, charge);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		charge = bundle.getInt(CHARGE);
	}

	//Red Eyes (the weapon) + Penitence + 10 energy. The result keeps the higher upgrade level of the two.
	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {

		{
			inputs =  new Class[]{RedEyes.class, Penitence.class};
			inQuantity = new int[]{1, 1};

			cost = 10;

			output = RedEyesPenitence.class;
			outQuantity = 1;
		}

		@Override
		public Item brew( ArrayList<Item> ingredients ) {
			int level = 0;
			for (Item i : ingredients){
				if (i instanceof RedEyes || i instanceof Penitence){
					level = Math.max(level, i.level());
				}
			}
			Item result = super.brew(ingredients);
			if (result != null){
				if (level > 0) result.upgrade(level);
				result.identify();
			}
			return result;
		}
	}
}

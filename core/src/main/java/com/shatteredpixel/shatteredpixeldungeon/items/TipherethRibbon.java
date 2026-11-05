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
package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//Tiphereth's starting item, the weapon counterpart of the warrior's broken seal: it is tied to a melee weapon,
//carries up to 1 upgrade between weapons and gives the weapon it is tied to +1 melee damage.
//With the maintenance talent it carries the weapon's enchantment too.
public class TipherethRibbon extends Item {

	public static final String AC_AFFIX = "AFFIX";

	public static final int DAMAGE_BONUS = 1;

	{
		image = ItemSpriteSheet.TIPHERETH_RIBBON;

		cursedKnown = levelKnown = true;
		unique = true;
		bones = false;

		defaultAction = AC_AFFIX;
	}

	private Weapon.Enchantment enchantment;

	public Weapon.Enchantment getEnchantment(){
		return enchantment;
	}

	public void setEnchantment( Weapon.Enchantment ench ){
		this.enchantment = ench;
	}

	//maintenance: the ribbon carries the weapon's enchantment when it is moved
	public boolean canTransferEnchantment(){
		return enchantment != null && Dungeon.hero != null && Dungeon.hero.hasTalent(Talent.MAINTENANCE);
	}

	//maintenance +2: the enchantment of the weapon the ribbon is tied to is never lost to upgrades
	public static boolean keepsEnchantment(){
		return Dungeon.hero != null && Dungeon.hero.pointsInTalent(Talent.MAINTENANCE) >= 2;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return enchantment != null ? enchantment.glowing() : null;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_AFFIX);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute(hero, action);
		if (action.equals(AC_AFFIX)){
			curItem = this;
			GameScene.selectItem(weaponSelector);
		}
	}

	//outgoing is either the ribbon itself as an item, or the melee weapon it is tied to
	public void affixToWeapon( MeleeWeapon weapon, Item outgoing ){
		if (weapon == null) return;
		if (!weapon.cursedKnown){
			GLog.w(Messages.get(TipherethRibbon.class, "unknown_weapon"));
			return;
		}
		if (weapon.cursed && (enchantment == null || !enchantment.curse())){
			GLog.w(Messages.get(TipherethRibbon.class, "cursed_weapon"));
			return;
		}
		if (weapon.enchantment != null && enchantment != null
				&& (canTransferEnchantment() || outgoing == this)
				&& weapon.enchantment.getClass() != enchantment.getClass()){
			//both have an enchantment, the hero picks which one stays
			GameScene.show(new WndOptions(new ItemSprite(ItemSpriteSheet.TIPHERETH_RIBBON),
					Messages.get(TipherethRibbon.class, "choose_title"),
					Messages.get(TipherethRibbon.class, "choose_desc", weapon.enchantment.name(), enchantment.name()),
					weapon.enchantment.name(),
					enchantment.name()){
				@Override
				protected void onSelect( int index ) {
					if (index == -1) return;
					release(outgoing);
					if (index == 0) setEnchantment(null);
					tie(weapon);
				}
			});
		} else {
			release(outgoing);
			tie(weapon);
		}
	}

	private void release( Item outgoing ){
		if (outgoing == this){
			detach(Dungeon.hero.belongings.backpack);
		} else if (outgoing instanceof MeleeWeapon){
			((MeleeWeapon) outgoing).detachRibbon();
		}
	}

	private void tie( MeleeWeapon weapon ){
		GLog.p(Messages.get(TipherethRibbon.class, "affix", weapon.name()));
		Dungeon.hero.sprite.operate(Dungeon.hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.UNLOCK);
		weapon.affixRibbon(this);
		updateQuickslot();
	}

	@Override
	public String name() {
		return enchantment != null ? enchantment.name(super.name()) : super.name();
	}

	@Override
	public String info() {
		String info = super.info();
		if (enchantment != null){
			info += "\n\n" + Messages.get(this, "enchanted", enchantment.name());
			info += " " + enchantment.desc();
		}
		return info;
	}

	//a scroll of upgrade works on the ribbon itself once, the same as upgrading the weapon it is tied to
	@Override
	public boolean isUpgradable() {
		return level() == 0;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 0;
	}

	protected static WndBag.ItemSelector weaponSelector = new WndBag.ItemSelector() {

		@Override
		public String textPrompt() {
			return Messages.get(TipherethRibbon.class, "prompt");
		}

		@Override
		public Class<? extends Bag> preferredBag(){
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable( Item item ) {
			return item instanceof MeleeWeapon && ((MeleeWeapon) item).checkRibbon() == null;
		}

		@Override
		public void onSelect( Item item ) {
			if (item instanceof MeleeWeapon) {
				TipherethRibbon ribbon = (TipherethRibbon) curItem;
				ribbon.affixToWeapon((MeleeWeapon) item, ribbon);
			}
		}
	};

	private static final String ENCHANTMENT = "enchantment";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(ENCHANTMENT, enchantment);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		enchantment = (Weapon.Enchantment) bundle.get(ENCHANTMENT);
	}
}

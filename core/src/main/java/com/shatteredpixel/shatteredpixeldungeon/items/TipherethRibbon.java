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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce;
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
//The natural science floor librarian can wear it as her weapon instead: her attacks count as unarmed and deal RED damage
//rolled like a ring of force of the ribbon's level (a worn ring of force doesn't stack, the higher counts).
//Its strength requirement starts at 10 and goes down with upgrades like any weapon's.
public class TipherethRibbon extends MeleeWeapon {

	public static final String AC_AFFIX = "AFFIX";

	public static final int DAMAGE_BONUS = 1;

	{
		image = ItemSpriteSheet.TIPHERETH_RIBBON;

		tier = 1;
		cursedKnown = levelKnown = true;
		unique = true;
		bones = false;

		defaultAction = AC_AFFIX;
	}

	//the enchantment the ribbon carries between weapons
	private Weapon.Enchantment carried;

	public Weapon.Enchantment getEnchantment(){
		return carried;
	}

	public void setEnchantment( Weapon.Enchantment ench ){
		this.carried = ench;
	}

	//only the natural science floor librarian wears the ribbon
	public static boolean wearable( Hero hero ){
		return hero != null && hero.subClass == HeroSubClass.SCIENCE_LIBRARIAN;
	}

	public static TipherethRibbon worn( Char ch ){
		if (ch instanceof Hero && ((Hero) ch).belongings.weapon() instanceof TipherethRibbon){
			return (TipherethRibbon) ((Hero) ch).belongings.weapon();
		}
		return null;
	}

	//proof of competence: +1/2/3 to the ribbon's level and strength requirement
	public static int competence( Hero hero ){
		return hero == null ? 0 : hero.pointsInTalent(Talent.PROOF_OF_COMPETENCE);
	}

	@Override
	public int buffedLvl() {
		//the ribbon's own level only counts while it is worn, its bonus on a tied weapon is applied there
		return super.buffedLvl() + (isEquipped(Dungeon.hero) ? competence(Dungeon.hero) : 0);
	}

	//10 at tier 1, lowered by upgrades and the potion of mastery like any weapon, raised by proof of competence
	@Override
	public int STRReq( int lvl ) {
		return super.STRReq(lvl) + competence(Dungeon.hero);
	}

	//the punches are rolled like a ring of force of the ribbon's level
	@Override
	public int min( int lvl ) {
		return RingOfForce.unarmedMin(lvl, Dungeon.hero == null ? 10 : Dungeon.hero.STR());
	}

	@Override
	public int max( int lvl ) {
		return RingOfForce.unarmedMax(lvl, Dungeon.hero == null ? 10 : Dungeon.hero.STR());
	}

	@Override
	public String defaultAction() {
		if (Dungeon.hero != null && wearable(Dungeon.hero)){
			return isEquipped(Dungeon.hero) ? AC_UNEQUIP : AC_EQUIP;
		}
		return AC_AFFIX;
	}

	@Override
	public boolean doEquip( Hero hero ) {
		if (!wearable(hero)) return false;
		return super.doEquip(hero);
	}

	//maintenance: the ribbon carries the weapon's enchantment when it is moved
	public boolean canTransferEnchantment(){
		return carried != null && Dungeon.hero != null && Dungeon.hero.hasTalent(Talent.MAINTENANCE);
	}

	//maintenance +2: the enchantment of the weapon the ribbon is tied to is never lost to upgrades
	public static boolean keepsEnchantment(){
		return Dungeon.hero != null && Dungeon.hero.pointsInTalent(Talent.MAINTENANCE) >= 2;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return carried != null ? carried.glowing() : null;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		actions.remove(AC_ABILITY);
		if (!wearable(hero)){
			actions.remove(AC_EQUIP);
		}
		if (!isEquipped(hero)){
			actions.add(AC_AFFIX);
		}
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
		if (weapon.cursed && (carried == null || !carried.curse())){
			GLog.w(Messages.get(TipherethRibbon.class, "cursed_weapon"));
			return;
		}
		if (weapon.enchantment != null && carried != null
				&& (canTransferEnchantment() || outgoing == this)
				&& weapon.enchantment.getClass() != carried.getClass()){
			//both have an enchantment, the hero picks which one stays
			GameScene.show(new WndOptions(new ItemSprite(ItemSpriteSheet.TIPHERETH_RIBBON),
					Messages.get(TipherethRibbon.class, "choose_title"),
					Messages.get(TipherethRibbon.class, "choose_desc", weapon.enchantment.name(), carried.name()),
					weapon.enchantment.name(),
					carried.name()){
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
		return carried != null ? carried.name(trueName()) : trueName();
	}

	@Override
	public String info() {
		String info = desc();
		if (Dungeon.hero != null && wearable(Dungeon.hero)){
			int lvl = buffedLvl();
			info += "\n\n" + Messages.get(this, "worn_stats", min(lvl), max(lvl), STRReq(), Math.max(0, lvl));
		}
		if (carried != null){
			info += "\n\n" + Messages.get(this, "enchanted", carried.name());
			info += " " + carried.desc();
		}
		return info;
	}

	//a scroll of upgrade works on the ribbon itself once, the same as upgrading the weapon it is tied to.
	//The librarian wears it as her weapon, so she can upgrade it like one
	@Override
	public boolean isUpgradable() {
		return level() == 0 || (Dungeon.hero != null && wearable(Dungeon.hero));
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public Item identify( boolean byHero ) {
		levelKnown = cursedKnown = true;
		return this;
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
			return item instanceof MeleeWeapon && !(item instanceof TipherethRibbon) && ((MeleeWeapon) item).checkRibbon() == null;
		}

		@Override
		public void onSelect( Item item ) {
			if (item instanceof MeleeWeapon) {
				TipherethRibbon ribbon = (TipherethRibbon) curItem;
				ribbon.affixToWeapon((MeleeWeapon) item, ribbon);
			}
		}
	};

	private static final String CARRIED = "carried_enchantment";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(CARRIED, carried);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		carried = (Weapon.Enchantment) bundle.get(CARRIED);
		//ribbons from the first build kept their enchantment under the weapon's own key
		if (carried == null && enchantment != null){
			carried = enchantment;
			enchantment = null;
		}
	}
}

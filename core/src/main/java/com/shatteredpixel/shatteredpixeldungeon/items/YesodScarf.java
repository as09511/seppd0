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

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import java.util.Collections;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SmokeScreen;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.yesod.Charge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.yesod.Analyzed;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.yesod.InfoPower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.yesod.Prediction;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.yesod.YesodTrackers;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//Yesod's starting item: spends the information he gathers on his abilities
public class YesodScarf extends Item {

	//quickslot / default action: choose which ability to use
	public static final String AC_USE     = "USE";
	public static final String AC_ANALYZE = "ANALYZE";
	public static final String AC_DETECT  = "DETECT";
	public static final String AC_PREDICT = "PREDICT";
	public static final String AC_REVIEW  = "REVIEW";
	public static final String AC_CENSOR  = "CENSOR";
	public static final String AC_CHARGE  = "CHARGE";

	public static final int ANALYZE_COST = 2;
	public static final int DETECT_COST  = 1;
	public static final int PREDICT_COST = 3;
	public static final int CENSOR_COST  = 5;
	//info exchange: gold value of one point of info in shops
	public static final int INFO_EXCHANGE_GOLD = 75;

	public static final float CENSOR_DURATION = 5f;

	public static final int DETECT_RANGE = 8;

	{
		image = ItemSpriteSheet.YESOD_SCARF;

		defaultAction = AC_USE;

		unique = true;
		bones = false;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		if (hero.buff(MagicImmune.class) == null) {
			actions.add(AC_DETECT);
			actions.add(AC_ANALYZE);
			actions.add(AC_PREDICT);
			if (hero.subClass == HeroSubClass.INFO_SEPHIRAH) actions.add(AC_CENSOR);
			if (hero.subClass == HeroSubClass.TECH_LIBRARIAN) actions.add(AC_CHARGE);
		}
		if (reviewCharges(hero) > 0){
			actions.add(AC_REVIEW);
		}
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		switch (action){
			case AC_ANALYZE: return Messages.get(this, "ac_analyze", ANALYZE_COST);
			case AC_DETECT:  return Messages.get(this, "ac_detect", DETECT_COST);
			case AC_PREDICT: return Messages.get(this, "ac_predict", PREDICT_COST);
			case AC_REVIEW:  return Messages.get(this, "ac_review", reviewCharges(hero));
			case AC_CENSOR:  return Messages.get(this, "ac_censor", CENSOR_COST);
			case AC_CHARGE:  return Messages.get(this, "ac_charge");
		}
		return super.actionName(action, hero);
	}

	private static boolean canPay( int cost ){
		if (InfoPower.current() < cost){
			GLog.w( Messages.get(YesodScarf.class, "no_info", cost) );
			return false;
		}
		return true;
	}

	@Override
	public void execute( Hero hero, String action ) {

		super.execute(hero, action);

		if (action.equals(AC_USE)){
			chooseAbility(hero);
			return;
		}

		if (action.equals(AC_REVIEW)){
			openDocumentReview(hero);
			return;
		}

		if (hero.buff(MagicImmune.class) != null) return;

		switch (action){
			case AC_ANALYZE:
				if (canPay(ANALYZE_COST)) GameScene.selectCell(analyzer);
				break;
			case AC_DETECT:
				if (canPay(DETECT_COST)){
					InfoPower.get().spend(DETECT_COST);
					detect(hero);
					hero.sprite.operate(hero.pos);
					hero.spendAndNext(1f);
				}
				break;
			case AC_PREDICT:
				if (canPay(PREDICT_COST)){
					InfoPower.get().spend(PREDICT_COST);
					Buff.affect(hero, Prediction.Evade.class);
					Buff.affect(hero, Prediction.Strike.class);
					hero.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 6);
					Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
					hero.sprite.operate(hero.pos);
					hero.spendAndNext(1f);
				}
				break;
			case AC_CENSOR:
				if (canPay(CENSOR_COST)){
					InfoPower.get().spend(CENSOR_COST);
					censor(hero);
					hero.spendAndNext(1f);
				}
				break;
			case AC_CHARGE:
				charge(hero);
				break;
		}
	}

	//every ability the scarf can use right now, in the order of the item window
	private ArrayList<String> abilities( Hero hero ){
		ArrayList<String> list = new ArrayList<>();
		for (String a : actions(hero)){
			if (a.equals(AC_ANALYZE) || a.equals(AC_DETECT) || a.equals(AC_PREDICT)
					|| a.equals(AC_CENSOR) || a.equals(AC_CHARGE) || a.equals(AC_REVIEW)){
				list.add(a);
			}
		}
		return list;
	}

	private static boolean affordable( String action ){
		int info = InfoPower.current();
		switch (action){
			case AC_ANALYZE: return info >= ANALYZE_COST;
			case AC_DETECT:  return info >= DETECT_COST;
			case AC_PREDICT: return info >= PREDICT_COST;
			case AC_CENSOR:  return info >= CENSOR_COST;
			case AC_CHARGE:  return info > 0;
			default:         return true;
		}
	}

	//used from the quickslot: a window listing the abilities, unaffordable ones greyed out
	private void chooseAbility( final Hero hero ){
		final ArrayList<String> list = abilities(hero);
		if (list.isEmpty()) return;
		String[] names = new String[list.size()];
		for (int i = 0; i < names.length; i++){
			names[i] = actionName(list.get(i), hero);
		}
		GameScene.show(new WndOptions(new ItemSprite(this),
				Messages.titleCase(name()),
				Messages.get(YesodScarf.class, "choose_msg", InfoPower.current(), InfoPower.max(hero)),
				names){
			@Override
			protected boolean enabled(int index) {
				return affordable(list.get(index));
			}

			@Override
			protected void onSelect(int index) {
				execute(hero, list.get(index));
			}
		});
	}

	//Information Team Sephirah: 5 turns of invisibility and a 5x5 smoke screen
	private static void censor( Hero hero ){
		Buff.affect(hero, Invisibility.class, CENSOR_DURATION);
		for (int dx = -2; dx <= 2; dx++){
			for (int dy = -2; dy <= 2; dy++){
				int cell = hero.pos + dx + dy*Dungeon.level.width();
				if (cell >= 0 && cell < Dungeon.level.length() && !Dungeon.level.solid[cell]
						&& Dungeon.level.distance(hero.pos, cell) <= 2){
					GameScene.add(Blob.seed(cell, (int)CENSOR_DURATION, SmokeScreen.class));
				}
			}
		}
		CellEmitter.get(hero.pos).burst(Speck.factory(Speck.SMOKE), 10);
		Sample.INSTANCE.play(Assets.Sounds.PUFF);
		hero.sprite.operate(hero.pos);
	}

	//Technological Sciences Floor Librarian: all info becomes charge, takes no time
	private static void charge( Hero hero ){
		InfoPower p = InfoPower.get();
		int amount = p == null ? 0 : p.info();
		if (amount <= 0){
			GLog.w(Messages.get(YesodScarf.class, "no_charge"));
			return;
		}
		p.spend(amount);
		Buff.affect(hero, Charge.class).add(amount);
		hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 8);
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
		GLog.p(Messages.get(YesodScarf.class, "charged", amount));
	}

	//reveals the map, traps and secrets within range. Secrets found this way give no info
	private static void detect( Hero hero ){
		int[] map = Dungeon.level.map;
		boolean[] mapped = Dungeon.level.mapped;
		boolean[] discoverable = Dungeon.level.discoverable;
		boolean noticed = false;
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (!discoverable[i] || Dungeon.level.distance(hero.pos, i) > DETECT_RANGE) continue;
			int terr = map[i];
			mapped[i] = true;
			if ((Terrain.flags[terr] & Terrain.SECRET) != 0){
				Dungeon.level.discover(i);
				if (Dungeon.level.heroFOV[i]){
					GameScene.discoverTile(i, terr);
					ScrollOfMagicMapping.discover(i);
					noticed = true;
				}
			}
		}
		GameScene.updateFog();
		Dungeon.observe();
		if (noticed) Sample.INSTANCE.play(Assets.Sounds.SECRET);
		SpellSprite.show(hero, SpellSprite.MAP);
		Sample.INSTANCE.play(Assets.Sounds.SCAN);
	}

	private static final CellSelector.Listener analyzer = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell == null) return;
			Hero hero = Dungeon.hero;
			Char ch = Actor.findChar(cell);
			if (ch == null || ch == hero || !Dungeon.level.heroFOV[cell]){
				GLog.w(Messages.get(YesodScarf.class, "no_target"));
				return;
			}
			InfoPower p = InfoPower.get();
			if (p == null || !p.spend(ANALYZE_COST)) return;
			Analyzed.apply(ch, Analyzed.DURATION);
			//information spread: 1/2/3 more enemies close to the target are analyzed too
			if (hero.hasTalent(Talent.INFO_SPREAD)){
				ArrayList<Mob> others = new ArrayList<>();
				for (Mob m : Dungeon.level.mobs){
					if (m != ch && m.isAlive() && m.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[m.pos]){
						others.add(m);
					}
				}
				final int target = ch.pos;
				Collections.sort(others, (a, b) -> Integer.compare(Dungeon.level.distance(target, a.pos), Dungeon.level.distance(target, b.pos)));
				for (int i = 0; i < Math.min(others.size(), hero.pointsInTalent(Talent.INFO_SPREAD)); i++){
					Analyzed.apply(others.get(i), Analyzed.DURATION);
					if (others.get(i).sprite != null){
						others.get(i).sprite.emitter().burst(Speck.factory(Speck.DISCOVER), 4);
					}
				}
			}
			if (ch.sprite != null){
				ch.sprite.emitter().burst(Speck.factory(Speck.DISCOVER), 8);
			}
			Sample.INSTANCE.play(Assets.Sounds.DEBUFF);
			hero.sprite.zap(cell);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(YesodScarf.class, "analyze_prompt");
		}
	};

	//document review talent: free identifications, one per talent point
	public static int reviewCharges( Hero hero ){
		YesodTrackers.DocumentReviewCharges c = hero.buff(YesodTrackers.DocumentReviewCharges.class);
		return c == null ? 0 : (int)c.count();
	}

	public static void openDocumentReview( Hero hero ){
		if (reviewCharges(hero) <= 0) return;
		boolean anyUnknown = false;
		for (Item i : hero.belongings){
			if (!i.isIdentified()){
				anyUnknown = true;
				break;
			}
		}
		if (!anyUnknown){
			GLog.i(Messages.get(YesodScarf.class, "review_nothing"));
			return;
		}
		GameScene.selectItem(new WndBag.ItemSelector() {
			@Override
			public String textPrompt() {
				return Messages.get(YesodScarf.class, "review_prompt");
			}

			@Override
			public boolean itemSelectable(Item item) {
				return !item.isIdentified();
			}

			@Override
			public void onSelect(Item item) {
				if (item == null || item.isIdentified()) return;
				YesodTrackers.DocumentReviewCharges c = Dungeon.hero.buff(YesodTrackers.DocumentReviewCharges.class);
				if (c == null || c.count() <= 0) return;
				c.countDown(1);
				if (c.count() <= 0) c.detach();
				item.identify();
				GLog.p(Messages.get(YesodScarf.class, "review_done", item.name()));
				Dungeon.hero.sprite.emitter().burst(Speck.factory(Speck.DISCOVER), 6);
				Sample.INSTANCE.play(Assets.Sounds.READ);
				updateQuickslot();
			}
		});
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public String status() {
		if (Dungeon.hero == null) return null;
		return Messages.format("%d/%d", InfoPower.current(), InfoPower.max(Dungeon.hero));
	}

	@Override
	public String desc() {
		String desc = super.desc();
		desc += "\n\n" + Messages.get(this, "effect", DETECT_COST, DETECT_RANGE, ANALYZE_COST, (int)Analyzed.DURATION, PREDICT_COST);
		if (Dungeon.hero != null && Dungeon.hero.subClass == HeroSubClass.INFO_SEPHIRAH){
			desc += "\n\n" + Messages.get(this, "effect_info", CENSOR_COST, (int)CENSOR_DURATION, INFO_EXCHANGE_GOLD);
		} else if (Dungeon.hero != null && Dungeon.hero.subClass == HeroSubClass.TECH_LIBRARIAN){
			desc += "\n\n" + Messages.get(this, "effect_tech", Math.round(100*Charge.DAMAGE_PER_POINT));
		}
		//the turn cost note always goes last, below any abilities from the subclass
		if (Dungeon.hero != null && Dungeon.hero.subClass == HeroSubClass.TECH_LIBRARIAN){
			desc += "\n\n" + Messages.get(this, "turn_note_tech");
		} else {
			desc += "\n\n" + Messages.get(this, "turn_note");
		}
		return desc;
	}

	@Override
	public int value() {
		return 0;
	}
}

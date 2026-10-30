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


package com.shatteredpixel.shatteredpixeldungeon.lobotomy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Adapts Shattered Pixel Dungeon's own translated stat sentences for E.G.O,
//so they appear in whatever language the game is set to
public class EgoText {

	//the highlighted span containing the tier, e.g. "_tier-%1$d_", "_%1$d티어_", "_Stufe %1$d_"
	private static final Pattern TIER_SPAN = Pattern.compile("_[^_]*%1\\$d[^_]*_");

	//the max damage plus anything attached to it, e.g. "%3$d" in "1-10 damage" or "%3$d의" in "1-10의 피해"
	private static final Pattern MAX_DAMAGE = Pattern.compile("%3\\$d[^\\s_]*");

	//replaces the tier with a string argument (%1$s), used for the colored risk level
	public static String riskTemplate(String template){
		Matcher m = TIER_SPAN.matcher(template);
		if (m.find()){
			return template.substring(0, m.start()) + "%1$s" + template.substring(m.end());
		} else {
			return template.replace("%1$d", "%1$s");
		}
	}

	//adds the damage type (as argument %5$s) right after the damage range
	public static String withDamageType(String template){
		Matcher m = MAX_DAMAGE.matcher(template);
		if (m.find()){
			return template.substring(0, m.end()) + " %5$s" + template.substring(m.end());
		} else {
			return template;
		}
	}

}

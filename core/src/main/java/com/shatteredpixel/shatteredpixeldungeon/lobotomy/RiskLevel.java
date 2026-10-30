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

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

//Lobotomy Corporation's risk levels, used by abnormalities and their E.G.O
public enum RiskLevel {
	ZAYIN, TETH, HE, WAW, ALEPH;

	public int tier(){
		return ordinal()+1;
	}

	//equipment without a risk level is standard-issue gear, such as the suit and baton
	public static String desc(RiskLevel risk){
		String name = Messages.get(RiskLevel.class, risk == null ? "standard" : risk.name());
		return Messages.get(RiskLevel.class, "risk", name);
	}
}

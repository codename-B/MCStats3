package com.ubempire.MCStats3.service;
//Copyright (C) 2010  Ryan Michela
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

import java.util.ArrayList;

import org.bukkit.entity.Player;

import com.ubempire.MCStats3.StatsPlugin;

public class GroupService {
	StatsPlugin plugin;
	PlayerInfo info;
	private String[] ignoreGroups;
	
	public GroupService(StatsPlugin plugin, String[] ignoreGroups) {
		this.ignoreGroups = ignoreGroups;
		this.plugin = plugin;

	    if (plugin.infoservice != null) {
	    	this.info = plugin.infoservice;
	        
	    } else {
	        plugin.getServer().getLogger().info("[MCStats] bInfo not detected, disabling group support.");
        }
	}
	
	public String[] getGroups(Player player) {
		ArrayList<String> playerGroups = new ArrayList<String>();
		
		if(player.isOp()) {
			playerGroups.add("Ops");
		}
		
		if(info != null) {
			ArrayList<String> keepGroups = new ArrayList<String>();
			
			// Try to filter down to only explicitly assigned permissions. This is not implemented
			// in the FakePermissions GroupManager plugin and will throw an error if GroupManager
			// is installed
			try {
				String prefix = info.getPrefix(player);
				keepGroups.add(toTitleCase(prefix));
				String suffix = info.getPrefix(player);
				keepGroups.add(toTitleCase(suffix));
				}
			 catch(NoSuchMethodError e) {}
			
			for(String ignore : ignoreGroups) {
				String tcIgnore = toTitleCase(ignore);
				if(keepGroups.contains(tcIgnore)) {
					keepGroups.remove(tcIgnore);
				}
			}
			
			playerGroups.addAll(keepGroups);
		}
		
		return playerGroups.toArray(new String[]{});
	}
	
	private String toTitleCase(String groupName) {
		String[] splits = groupName.split(" ");
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < splits.length; i++) {
			if(i > 0) sb.append(" ");
			sb.append(splits[i].substring(0,1).toUpperCase() + splits[i].substring(1).toLowerCase());
		}
		
		return sb.toString();
	}
}

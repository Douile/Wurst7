/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.wurstclient.DontHide;
import net.wurstclient.Feature;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.TooManyHaxHack;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.MathUtils;
import net.wurstclient.util.json.JsonException;

@DontHide
public final class TooManyHaxCmd extends Command
{
	public TooManyHaxCmd()
	{
		super("toomanyhax",
			"Allows to manage which hacks should be hidden\n"
				+ "when TooManyHax is enabled.",
			".toomanyhax hide <feature>", ".toomanyhax unhide <feature>",
			".toomanyhax list [<page>]", ".toomanyhax load-profile <file>",
			".toomanyhax save-profile <file>",
			".toomanyhax list-profiles [<page>]",
			"Profiles are saved in '.minecraft/wurst/toomanyhax'.");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length < 1)
			throw new CmdSyntaxError();
		
		switch(args[0].toLowerCase())
		{
			case "hide":
			hide(args);
			break;
			
			case "unhide":
			unhide(args);
			break;
			
			case "list":
			list(args);
			break;
			
			case "load-profile":
			loadProfile(args);
			break;
			
			case "save-profile":
			saveProfile(args);
			break;
			
			case "list-profiles":
			listProfiles(args);
			break;
			
			default:
			throw new CmdSyntaxError();
		}
	}
	
	private void hide(String[] args) throws CmdException
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		
		String name = args[1];
		Feature feature = parseFeature(name);
		String typeAndName = getType(feature) + " '" + name + "'";
		
		TooManyHaxHack tooManyHax = WURST.getHax().tooManyHaxHack;
		if(tooManyHax.isHidden(feature))
		{
			ChatUtils.error("The " + typeAndName + " is already hidden.");
			
			if(!tooManyHax.isEnabled())
				ChatUtils.message("Enable TooManyHax to see the effect.");
			
			return;
		}
		
		tooManyHax.setHidden(feature, true);
		ChatUtils.message("Added " + typeAndName + " to TooManyHax list.");
	}
	
	private void unhide(String[] args) throws CmdException
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		
		String name = args[1];
		Feature feature = parseFeature(name);
		String typeAndName = getType(feature) + " '" + name + "'";
		
		TooManyHaxHack tooManyHax = WURST.getHax().tooManyHaxHack;
		if(!tooManyHax.isHidden(feature))
			throw new CmdError(
				"The " + typeAndName + " is already not hidden.");
		
		tooManyHax.setHidden(feature, false);
		ChatUtils.message("Removed " + typeAndName + " from TooManyHax list.");
	}
	
	private Feature parseFeature(String name) throws CmdSyntaxError
	{
		Feature feature = WURST.getFeatureByName(name);
		if(feature == null)
			throw new CmdSyntaxError(
				"A feature named '" + name + "' could not be found");
		
		return feature;
	}
	
	private String getType(Feature feature)
	{
		if(feature instanceof Hack)
			return "hack";
		
		if(feature instanceof Command)
			return "command";
		
		if(feature instanceof OtherFeature)
			return "feature";
		
		throw new IllegalStateException();
	}
	
	private void list(String[] args) throws CmdException
	{
		if(args.length > 2)
			throw new CmdSyntaxError();
		
		TooManyHaxHack tooManyHax = WURST.getHax().tooManyHaxHack;
		List<Feature> binds = tooManyHax.getHiddenFeatures();
		int page = parsePage(args);
		int pages = (int)Math.ceil(binds.size() / 8.0);
		pages = Math.max(pages, 1);
		
		if(page > pages || page < 1)
			throw new CmdSyntaxError("Invalid page: " + page);
		
		String total = "Total: " + binds.size() + " hidden feature";
		total += binds.size() != 1 ? "s" : "";
		ChatUtils.message(total);
		
		int start = (page - 1) * 8;
		int end = Math.min(page * 8, binds.size());
		
		ChatUtils.message("TooManyHax list (page " + page + "/" + pages + ")");
		for(int i = start; i < end; i++)
			ChatUtils.message(binds.get(i).toString());
	}
	
	private int parsePage(String[] args) throws CmdSyntaxError
	{
		if(args.length < 2)
			return 1;
		
		if(!MathUtils.isInteger(args[1]))
			throw new CmdSyntaxError("Not a number: " + args[1]);
		
		return Integer.parseInt(args[1]);
	}
	
	private void loadProfile(String[] args) throws CmdException
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		
		String name = parseFileName(args[1]);
		
		try
		{
			WURST.getKeybinds().loadProfile(name);
			ChatUtils.message("TooManyHax profile loaded: " + name);
			
		}catch(NoSuchFileException e)
		{
			throw new CmdError("Profile '" + name + "' doesn't exist.");
			
		}catch(JsonException e)
		{
			e.printStackTrace();
			throw new CmdError(
				"Profile '" + name + "' is corrupted: " + e.getMessage());
			
		}catch(IOException e)
		{
			e.printStackTrace();
			throw new CmdError("Couldn't load profile: " + e.getMessage());
		}
	}
	
	private void saveProfile(String[] args) throws CmdException
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		
		String name = parseFileName(args[1]);
		
		try
		{
			WURST.getKeybinds().saveProfile(name);
			ChatUtils.message("TooManyHax profile saved: " + name);
			
		}catch(IOException | JsonException e)
		{
			e.printStackTrace();
			throw new CmdError("Couldn't save profile: " + e.getMessage());
		}
	}
	
	private String parseFileName(String input)
	{
		String fileName = input;
		if(!fileName.endsWith(".json"))
			fileName += ".json";
		
		return fileName;
	}
	
	private void listProfiles(String[] args) throws CmdException
	{
		if(args.length > 2)
			throw new CmdSyntaxError();
		
		ArrayList<Path> files = WURST.getKeybinds().listProfiles();
		int page = parsePage(args);
		int pages = (int)Math.ceil(files.size() / 8.0);
		pages = Math.max(pages, 1);
		
		if(page > pages || page < 1)
			throw new CmdSyntaxError("Invalid page: " + page);
		
		String total = "Total: " + files.size() + " profile";
		total += files.size() != 1 ? "s" : "";
		ChatUtils.message(total);
		
		int start = (page - 1) * 8;
		int end = Math.min(page * 8, files.size());
		
		ChatUtils.message(
			"TooManyHax profile list (page " + page + "/" + pages + ")");
		for(int i = start; i < end; i++)
			ChatUtils.message(files.get(i).getFileName().toString());
	}
}
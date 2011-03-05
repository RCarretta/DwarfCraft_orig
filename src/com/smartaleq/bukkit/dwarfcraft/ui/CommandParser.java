package com.smartaleq.bukkit.dwarfcraft.ui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.smartaleq.bukkit.dwarfcraft.ConfigManager;
import com.smartaleq.bukkit.dwarfcraft.DataManager;
import com.smartaleq.bukkit.dwarfcraft.Dwarf;
import com.smartaleq.bukkit.dwarfcraft.Effect;
import com.smartaleq.bukkit.dwarfcraft.Skill;
import com.smartaleq.bukkit.dwarfcraft.ui.DCCommandException.Type;

public class CommandParser {
	
	private CommandSender sender;
	private String[] input;
	private Dwarf target = null;
	public CommandParser(CommandSender sender, String[] args){
		this.sender = sender;
		this.input = args;
	}
		
	public List<Object> parse(List<Object> desiredArguments, boolean ignoreSize) throws DCCommandException {
		List<Object> output = new ArrayList<Object> ();
		int arrayIterator = 0;
		try{
			for(Object o:desiredArguments){
				if (o instanceof Dwarf) output.add(parseDwarf(arrayIterator));
				else if (o instanceof Player) output.add(parsePlayer(arrayIterator));
				else if (o instanceof Skill) output.add(parseSkill(arrayIterator));
				else if (o instanceof Effect) output.add(parseEffect(arrayIterator));
				else if (o instanceof Boolean) output.add(parseConfirm(arrayIterator));
				else if (o instanceof String && ((String) o).equalsIgnoreCase("SkillLevelInt")) output.add(parseSkillLevel(arrayIterator));
				else if (o instanceof String && ((String) o).equalsIgnoreCase("UniqueIdAdd")) output.add(parseUniqueId(arrayIterator, true));
				else if (o instanceof String && ((String) o).equalsIgnoreCase("UniqueIdRmv")) output.add(parseUniqueId(arrayIterator, false));
				else if (o instanceof String && ((String) o).equalsIgnoreCase("Name")) output.add(parseName(arrayIterator));
				else if (o instanceof String && ((String) o).equalsIgnoreCase("GreeterId")) output.add(parseGreeterId(arrayIterator));
				else if (o instanceof String && ((String) o).equalsIgnoreCase("newRace")) output.add(parseRace(arrayIterator));
				else if (o instanceof Integer) output.add(parseInteger(arrayIterator));
				else if (o instanceof String) output.add(o);
				arrayIterator++;
			}
		}
		catch (ArrayIndexOutOfBoundsException e){
			throw new DCCommandException(Type.TOOFEWARGS);
		}
		if (input.length>output.size()&&!ignoreSize) throw new DCCommandException(Type.TOOMANYARGS);
		if (input.length<output.size()&&!ignoreSize) throw new DCCommandException(Type.TOOFEWARGS);
		for(Object o:output) if (o==null) throw new DCCommandException();
		return output;
	}
	
	private Boolean parseRace(int arrayIterator) throws DCCommandException {
		if (input[arrayIterator].equalsIgnoreCase("elf")) return true;
		else if (input[arrayIterator].equalsIgnoreCase("dwarf")) return false;
		else throw new DCCommandException(Type.PARSERACEFAIL);
	}

	private Object parseConfirm(int arrayIterator) {
		try{if (input[arrayIterator].equalsIgnoreCase("confirm")) return true;}
		catch(IndexOutOfBoundsException e){ return false;}
		return false;
	}

	private Object parsePlayer(int arrayIterator) throws DCCommandException {
		Player player = sender.getServer().getPlayer(input[arrayIterator]);
		if (player == null) throw new DCCommandException(Type.PARSEPLAYERFAIL);
		return null;
	}

	private Object parseUniqueId(int arrayIterator, boolean add) throws DCCommandException {
		String uniqueId = input[arrayIterator];
		if ( DataManager.getTrainer(uniqueId) != null && add ) throw new DCCommandException(Type.NPCIDINUSE);
		if ( DataManager.getTrainer(uniqueId) == null && !add ) throw new DCCommandException(Type.NPCIDNOTFOUND);
		return uniqueId;
	}

	private Object parseName(int arrayIterator) {
		String name = input[arrayIterator];
		return name;
	}

	private String parseGreeterId(int arrayIterator) throws DCCommandException {
		if ( DataManager.getGreeterMessage(input[arrayIterator]) == null ) throw new DCCommandException(Type.NOGREETERMESSAGE);
		String greeterMessage = input[arrayIterator];
		return greeterMessage;
	}

	private Object parseInteger(int argNumber) throws DCCommandException {
		int i;
		try{
			i = Integer.parseInt(input[argNumber]);
		}
		catch(NumberFormatException nfe){
			throw new DCCommandException(Type.PARSEINTFAIL);
		}		
		return i;
	}

	protected Dwarf parseDwarf(int argNumber) throws DCCommandException{
		Player player;
		Dwarf dwarf = null;
		try{
			String dwarfName = input[argNumber];
			player = sender.getServer().getPlayer(dwarfName);
			if (player != null && player.isOnline())
				dwarf = Dwarf.find(player);
			else if(player == null || !player.isOnline())
				dwarf = Dwarf.findOffline(dwarfName);
			if (dwarf == null) {
				throw new DCCommandException(Type.PARSEDWARFFAIL);
			}
			this.target = dwarf;
			return dwarf;
		}
		catch(ArrayIndexOutOfBoundsException e){
			if (sender instanceof Player) {
				String[] fakeInput = new String[input.length+1];
				for(int i = 0; i<input.length;i++) fakeInput[i]= input[i];
				input = fakeInput;
				this.target = Dwarf.find((Player) sender);
				return target;
			}
			else throw new DCCommandException(Type.CONSOLECANNOTUSE);
		}
		
	}
	
	protected int parseSkillLevel(int argNumber)throws DCCommandException{
		String inputString = input[argNumber];
		int level;
		try{
			level = Integer.parseInt(inputString);
		}
		catch(NumberFormatException nfe) {
			throw new DCCommandException(Type.PARSELEVELFAIL);
		}
		if (level >30 || level <-1){
			throw new DCCommandException(Type.LEVELOUTOFBOUNDS);
		}
		return level;
	}
	
	protected Skill parseSkill (int argNumber) throws DCCommandException{
		Skill skill = null;
		String inputString = input[argNumber];
		int skillID;
		if (!(sender instanceof Player)) {
			for(Skill s:ConfigManager.getAllSkills()){
				try{
					skillID = Integer.parseInt(inputString);
					if (s.id == skillID) return s;
				}
				catch(NumberFormatException nfe){
					if (inputString.length()<5) throw new DCCommandException(Type.PARSESKILLFAIL);
					if (s.displayName.regionMatches(0, inputString, 0, 5)) return s;
				}
			}
			throw new DCCommandException(Type.PARSESKILLFAIL);
		}
		if (target == null) target = Dwarf.find((Player)sender);
		try{				
			try{
				skillID = Integer.parseInt(inputString);
				skill = target.getSkill(skillID);
			}
			catch(NumberFormatException nfe){
				skill = target.getSkill(inputString);
			}
		}
		catch (NullPointerException npe){
			throw new DCCommandException(Type.EMPTYPLAYER);
		}		
		if (skill == null) throw new DCCommandException(Type.PARSESKILLFAIL);
		return skill;
	}
	
	protected Effect parseEffect (int argNumber) throws DCCommandException{
		String inputString = input[argNumber];
		Effect effect;
		int effectId;
		
		try{
			effectId = Integer.parseInt(inputString);
			effect = target.getEffect(effectId);
		}
		catch (NullPointerException npe){
			throw new DCCommandException(Type.EMPTYPLAYER);
		}
		catch(NumberFormatException nfe){
			throw new DCCommandException(Type.PARSEEFFECTFAIL);
		}		
		if (effect==null) throw new DCCommandException(Type.PARSEEFFECTFAIL);
		return effect;
	}
}

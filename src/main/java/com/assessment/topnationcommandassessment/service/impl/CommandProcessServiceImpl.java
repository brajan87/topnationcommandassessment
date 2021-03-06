package com.assessment.topnationcommandassessment.service.impl;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.tomcat.util.collections.CaseInsensitiveKeyMap;
import org.springframework.stereotype.Service;

import com.assessment.topnationcommandassessment.model.Command;
import com.assessment.topnationcommandassessment.model.CommandProcessRequest;
import com.assessment.topnationcommandassessment.model.CommandProcessResponse;
import com.assessment.topnationcommandassessment.model.FrequentCommand;
import com.assessment.topnationcommandassessment.model.StateFrequentCommand;
import com.assessment.topnationcommandassessment.service.CommandProcessService;

@Service
public class CommandProcessServiceImpl implements CommandProcessService{
	
	@Override
	public CommandProcessResponse getTopCommands(CommandProcessRequest commandProcessRequest, Map<String, List<Command>> cumulativeStateCommands) {
		CommandProcessResponse commandProcessResponse = new CommandProcessResponse();
		List<StateFrequentCommand> stateFrequentCommands = new ArrayList<>();
		Map<String, Integer> nationCommandTracker = new CaseInsensitiveKeyMap<>();
		Map<String, List<Command>> localCumulativeStateCommands = new CaseInsensitiveKeyMap<>();
		localCumulativeStateCommands.putAll(cumulativeStateCommands);
		commandProcessRequest.getStateCommands().forEach(x -> {
			if(!localCumulativeStateCommands.containsKey(x.getState())) {
				localCumulativeStateCommands.put(x.getState(), new ArrayList<>());
			}
			localCumulativeStateCommands.get(x.getState()).addAll(x.getCommands());
		});
		localCumulativeStateCommands.forEach((key,value) -> {
			stateFrequentCommands.add(processStateCommands(key, value));
			value.forEach(command -> nationCommandTracker.put(command.getSpeakerCommand(), nationCommandTracker.getOrDefault(command.getSpeakerCommand(), 0)+1));
		});
		commandProcessResponse.setStateCommands(stateFrequentCommands);
		commandProcessResponse.setTopCommandNationally(sortMapbyValue(nationCommandTracker, 3));
		cumulativeStateCommands.putAll(localCumulativeStateCommands);
		return commandProcessResponse;
	}
	
	/**
	 * This method iterate each state and keeps track of top commands within a given state
	 */
	private StateFrequentCommand processStateCommands(String state, List<Command> commands) {
		StateFrequentCommand stateFrequentCommand = null;
		Map<String, Integer> commandTracker = new CaseInsensitiveKeyMap<>();
		List<String> topCommand = new ArrayList<>();
		FrequentCommand frequentCommand = new FrequentCommand(String.valueOf(Clock.systemDefaultZone().millis()));
		commands.forEach(command -> {
			commandTracker.put(command.getSpeakerCommand(), commandTracker.getOrDefault(command.getSpeakerCommand(), 0)+1);
		});
		if(commandTracker.isEmpty()) {
			stateFrequentCommand = new StateFrequentCommand();
			stateFrequentCommand.setState(state);
		}else {
			commandTracker.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEachOrdered(x -> topCommand.add(x.getKey()));
			frequentCommand.setMostFrequentCommand(sortMapbyValue(commandTracker, 1).get(0));
			frequentCommand.setStopProcessTime(String.valueOf(Clock.systemDefaultZone().millis()));
			stateFrequentCommand = new StateFrequentCommand(state,frequentCommand);
		}
		return stateFrequentCommand;
	}
	
	private List<String> sortMapbyValue(Map<String,Integer> map, Integer limiter){
		return map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(limiter).map(Map.Entry::getKey).collect(Collectors.toList());
	}
	
}

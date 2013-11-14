package org.ggp.base.player.gamer.raho;

import java.util.Arrays;
import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class DeliberationGamer extends SampleGamer {

    @Override
    public Move stateMachineSelectMove(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException,
            GoalDefinitionException {

        long start = System.currentTimeMillis();

        List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(),
                getRole());
        Move selection = bestMove(getCurrentState());

        long stop = System.currentTimeMillis();

        notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop
                - start));
        return selection;
    }

    private int maxScore(MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        StateMachine stateMachine = getStateMachine();
        if (stateMachine.isTerminal(state)) {
            return stateMachine.getGoal(state, getRole());
        }
        List<Move> legalMoves = stateMachine.getLegalMoves(state, getRole());
        int score = 0;
        for (Move move : legalMoves) {
            MachineState newState = stateMachine.getNextState(state, Arrays.asList(move));
            int result = maxScore(newState);
            if (result == 100) {
                return 100;
            }
            if (result > score) {
                score = result;
            }

        }
        return score;
    }

    private Move bestMove(MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        StateMachine stateMachine = getStateMachine();
        List<Move> legalMoves = stateMachine.getLegalMoves(state, getRole());
        Move action = legalMoves.get(0);
        int score = 0;
        for (Move move : legalMoves) {
            MachineState newState = stateMachine.getNextState(state, Arrays.asList(move));
            int result = maxScore(newState);
            if (result == 100) {
                return move;
            }
            if (result > score) {
                score = result;
                action = move;
            }
        }
        return action;
    }

}


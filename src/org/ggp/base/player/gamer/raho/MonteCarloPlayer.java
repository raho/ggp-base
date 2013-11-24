package org.ggp.base.player.gamer.raho;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import java.util.List;

/**
 * User: rafal.hotlos
 * Date: 20/11/13
 * Time: 08:48
 */
public class MonteCarloPlayer extends SampleGamer {

    private final int expansionLimit;
    private final int monteCarloPaths;

    private long finishBy;

    public MonteCarloPlayer() {
        super();
        expansionLimit = 0;
        monteCarloPaths = 4;
    }

    @Override
    public Move stateMachineSelectMove(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException,
            GoalDefinitionException {

        long start = System.currentTimeMillis();

        finishBy = timeout - 1000;

        Move selection = bestMove(getCurrentState());

        long stop = System.currentTimeMillis();
        List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(),
                getRole());
        notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop
                - start));
        return selection;
    }

    private Move bestMove(MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        StateMachine stateMachine = getStateMachine();
        List<Move> legalMoves = stateMachine.getLegalMoves(state, getRole());
        Move action = legalMoves.get(0);
        int score = 0;
        for (Move move : legalMoves) {
            int result = minScore(move, state, 0);
            if (result > score) {
                score = result;
                action = move;
            }
        }
        return action;
    }

    private int minScore(Move action, MachineState state, int level) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        StateMachine stateMachine = getStateMachine();

        List<List<Move>> legalJointMoves = stateMachine.getLegalJointMoves(state, getRole(), action);

        int score = 100;
        for (List<Move> jointMove : legalJointMoves) {
            MachineState newState = stateMachine.getNextState(state, jointMove);
            int result = maxScore(newState, level + 1);
            if (result < score) {
                score = result;
            }
        }
        return score;
    }

    private int maxScore(MachineState state, int level) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        StateMachine stateMachine = getStateMachine();
        if (stateMachine.isTerminal(state)) {
            return stateMachine.getGoal(state, getRole());
        }
        if (level > expansionLimit) {
            return monteCarlo(state);
        }
        List<Move> legalMoves = stateMachine.getLegalMoves(state, getRole());
        int score = 0;
        for (Move move : legalMoves) {
            int result = minScore(move, state, level);
            if (result > score) {
                score = result;
            }

        }
        return score;
    }

    private int monteCarlo(MachineState state) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        StateMachine stateMachine = getStateMachine();
        int total = 0;
        if (System.currentTimeMillis() > finishBy) {
//            int goal = stateMachine.getGoal(state, getRole());
//            System.out.println("no time to monte carlo... returning current goal = " + goal);
//            return goal;
            System.out.println("no time to monte carlo...");
            return 0;
        }
        int[] depthChargeResult = new int[1];
        for (int i=0; i<monteCarloPaths; i++) {
            MachineState finalState = stateMachine.performDepthCharge(state, depthChargeResult);
            total += stateMachine.getGoal(finalState, getRole());
        }
        return total/monteCarloPaths;
    }

}

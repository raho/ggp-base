package org.ggp.base.player.gamer.raho;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * User: rafal.hotlos
 * Date: 14/11/13
 * Time: 22:36
 */
public class AlphaBetaPlayer extends SampleGamer {
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

    private Move bestMove(MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        StateMachine stateMachine = getStateMachine();
        List<Move> legalMoves = stateMachine.getLegalMoves(state, getRole());
        Move action = legalMoves.get(0);
        int score = 0;
        for (Move move : legalMoves) {
            int result = minScore(move, state, 0, 100);
            if (result > score) {
                score = result;
                action = move;
            }
        }
        return action;
    }

    private int minScore(Move action, MachineState state, int alpha, int beta) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        StateMachine stateMachine = getStateMachine();
        List<Move> oponentMoves = stateMachine.getLegalMoves(state, getOponent());
        for (Move oponentMove : oponentMoves) {
            MachineState newState = stateMachine.getNextState(state, getMovesForNextState(action, oponentMove));
            int maxVal = maxScore(newState, alpha, beta);
            beta = Math.min(beta, maxVal);
            if (beta <= alpha) {
                return alpha;
            }
        }
        return beta;
    }

    private int maxScore(MachineState state, int alpha, int beta) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        StateMachine stateMachine = getStateMachine();
        if (stateMachine.isTerminal(state)) {
            return stateMachine.getGoal(state, getRole());
        }
        List<Move> legalMoves = stateMachine.getLegalMoves(state, getRole());
        for (Move move : legalMoves) {
            int minVal = minScore(move, state, alpha, beta);
            alpha = Math.max(alpha, minVal);
            if (alpha >= beta) {
                return beta;
            }

        }
        return alpha;
    }

    private List<Move> getMovesForNextState(Move myMove, Move oponentMove) {
        List<Role> roles = getStateMachine().getRoles();
        if (roles.get(0).equals(getRole())) {
            // I am first
            return asList(myMove, oponentMove);
        } else {
            // oponent is first
            return asList(oponentMove, myMove);
        }
    }

    private Role getOponent() {
        List<Role> roles = getStateMachine().getRoles();
        for (Role role : getStateMachine().getRoles()) {
            if (!role.equals(getRole())) {
                return role;
            }
        }
        throw new IllegalArgumentException("no oponent for role " + getRoleName());
    }

}
